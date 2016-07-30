package com.butler.example;

import com.butler.util.entity.User;
import com.butler.util.json.JsonObjectFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HardTimes {
    public static void main(String[] args) throws Exception {
        Map<SocketChannel, User> socketUserMap = new ConcurrentHashMap<>();
        AtomicInteger integer = new AtomicInteger(0);
        for (int i = 0; i < 10000; ++i) {
            CompletableFuture.supplyAsync(() -> {
                SocketChannel channel = null;
                try {
                    System.out.println(integer.incrementAndGet());
                    channel = SocketChannel.open(new InetSocketAddress("192.168.1.41", 13000));
                    socketUserMap.put(channel, new User());
                    System.out.println(channel);
                    StringBuilder builder = new StringBuilder();
                    try {
                        builder.append("{\"command\":\"newUser\",\"user\":{\"password\":\"")
                                .append(channel.getRemoteAddress()).append("\",\"login\":\"")
                                .append(channel.getLocalAddress()).append("\",\"id\":0}}");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        channel.write(ByteBuffer.wrap(builder.toString().getBytes()));
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        channel.read(buffer);
                        String userJson = new String(buffer.array()).trim();
                        System.out.println(userJson);
                        User user = JsonObjectFactory.getObjectFromJson(userJson, User.class);
                        assert user != null;
                        if (user.validation()) {
                            socketUserMap.put(channel, user);
                            return channel;
                        }
                    } catch (IOException ignored) {
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socketUserMap.remove(channel);
                return channel;
            }).thenApply(activeChannel -> {
                User user = socketUserMap.get(activeChannel);
                try {
                    String auth = JsonObjectFactory.getJsonString("getUserByLoginPassword", user);
                    activeChannel.write(ByteBuffer.wrap(auth.getBytes()));
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    activeChannel.read(buffer);
                    String userJson = new String(buffer.array()).trim();
                    System.out.println(userJson);
                    User userExpected = JsonObjectFactory.getObjectFromJson(userJson, User.class);
                    if (user.equals(userExpected)) {
                        return activeChannel;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }).thenAcceptAsync(activeSocket -> {
                if (activeSocket != null) {
                    System.out.println(socketUserMap.get(activeSocket));
                    try {
                        String message = activeSocket.getLocalAddress() + ": " + "kek\n";
                        for (int j = 0; j < 10; ++j) {
                            ByteBuffer wrap = ByteBuffer.wrap(message.getBytes());
                            activeSocket.write(wrap);
                        }
                        activeSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).join();
        }
    }
}
