package com.butler.client;

import com.butler.entity.User;
import com.butler.json.JsonObjectFactory;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientDatabaseExample {
    public static void main(String[] args) {
        String command = "getUserByLoginPassword";
        String login = "kek";
        String password = "kek";

        try (Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), 8245)) {
            Scanner scanner = new Scanner(socket.getInputStream());
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            String jsonString = JsonObjectFactory.getJsonString(command, new User(login, password));
            printWriter.println(jsonString);
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
