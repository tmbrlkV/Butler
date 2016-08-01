package com.butler.example;

import com.butler.util.entity.User;
import com.butler.util.json.JsonMessage;
import com.butler.util.json.JsonObjectFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HardTimes {

    private static int[] counter;

    public static void main(String[] args) throws Exception {
        Map<SocketChannel, User> socketUserMap = new ConcurrentHashMap<>();
        counter = new int[]{0};
        for (int i = 0; i < 100_000; ++i) {
            try (SocketChannel channel = SocketChannel.open(new InetSocketAddress("10.66.160.89", 13000))) {

                CompletableFuture.supplyAsync(() -> {
                    try {
                        System.out.println(++counter[0]);

                        socketUserMap.put(channel, new User());
                        System.out.println(channel);
                        writeUserToDatabase(channel);
                        User user = getUserFromDatabase(channel);
                        assert user != null;
                        if (user.validation()) {
                            socketUserMap.put(channel, user);
                            return channel;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    socketUserMap.remove(channel);
                    return null;
                }).thenApply(activeChannel -> authorization(socketUserMap, activeChannel))
                        .thenAcceptAsync(socketChannel -> writeToChat(socketUserMap, socketChannel)).join();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static User getUserFromDatabase(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.read(buffer);
        String userJson = new String(buffer.array()).trim();
        System.out.println(userJson);

        User user = JsonObjectFactory.getObjectFromJson(userJson, User.class);
        if (user != null) {
            return user;
        }
        return new User();
    }

    private static void writeUserToDatabase(SocketChannel channel) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"command\":\"newUser\",\"user\":{\"password\":\"")
                .append(counter[0]).append("\",\"login\":\"")
                .append(counter[0]).append("\",\"id\":0}}");

        channel.write(ByteBuffer.wrap(builder.toString().getBytes()));
    }

    private static SocketChannel authorization(Map<SocketChannel, User> socketUserMap, SocketChannel activeChannel) {
        if (activeChannel != null) {
            User user = socketUserMap.get(activeChannel);
            try {
                String auth = JsonObjectFactory.getJsonString("getUserByLoginPassword", user);
                activeChannel.write(ByteBuffer.wrap(auth.getBytes()));
                User userExpected = getUserFromDatabase(activeChannel);
                if (userExpected != null && user != null && user.equals(userExpected)) {
                    return activeChannel;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void writeToChat(Map<SocketChannel, User> socketUserMap, SocketChannel socketChannel) {
        if (socketChannel != null) {
            System.out.println(socketUserMap.get(socketChannel));
            try {
                JsonMessage message = new JsonMessage("message",
                        socketChannel.getLocalAddress().toString(), "kek");
                String jsonString = JsonObjectFactory.getJsonString(message);
                socketChannel.write(ByteBuffer.wrap(jsonString.getBytes()));
                socketUserMap.remove(socketChannel);
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
