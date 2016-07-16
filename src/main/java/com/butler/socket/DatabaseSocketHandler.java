package com.butler.socket;

import com.butler.json.JsonObject;
import com.butler.json.JsonObjectFactory;
import org.zeromq.ZMQ;

public class DatabaseSocketHandler {
    private ZMQ.Socket requester;

    public DatabaseSocketHandler(ZMQ.Context context) {
        requester = context.socket(ZMQ.REQ);
        requester.connect("tcp://10.66.160.204:11000");
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
