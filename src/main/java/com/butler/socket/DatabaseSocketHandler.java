package com.butler.socket;

import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class DatabaseSocketHandler {
    private ZMQ.Socket requester;
    private Scanner scanner;
    private PrintWriter outputStream;

    public DatabaseSocketHandler(ZMQ.Context context, Socket nativeSocket) {
        requester = context.socket(ZMQ.REQ);
        requester.connect("tcp://10.66.160.204:11000");
        try {
            scanner = new Scanner(nativeSocket.getInputStream());
            outputStream = new PrintWriter(nativeSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send() {
        requester.send(scanner.nextLine());
    }

    public String waitForReply() {
        String reply = requester.recvStr();
        outputStream.println(reply);
        return reply;
    }
}
