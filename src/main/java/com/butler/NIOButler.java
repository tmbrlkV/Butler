package com.butler;

import com.butler.socket.DatabaseSocketHandler;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class NIOButler implements AutoCloseable {
    private Selector selector;
    private InetSocketAddress address;
    private ServerSocketChannel socketChannel;
    private ZMQ.Context context = ZMQ.context(1);

    private NIOButler(String address, int port) {
        this.address = new InetSocketAddress(address, port);
    }

    public static void main(String[] args) {
        try (NIOButler butler = new NIOButler("127.0.0.1", 8246)) {

            butler.selector = Selector.open();
            butler.socketChannel = ServerSocketChannel.open();
            butler.socketChannel.configureBlocking(false);

            butler.socketChannel.socket().bind(butler.address);
            butler.socketChannel.register(butler.selector, SelectionKey.OP_ACCEPT);

            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("select");
                butler.selector.select();
                Iterator keyIterator = butler.selector.selectedKeys().iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = (SelectionKey) keyIterator.next();
                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        butler.accept(key);
                    } else if (key.isWritable()) {
                        butler.write(key);
                    } else if (key.isReadable()) {
                        butler.read(key);
                    }
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) {
        System.out.println("read");
        SocketChannel clientChannel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead;
        try {
            numRead = clientChannel.read(buffer);

            if (numRead == -1) {
                dropClient(key, clientChannel);
                return;
            }

            String message = new String(buffer.array());
            System.out.println(message);

            DatabaseSocketHandler socketHandler = new DatabaseSocketHandler(context, clientChannel.socket());
            socketHandler.send(message);
            String databaseReply = socketHandler.waitForReply();
            System.out.println(databaseReply);

            clientChannel.register(selector, SelectionKey.OP_WRITE, databaseReply);
        } catch (IOException e) {
            dropClient(key, clientChannel);
        }

    }

    private void write(SelectionKey key) throws IOException {
        System.out.println("write");
        SocketChannel clientChannel = (SocketChannel) key.channel();
        String databaseReply = (String) key.attachment();
        if (databaseReply != null) {
            try {
                byte[] messageBytes = databaseReply.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
                clientChannel.write(buffer);
                System.out.println(databaseReply);
                buffer.clear();
                clientChannel.register(selector, SelectionKey.OP_READ);
            } catch (IOException e) {
                dropClient(key, clientChannel);
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        System.out.println("accept");
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);

        channel.register(selector, SelectionKey.OP_READ);
    }

    private void dropClient(SelectionKey key, SocketChannel clientChannel) {
        try {
            Socket socket = clientChannel.socket();
            SocketAddress remoteAddress = socket.getRemoteSocketAddress();
            System.out.println("Connection closed by client: " + remoteAddress);
            clientChannel.close();
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
