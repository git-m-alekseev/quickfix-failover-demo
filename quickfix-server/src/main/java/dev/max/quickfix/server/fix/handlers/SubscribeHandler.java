package dev.max.quickfix.server.fix.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.max.fix.requests.SubscriptionRequest;
import dev.max.fix.utils.ClientRequestTypes;
import dev.max.fix.utils.ClientResponseStatuses;
import dev.max.fix44.custom.fields.ClientInstrument;
import dev.max.fix44.custom.fields.ClientResponseStatus;
import dev.max.fix44.custom.fields.ReqID;
import dev.max.fix44.custom.fields.SubscriptionFinishedReason;
import dev.max.fix44.custom.fields.Text;
import dev.max.fix44.custom.messages.ClientRequest;
import dev.max.fix44.custom.messages.ClientResponse;
import dev.max.fix44.custom.messages.Message;
import dev.max.fix44.custom.messages.SubscriptionFinished;
import dev.max.quickfix.server.subscription.QuoteStreamSubscriberImpl;
import dev.max.quickfix.server.subscription.QuoteStreamSubscriber;
import dev.max.quickfix.server.subscription.SubscriberException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeHandler implements RequestHandler {

    private final ObjectMapper objectMapper;
    private final ClientSubscriptionsManager subscriptionManager;

    @Override
    public ClientResponse handle(ClientRequest request, SessionID sessionID) throws FieldNotFound, IncorrectTagValue {
        var reqId = request.getReqID().getValue();
        var body = request.getText().getValue();
        var subscribeRequest = parseJson(body);
        var response = new ClientResponse();
        response.set(request.getReqID());
        response.set(request.getClientRequestType());
        try {
            var subscriber = createSubscriber(reqId, subscribeRequest.instrument(), sessionID);
            subscriptionManager.subscribe(subscribeRequest, subscriber);
            response.set(new ClientResponseStatus(ClientResponseStatuses.OK));
        } catch (Exception e) {
            log.error("Error subscribing to instrument {}", subscribeRequest.instrument(), e);
            response.set(new ClientResponseStatus(ClientResponseStatuses.ERROR));
            response.set(new Text(e.getMessage()));
        }
        return response;
    }

    private QuoteStreamSubscriber createSubscriber(String reqId, String instrument, SessionID sessionID) {
        var finished = new SubscriptionFinished();
        finished.set(new ReqID(reqId));
        finished.set(new ClientInstrument(instrument));;
        return QuoteStreamSubscriberImpl.builder()
                .onQuote(quote -> {
                    sendMessage(quote.toClientQuote(reqId), sessionID);
                }).onComplete(() -> {
                    finished.set(new SubscriptionFinishedReason("OK"));
                    sendMessage(finished, sessionID);
                }).onError(th -> {
                    log.error("Subscriber error [instrument: {}]", instrument, th);
                    finished.set(new SubscriptionFinishedReason(th.getMessage()));
                    sendMessage(finished, sessionID);
                }).build();
    }

    private static void sendMessage(Message msg, SessionID sessionID) {
        try {
            Session.sendToTarget(msg, sessionID);
        } catch (SessionNotFound e) {
            log.error("Couldn't send quote to session {}", sessionID, e);
            throw new SubscriberException("Couldn't send quote to session " + sessionID, true, e);
        }
    }

    @Override
    public ClientRequestTypes type() {
        return ClientRequestTypes.SUBSCRIBE;
    }

    private SubscriptionRequest parseJson(String body) throws IncorrectTagValue {
        try {
            return objectMapper.readValue(body, SubscriptionRequest.class);
        } catch (Exception e) {
            throw new IncorrectTagValue(Text.FIELD, body, "Deserialize subscribe request failed");
        }
    }
}
