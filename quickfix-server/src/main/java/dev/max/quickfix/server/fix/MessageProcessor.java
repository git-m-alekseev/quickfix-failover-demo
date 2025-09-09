package dev.max.quickfix.server.fix;

import dev.max.fix.utils.ClientRequestTypes;
import dev.max.fix44.custom.messages.ClientRequest;
import dev.max.fix44.custom.messages.MessageCracker;
import dev.max.quickfix.server.fix.handlers.RequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MessageProcessor extends MessageCracker {

    private final Map<ClientRequestTypes, RequestHandler> handlers;

    public MessageProcessor(List<RequestHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(RequestHandler::type, Function.identity()));
    }

    public void process(Message message, SessionID sessionID) throws UnsupportedMessageType, IncorrectTagValue, FieldNotFound {
        crack(message, sessionID);
    }

    public void onMessage(ClientRequest clientRequest, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        var type = clientRequest.getClientRequestType().getValue();
        var rqType = ClientRequestTypes.fromString(type);
        switch (rqType) {
            case PING -> getHandler(ClientRequestTypes.PING).handle(clientRequest, sessionID);
            default -> throw new UnsupportedMessageType();
        }
    }

    private RequestHandler getHandler(ClientRequestTypes clientRequestTypes) throws UnsupportedMessageType {
        var handler = handlers.get(clientRequestTypes);
        if (handler == null) {
            throw new UnsupportedMessageType();
        }
        return handler;
    }
}
