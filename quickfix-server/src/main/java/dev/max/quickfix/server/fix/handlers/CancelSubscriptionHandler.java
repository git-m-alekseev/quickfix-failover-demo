package dev.max.quickfix.server.fix.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.max.fix.requests.SubscriptionRequest;
import dev.max.fix.utils.ClientRequestTypes;
import dev.max.fix44.custom.fields.ClientResponseStatus;
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
public class CancelSubscriptionHandler implements RequestHandler {

    private final ObjectMapper objectMapper;
    private final ClientSubscriptionsManager subscriptionManager;

    @Override
    public ClientResponse handle(ClientRequest request, SessionID sessionID) throws FieldNotFound, IncorrectTagValue {
        var reqId = request.getReqID().getValue();
        var body = request.getText().getValue();
        var subscribeRequest = parseJson(body);
        var response = new ClientResponse();
        response.set(request.getReqID());
        try {
            subscriptionManager.cancelSubscription(subscribeRequest);
            response.set(new ClientResponseStatus("OK"));
        } catch (Exception e) {
            log.error("Error subscribing to instrument {}", subscribeRequest.instrument(), e);
            response.set(new ClientResponseStatus("ERROR"));
            response.set(new Text(e.getMessage()));
        }
        return response;
    }

    @Override
    public ClientRequestTypes type() {
        return ClientRequestTypes.UNSUBSCRIBE;
    }

    private SubscriptionRequest parseJson(String body) throws IncorrectTagValue {
        try {
            return objectMapper.readValue(body, SubscriptionRequest.class);
        } catch (Exception e) {
            throw new IncorrectTagValue(Text.FIELD, body, "Deserialize unsubscribe request failed");
        }
    }
}
