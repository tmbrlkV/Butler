package com.butler.socket;

import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatReceiverSocketHandler implements Runnable {
    private ZMQ.Socket receiver;
    private ZMQ.Poller poller;
    private CopyOnWriteArrayList<Socket> sockets = new CopyOnWriteArrayList<>();

    public ChatReceiverSocketHandler(ZMQ.Context context) {
        receiver = context.socket(ZMQ.SUB);
        receiver.connect("tcp://10.66.162.213:10000");
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
                sockets.parallelStream().forEach(socket -> {
                    try {
                        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
                        printWriter.println(reply);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
