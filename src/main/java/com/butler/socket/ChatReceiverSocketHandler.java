package com.butler.socket;

import com.butler.service.ConnectionProperties;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ChatReceiverSocketHandler implements Runnable {
    private ZMQ.Socket receiver;
    private ZMQ.Poller poller;
    private CopyOnWriteArrayList<Socket> sockets = new CopyOnWriteArrayList<>();
    private static int CUTOFF;

    public ChatReceiverSocketHandler(ZMQ.Context context) {
        Properties properties = ConnectionProperties.getProperties();
        String chatAddress = properties.getProperty("chat_receiver_address");
        CUTOFF = Integer.parseInt(properties.getProperty("connections_threshold"));

        receiver = context.socket(ZMQ.SUB);
        receiver.connect(chatAddress);
        receiver.subscribe("".getBytes());
        poller = new ZMQ.Poller(0);
        poller.register(receiver, ZMQ.Poller.POLLIN);
    }

    public void addHandle(Socket socket) {
        sockets.add(socket);
    }

    public void removeHandle(Socket socket) {
        sockets.remove(socket);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            int events = poller.poll();
            if (events > 0) {
                String reply = receiver.recvStr();
                Consumer<Socket> handler = socket -> {
                    try {
                        SocketChannel channel = socket.getChannel();
                        channel.write(ByteBuffer.wrap(reply.getBytes()));
                    } catch (IOException e) {
                        removeHandle(socket);
                    }
                };
                if (sockets.size() > CUTOFF) {
                    sockets.parallelStream().forEach(handler);
                } else {
                    sockets.forEach(handler);
                }
            }
        }
    }
}
