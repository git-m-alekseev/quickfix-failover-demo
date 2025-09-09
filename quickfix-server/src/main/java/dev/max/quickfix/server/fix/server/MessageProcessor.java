package dev.max.quickfix.server.fix.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.max.fix.requests.Ping;
import dev.max.fix.requests.Pong;
import dev.max.fix.utils.ClientRequestTypes;
import dev.max.fix44.custom.fields.ReqID;
import dev.max.fix44.custom.fields.Text;
import dev.max.fix44.custom.messages.ClientRequest;
import dev.max.fix44.custom.messages.ClientResponse;
import dev.max.fix44.custom.messages.MessageCracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProcessor extends MessageCracker {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void process(Message message, SessionID sessionID) throws UnsupportedMessageType, IncorrectTagValue, FieldNotFound {
        crack(message, sessionID);
    }

    public void onMessage(ClientRequest clientRequest, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        var type = clientRequest.getClientRequestType().getValue();
        var rqType = ClientRequestTypes.fromString(type);
        switch (rqType) {
            case PING -> onPing(clientRequest.getReqID().getValue(), clientRequest.getText().getValue(), sessionID);
            default -> throw new UnsupportedMessageType();
        }
    }

    private void onPing(String reqId, String pingBody, SessionID sessionID) throws IncorrectTagValue {
        log.info("[sessionId: {}] Received Ping request [reqId: {}]: {}", sessionID, reqId, pingBody);
        var ping = parseJson(pingBody);
        var pong = createPongResponse(reqId, ping);
        try {
            quickfix.Session.sendToTarget(pong, sessionID);
        } catch (quickfix.SessionNotFound e) {
            log.error("[sessionId: {}] Couldn't send response to request [reqId: {}]", sessionID, reqId, e);
        }
    }

    private ClientResponse createPongResponse(String reqId, Ping ping) throws IncorrectTagValue {
        var pong = new Pong(ping.text());
        var responseBody = toJson(pong);
        var response = new ClientResponse();
        response.set(new Text(responseBody));
        response.set(new ReqID(reqId));
        return response;
    }

    private String toJson(Pong pong) throws IncorrectTagValue {
        try {
            return objectMapper.writeValueAsString(pong);
        } catch (JsonProcessingException e) {
            throw new IncorrectTagValue(Text.FIELD, pong.text(), "Serialize pong failed");
        }
    }

    private Ping parseJson(String pingBody) throws IncorrectTagValue {
        try {
            return objectMapper.readValue(pingBody, Ping.class);
        } catch (JsonProcessingException e) {
            throw new IncorrectTagValue(Text.FIELD, pingBody, "Parse ping failed");
        }
    }
}
