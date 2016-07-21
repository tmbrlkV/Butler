package com.butler.example;

import com.butler.entity.User;
import com.butler.json.JsonObjectFactory;
import com.butler.service.ConnectionProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

public class DatabaseExample {
    public static void main(String[] args) {
        Properties properties = ConnectionProperties.getProperties();
        String address = properties.getProperty("butler_address");
        int port = Integer.parseInt(properties.getProperty("butler_port"));

        try (Socket socket = new Socket(address, port)) {
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
