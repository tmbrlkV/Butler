package com.butler.example;

import com.butler.util.entity.User;
import com.butler.util.json.JsonMessage;
import com.butler.util.json.JsonObjectFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HardTimes {
    public static void main(String[] args) throws Exception {
        Map<SocketChannel, User> socketUserMap = new HashMap<>();
        final int[] counter = {0};
        for (int i = 0; i < 100_000; ++i) {
            CompletableFuture.supplyAsync(() -> {
                SocketChannel channel = null;
                try {
                    System.out.println(++counter[0]);
                    channel = SocketChannel.open(new InetSocketAddress("192.168.1.36", 13000));
                    socketUserMap.put(channel, new User());
                    System.out.println(channel);
                    writeUserToDatabase(channel);
                    User user = getUserFromDatabase(channel, 0);
                    assert user != null;
                    if (user.validation()) {
                        socketUserMap.put(channel, user);
                        return channel;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                socketUserMap.remove(channel);
                return channel;
            }).thenApply(activeChannel -> authorization(socketUserMap, activeChannel))
                    .thenAcceptAsync(socketChannel -> closeChannel(socketUserMap, socketChannel)).join();
        }
    }

    private static User getUserFromDatabase(SocketChannel channel, int k) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.read(buffer);
        String userJson = new String(buffer.array()).trim();
        System.out.println(userJson);

        User user = JsonObjectFactory.getObjectFromJson(userJson, User.class);
        if (user != null) {
            return user;
        } else if (k < 2) {
            return getUserFromDatabase(channel, k + 1);
        } else {
            return new User();
        }
    }

    private static void writeUserToDatabase(SocketChannel channel) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"command\":\"newUser\",\"user\":{\"password\":\"")
                .append(channel.getRemoteAddress()).append("\",\"login\":\"")
                .append(channel.getLocalAddress()).append("\",\"id\":0}}");


        channel.write(ByteBuffer.wrap(builder.toString().getBytes()));
    }

    private static SocketChannel authorization(Map<SocketChannel, User> socketUserMap, SocketChannel activeChannel) {
        User user = socketUserMap.get(activeChannel);
        try {
            String auth = JsonObjectFactory.getJsonString("getUserByLoginPassword", user);
            activeChannel.write(ByteBuffer.wrap(auth.getBytes()));
            User userExpected = getUserFromDatabase(activeChannel, 0);
            if (user.equals(userExpected)) {
                return activeChannel;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void closeChannel(Map<SocketChannel, User> socketUserMap, SocketChannel socketChannel) {
        if (socketChannel != null) {
            System.out.println(socketUserMap.get(socketChannel));
            try {
                JsonMessage message = new JsonMessage("message",
                        socketChannel.getLocalAddress().toString(), "kek");
                String jsonString = JsonObjectFactory.getJsonString(message);
                socketChannel.write(ByteBuffer.wrap(jsonString.getBytes()));
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
