package com.butler.client;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientSenderExample {
    public static void main(String[] args) {
        try (Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), 8245)) {
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            Scanner input = new Scanner(System.in);
            while (input.hasNextLine()) {
                String line = input.nextLine();
                printWriter.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
