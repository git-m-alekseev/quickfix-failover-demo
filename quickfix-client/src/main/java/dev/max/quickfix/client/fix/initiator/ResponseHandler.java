package dev.max.quickfix.client.fix.initiator;

import dev.max.fix44.custom.messages.ClientResponse;
import quickfix.SessionID;

public interface ResponseHandler {

    void onResponse(ClientResponse clientResponse, SessionID sessionId);

    void onTimeout();
}
