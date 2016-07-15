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

    public void send() {
        StringBuilder builder = new StringBuilder();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            int bytesRead = 0;
            while (bytesRead == 0) {
                bytesRead = inChannel.read(buffer);
            }

            while (bytesRead > 0) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    builder.append((char) buffer.get());
                }

                buffer.clear();
                bytesRead = inChannel.read(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        requester.send(builder.toString());
    }

    public String waitForReply() {
        return requester.recvStr();
    }
}
