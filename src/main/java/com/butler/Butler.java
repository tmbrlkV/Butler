package com.butler;

import com.butler.socket.ChatReceiverSocketHandler;
import com.butler.socket.DatabaseSocketHandler;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Butler {

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8245)) {
            try (ZMQ.Context context = ZMQ.context(1)) {
                ChatReceiverSocketHandler chatReceiverSocketHandler = new ChatReceiverSocketHandler(context);
                new Thread(chatReceiverSocketHandler).start();

                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    DatabaseSocketHandler databaseSocketHandler = new DatabaseSocketHandler(context, socket);
                    databaseSocketHandler.send();
                    String reply = databaseSocketHandler.waitForReply();
                    if (reply != null) {
                        chatReceiverSocketHandler.addHandle(socket);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}