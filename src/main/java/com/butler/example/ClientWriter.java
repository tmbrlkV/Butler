package com.butler.example;

import com.butler.service.ConnectionProperties;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

public class ClientWriter {
    public static void main(String[] args) {
        Properties properties = ConnectionProperties.getProperties();
        String address = properties.getProperty("butler_address");
        int port = Integer.parseInt(properties.getProperty("butler_port"));

        try (Socket socket = new Socket(address, port)) {
            Scanner scanner = new Scanner(System.in);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            while (scanner.hasNextLine()) {
                out.println(scanner.nextLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
