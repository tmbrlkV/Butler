package com.butler.acceptor;

import com.butler.socket.DatabaseSocketHandler;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class DatabaseAcceptor {
    private SocketChannel channel;
    private ZMQ.Context context;


    public DatabaseAcceptor(SelectionKey key, ZMQ.Context context) {
        this.channel = (SocketChannel) key.channel();
        this.context = context;
    }

    public String chainToDatabase(String message) throws IOException {
        DatabaseSocketHandler socketHandler = new DatabaseSocketHandler(context);
        return socketHandler.communicate(message);
    }

    public void chainFromDatabase(String databaseReply) throws IOException {
        if (databaseReply != null) {
            byte[] messageBytes = databaseReply.getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
            channel.write(buffer);
            System.out.println(databaseReply);
            buffer.clear();
        }
    }


}
