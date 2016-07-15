package com.butler.socket;

import org.zeromq.ZMQ;

public class ChatSenderSocketHandler {
    private ZMQ.Socket sender;

    public ChatSenderSocketHandler(ZMQ.Context context) {
        sender = context.socket(ZMQ.PUSH);
        sender.connect("tcp://10.66.160.204:10001");
    }

    public void send(String message) {
        sender.send(message);
    }
}
