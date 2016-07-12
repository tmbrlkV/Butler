package com.butler;

import com.butler.socket.ChatReceiverSocket;
import com.butler.socket.ChatSenderSocket;
import com.butler.socket.DatabaseSocket;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Butler {
    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(8245);
             ZMQ.Context context = ZMQ.context(1)) {
            while (!Thread.currentThread().isInterrupted()) {
                // TODO: 7/12/16 async with completable future
                Socket socket = serverSocket.accept();
                new Thread(new ChatReceiverSocket(context, socket)).start();
                new Thread(new ChatSenderSocket(context, socket)).start();
                new Thread(new DatabaseSocket(context, socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
