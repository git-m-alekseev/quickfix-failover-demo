package dev.max.quickfix.server.fix.handlers;

import dev.max.fix.utils.ClientRequestTypes;
import dev.max.fix44.custom.messages.ClientRequest;
import dev.max.fix44.custom.messages.ClientResponse;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.SessionID;

public interface RequestHandler {

    ClientResponse handle(ClientRequest request, SessionID sessionID) throws FieldNotFound, IncorrectTagValue;

    ClientRequestTypes type();
}
