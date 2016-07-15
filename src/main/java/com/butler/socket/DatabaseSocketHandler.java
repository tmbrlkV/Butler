package com.butler.socket;

import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class DatabaseSocketHandler {
    private ZMQ.Socket requester;
    private final SocketChannel inChannel;

    public DatabaseSocketHandler(ZMQ.Context context, Socket nativeSocket) {
        requester = context.socket(ZMQ.REQ);
        requester.connect("tcp://10.66.160.204:11000");
        inChannel = nativeSocket.getChannel();
    }

    public void send(String message) {
        requester.send(message);
    }

    public String waitForReply() {
        return requester.recvStr();
    }
}
