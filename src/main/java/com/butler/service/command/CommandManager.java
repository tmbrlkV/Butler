package com.butler.service.command;

import com.butler.socket.ChatSenderSocketHandler;
import com.butler.socket.TimeoutManager;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.TreeMap;

public class CommandManager {
    private Map<String, Command> commands = new TreeMap<>();

    public CommandManager(ChatSenderSocketHandler senderSocketHandler, TimeoutManager timeoutManager) {
        commands.put("message", new ChatCommand(senderSocketHandler, timeoutManager));
    }

    public void execute(String message, SelectionKey key) throws IOException {
        Command process = commands.getOrDefault(getCommand(message), (n, o) -> "");
        process.execute(message, key);
    }

    private String getCommand(String message) {
        return message.split("command")[1].split(",")[0].replace(":", "").replace("\"", "");
    }
}
