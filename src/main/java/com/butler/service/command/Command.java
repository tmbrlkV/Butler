package com.butler.service.command;

import java.io.IOException;
import java.nio.channels.SelectionKey;

@FunctionalInterface
interface Command {
    String execute(String message, SelectionKey key) throws IOException;
}
