package com.butler.socket;

import com.butler.json.JsonObject;
import com.butler.json.JsonObjectFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class DatabaseSocket implements Runnable {
    private ZMQ.Socket databaseRequester;
    private PrintWriter printWriter;
    private Scanner scanner;

    public DatabaseSocket(ZMQ.Context context, Socket nativeSocket) {
        databaseRequester = context.socket(ZMQ.REQ);
        databaseRequester.connect("tcp://localhost:11000");
        try {
            printWriter = new PrintWriter(nativeSocket.getOutputStream(), true);
            scanner = new Scanner(nativeSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            JsonObject objectFromJson = JsonObjectFactory.getObjectFromJson(line, JsonObject.class);

            if (objectFromJson != null) {
                databaseRequester.send(line);
                String reply = databaseRequester.recvStr();
                printWriter.println(reply);
            } else {
                Thread.currentThread().interrupt();
            }
        }
    }
}
