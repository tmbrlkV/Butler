package com.butler.socket;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TimeoutManager implements Runnable {
    private Map<Socket, Instant> sockets = new ConcurrentHashMap<>();
    private static long timeout = 10;

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (sockets.isEmpty()) {
                continue;
            }
            Consumer<Socket> handler = socket -> {
                if (Duration.between(sockets.get(socket), Instant.now()).getSeconds() > timeout) {
                    removeHandle(socket);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            if (sockets.size() > 50) {
                sockets.keySet().parallelStream().forEach(handler);
            } else {
                sockets.keySet().forEach(handler);
            }
        }
    }

    public void addHandle(Socket socket, Instant instant) {
        sockets.put(socket, instant);
    }

    public void removeHandle(Socket socket) {
        sockets.remove(socket);
    }

}
