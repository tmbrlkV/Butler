package com.butler;

import com.butler.socket.DatabaseSocketHandler;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
                butler.selector.select();
                Iterator keyIterator = butler.selector.selectedKeys().iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = (SelectionKey) keyIterator.next();
                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        butler.accept(key);
                    }
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(true);
        DatabaseSocketHandler databaseSocketHandler = new DatabaseSocketHandler(context, channel.socket());
        databaseSocketHandler.send();
        String reply = databaseSocketHandler.waitForReply();
        System.out.println(reply);
    }

    @Override
    public void close() throws Exception {
        selector.close();
        socketChannel.close();
        context.close();
    }
}
