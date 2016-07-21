package com.butler.service;

import com.butler.acceptor.DatabaseAcceptor;
import com.butler.json.JsonObject;
import com.butler.json.JsonObjectFactory;
import com.butler.socket.ChatReceiverSocketHandler;
import com.butler.socket.ChatSenderSocketHandler;
import com.butler.socket.TimeoutManager;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.Iterator;
import java.util.Properties;

public class NIOButler implements AutoCloseable {
    private Selector selector;
    private InetSocketAddress address;
    private ServerSocketChannel socketChannel;
    private ZMQ.Context context = ZMQ.context(1);
    private ChatReceiverSocketHandler chatReceiverSocketHandler;
    private DatabaseAcceptor databaseAcceptor;
    private ChatSenderSocketHandler chatSenderSocketHandler;
    private TimeoutManager timeoutManager;

    private NIOButler(String address, int port) {
        this.address = new InetSocketAddress(address, port);
    }

    public static void main(String[] args) {
        Properties properties = ConnectionProperties.getProperties();
        String address = properties.getProperty("butler_address");
        int port = Integer.parseInt(properties.getProperty("butler_port"));

        try (NIOButler butler = new NIOButler(address, port)) {
            butler.init();
            new Thread(butler.chatReceiverSocketHandler).start();

            while (!Thread.currentThread().isInterrupted()) {
                butler.selector.select();
                Iterator keyIterator = butler.selector.selectedKeys().iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = (SelectionKey) keyIterator.next();
                    if (!key.isValid()) {
                        continue;
                    }
                    try {
                        if (key.isAcceptable()) {
                            butler.accept(key);
                        } else if (key.isWritable()) {
                            butler.write(key);
                        } else if (key.isReadable()) {
                            butler.read(key);
                        }
                    } catch (IOException e) {
                        butler.dropClient(key);
                    }
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws IOException {
        selector = Selector.open();
        socketChannel = ServerSocketChannel.open();
        socketChannel.configureBlocking(false);

        socketChannel.socket().bind(address);
        socketChannel.register(selector, SelectionKey.OP_ACCEPT);
        chatReceiverSocketHandler = new ChatReceiverSocketHandler(context);
        chatSenderSocketHandler = new ChatSenderSocketHandler(context);

        timeoutManager = new TimeoutManager();
        new Thread(timeoutManager).start();
    }

    private void read(SelectionKey key) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketChannel channel = (SocketChannel) key.channel();
        int numRead = channel.read(buffer);

        if (numRead == -1) {
            throw new IOException("connection is closed");
        }

        String message = new String(buffer.array()).trim();

        if (JsonObjectFactory.getObjectFromJson(message, JsonObject.class) == null) {
            chatSenderSocketHandler.send(message);
            timeoutManager.addHandle(channel.socket(), Instant.now());
        } else {
            databaseAcceptor = new DatabaseAcceptor(key, context);
            String databaseReply = databaseAcceptor.chainToDatabase(message);
            key.attach(null);
            key.channel().register(selector, SelectionKey.OP_WRITE, databaseReply);
        }
    }

    private void write(SelectionKey key) throws IOException {
        String databaseReply = (String) key.attachment();
        databaseAcceptor.chainFromDatabase(databaseReply);
        key.channel().register(selector, SelectionKey.OP_READ);
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);
        chatReceiverSocketHandler.addHandle(channel.socket());
        channel.register(selector, SelectionKey.OP_READ);
    }

    private void dropClient(SelectionKey key) {
        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            Socket socket = clientChannel.socket();
            SocketAddress remoteAddress = socket.getRemoteSocketAddress();
            System.out.println("Connection closed by client: " + remoteAddress);
            clientChannel.close();
            chatReceiverSocketHandler.removeHandle(socket);
            timeoutManager.removeHandle(socket);
            key.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void close() throws Exception {
        selector.close();
        socketChannel.close();
        context.close();
    }
}
