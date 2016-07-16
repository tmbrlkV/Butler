package com.butler;

import com.butler.entity.User;
import com.butler.json.JsonObjectFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class DatabaseExample {
    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 8246)) {
            String command = "getUserByLoginPassword";
            String login = "kek";
            String password = "kek";

            String jsonString = JsonObjectFactory.getJsonString(command, new User(login, password));
            InputStream in = socket.getInputStream();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(jsonString);
            int read;
            do {
                byte[] message = new byte[1024];
                read = in.read(message);
                System.out.println(new String(message));
            } while (read > 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
