package com.butler.socket;

import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import static org.zeromq.ZMQ.SUB;

public class ChatReceiverSocket implements Runnable {
    private ZMQ.Socket receiver;
    private ZMQ.Poller poller;
    private PrintWriter printWriter;

    public ChatReceiverSocket(ZMQ.Context context, Socket nativeSocket) {
        try {
            printWriter = new PrintWriter(nativeSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        init(context);
    }

    private void init(ZMQ.Context context) {
        receiver = context.socket(SUB);
        receiver.connect("tcp://localhost:10000");
        receiver.subscribe("".getBytes());
        poller = new ZMQ.Poller(0);
        poller.register(receiver, ZMQ.Poller.POLLIN);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            int events = poller.poll();
            if (events > 0) {
                String message = receiver.recvStr(0);
                System.out.println(message);
                printWriter.println(message);
            }
        }
    }
}
