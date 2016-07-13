package com.butler;

import com.butler.entity.User;
import com.butler.json.JsonObjectFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class DatabaseExample {
    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 8245)) {
            String command = "getUserByLoginPassword";
            String login = "kek";
            String password = "kek";

            String jsonString = JsonObjectFactory.getJsonString(command, new User(login, password));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(jsonString);
            Scanner scanner = new Scanner(socket.getInputStream());
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
