package com.butler.socket;

import com.butler.service.ConnectionProperties;
import org.zeromq.ZMQ;

import java.util.Properties;

public class ChatSenderSocketHandler {
    private ZMQ.Socket sender;

    public ChatSenderSocketHandler(ZMQ.Context context) {
        Properties properties = ConnectionProperties.getProperties();
        String chatAddress = properties.getProperty("chat_sender_address");

        sender = context.socket(ZMQ.PUSH);
        sender.connect(chatAddress);
    }

    public void send(String message) {
        sender.send(message);
    }
}
