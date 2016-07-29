package com.butler.socket;

import com.butler.util.json.JsonObject;
import com.butler.util.json.JsonObjectFactory;
import com.butler.service.ConnectionProperties;
import org.zeromq.ZMQ;

import java.util.Properties;

public class DatabaseSocketHandler {
    private ZMQ.Socket requester;

    public DatabaseSocketHandler(ZMQ.Context context) {
        Properties properties = ConnectionProperties.getProperties();
        String databaseAddress = properties.getProperty("database_address");

        requester = context.socket(ZMQ.REQ);
        requester.connect(databaseAddress);
    }

    public String communicate(String request) {
        if (isValid(request)) {
            requester.send(request);
            return waitForReply();
        }
        return null;
    }

    private boolean isValid(String request) {
        return JsonObjectFactory.getObjectFromJson(request, JsonObject.class) != null;
    }

    private String waitForReply() {
        return requester.recvStr();
    }
}
