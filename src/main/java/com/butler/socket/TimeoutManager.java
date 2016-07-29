package com.butler.socket;

import com.butler.service.ConnectionProperties;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TimeoutManager implements Runnable {
    private Map<Socket, Instant> sockets = new ConcurrentHashMap<>();
    private final int threshold;
    private final int threadCheckDelay;
    private static long timeout;

    public TimeoutManager() {
        Properties properties = ConnectionProperties.getProperties();
        threshold = Integer.parseInt(properties.getProperty("connections_threshold"));
        threadCheckDelay = Integer.parseInt(properties.getProperty("thread_check_delay_ms"));
        timeout = Long.parseLong(properties.getProperty("timeout_sec"));
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(threadCheckDelay);
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
            if (sockets.size() > threshold) {
                sockets.keySet().parallelStream().forEach(handler);
            } else {
                sockets.keySet().forEach(handler);
            }
        }
    }

    public void addHandle(Socket socket, Instant instant) {
        System.out.println("Added " + socket);
        sockets.put(socket, instant);
        System.out.println(sockets.size());
    }

    public void removeHandle(Socket socket) {
        System.out.println("Removed " + socket);
        sockets.remove(socket);
        System.out.println(sockets.size());
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
