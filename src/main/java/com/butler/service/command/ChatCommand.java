package com.butler.service.command;

import com.butler.socket.ChatSenderSocketHandler;
import com.butler.socket.TimeoutManager;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.time.Instant;

class ChatCommand implements Command {
    private TimeoutManager timeoutManager;
    private ChatSenderSocketHandler chatSenderSocketHandler;

    ChatCommand(ChatSenderSocketHandler chatSenderSocketHandler, TimeoutManager timeoutManager) {
        this.chatSenderSocketHandler = chatSenderSocketHandler;
        this.timeoutManager = timeoutManager;
    }

    @Override
    public String execute(String message, SelectionKey key) {
        chatSenderSocketHandler.send(message);
        timeoutManager.addHandle(((SocketChannel) key.channel()).socket(), Instant.now());
        return "";
    }
}
