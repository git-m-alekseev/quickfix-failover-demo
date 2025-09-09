package dev.max.quickfix.server.fix.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.max.fix.requests.ExecutionRequest;
import dev.max.fix.requests.SubscriptionRequest;
import dev.max.fix.utils.ClientRequestTypes;
import dev.max.fix44.custom.fields.ClientResponseStatus;
import dev.max.fix44.custom.fields.Text;
import dev.max.fix44.custom.messages.ClientRequest;
import dev.max.fix44.custom.messages.ClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.SessionID;

import static dev.max.fix.utils.ClientRequestTypes.EXECUTE;

@Service
@RequiredArgsConstructor
public class ExecuteRequestHandler implements RequestHandler {

    private final ObjectMapper objectMapper;
    private final ClientSubscriptionsManager clientSubscriptionsManager;

    @Override
    public ClientResponse handle(ClientRequest request, SessionID sessionID) throws FieldNotFound, IncorrectTagValue {
        var response = new ClientResponse();
        response.set(request.getReqID());

        var executionRequest = parseJson(request.getText().getValue());
        var subscriptionRequest = new SubscriptionRequest(
                executionRequest.clientId(),
                executionRequest.instrument());

        var sub = clientSubscriptionsManager.getSubscription(subscriptionRequest);
        if (sub == null) {
            response.set(new ClientResponseStatus("ERROR"));
            response.set(new Text("No subscription found"));
            return response;
        }
        var lastQuote = sub.lastQuote();
        if (lastQuote == null || Math.abs(lastQuote.bid() - executionRequest.price()) > 10) {
            response.set(new ClientResponseStatus("ERROR"));
            response.set(new Text("Price has gone"));
            return response;
        }
        response.set(new ClientResponseStatus("OK"));
        return response;
    }

    @Override
    public ClientRequestTypes type() {
        return EXECUTE;
    }

    private ExecutionRequest parseJson(String body) throws IncorrectTagValue {
        try {
            return objectMapper.readValue(body, ExecutionRequest.class);
        } catch (Exception e) {
            throw new IncorrectTagValue(Text.FIELD, body, "Deserialize execution request failed");
        }
    }
}
