package com.butler.socket;

import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatSenderSocket implements Runnable {
    private ZMQ.Socket sender;
    private PrintWriter printWriter;
    private Scanner scanner;
    public ChatSenderSocket(ZMQ.Context context, Socket socket) {
        sender = context.socket(ZMQ.PUSH);
        sender.connect("tcp://localhost:10001");
        try {
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            scanner = new Scanner(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String messageToSend = scanner.nextLine();
            sender.send(messageToSend);
            printWriter.println(messageToSend);
        }
    }
}
