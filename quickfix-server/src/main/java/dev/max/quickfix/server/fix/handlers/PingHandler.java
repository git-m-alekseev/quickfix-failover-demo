package dev.max.quickfix.server.fix.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.max.fix.requests.Ping;
import dev.max.fix.requests.Pong;
import dev.max.fix.utils.ClientRequestTypes;
import dev.max.fix.utils.ClientResponseStatuses;
import dev.max.fix44.custom.fields.ClientRequestType;
import dev.max.fix44.custom.fields.ClientResponseStatus;
import dev.max.fix44.custom.fields.ReqID;
import dev.max.fix44.custom.fields.Text;
import dev.max.fix44.custom.messages.ClientRequest;
import dev.max.fix44.custom.messages.ClientResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.SessionID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PingHandler implements RequestHandler {

    private final ObjectMapper objectMapper;

    @Override
    public ClientResponse handle(ClientRequest request, SessionID sessionId) throws FieldNotFound, IncorrectTagValue {
        var reqId = request.getReqID().getValue();
        var pingBody = request.getText().getValue();
        log.info("[sessionId: {}] Received Ping request [reqId: {}]: {}", sessionId, reqId, pingBody);
        var ping = parseJson(pingBody);
        return createPongResponse(reqId, request.getClientRequestType(), ping);
    }

    private ClientResponse createPongResponse(String reqId, ClientRequestType type, Ping ping) throws IncorrectTagValue {
        var pong = new Pong(ping.text());
        var responseBody = toJson(pong);
        var response = new ClientResponse();
        response.set(new Text(responseBody));
        response.set(new ReqID(reqId));
        response.set(new ClientResponseStatus(ClientResponseStatuses.OK));
        response.set(type);
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

    @Override
    public ClientRequestTypes type() {
        return ClientRequestTypes.PING;
    }
}
