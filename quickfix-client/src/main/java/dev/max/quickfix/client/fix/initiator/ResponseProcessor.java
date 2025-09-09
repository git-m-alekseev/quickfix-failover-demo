package dev.max.quickfix.client.fix.initiator;

import dev.max.fix.requests.SubscriptionRequest;
import dev.max.fix.utils.MessageUtils;
import dev.max.fix44.custom.messages.ClientQuote;
import dev.max.fix44.custom.messages.ClientResponse;
import dev.max.fix44.custom.messages.MessageCracker;
import dev.max.fix44.custom.messages.SubscriptionFinished;
import dev.max.quickfix.client.ex.FixInitiatorException;
import dev.max.quickfix.client.fix.config.FixInitiatorSession;
import dev.max.quickfix.client.fix.initiator.subscription.SubscriptionHandler;
import dev.max.quickfix.client.time.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class ResponseProcessor extends MessageCracker {

    private final FixInitiatorSession session;
    private final ConcurrentMap<String, ResponseHandler> responseProcessors = new ConcurrentHashMap<>();
    private final ConcurrentMap<SubscriptionRequest, SubscriptionHandler> subscriptionHandlers = new ConcurrentHashMap<>();

    public void register(String reqId, ResponseHandler messageProcessor) {
        responseProcessors.put(reqId, messageProcessor);
        TimeUtils.onTimeout(3000, () -> {
            var processor = responseProcessors.remove(reqId);
            if (processor != null) {
                log.info("Request timeout {}", reqId);
                processor.onTimeout();
            }
        });
    }

    public void register(SubscriptionRequest request, SubscriptionHandler subscriptionHandler) {
        var registered = new AtomicBoolean(false);
        subscriptionHandlers.computeIfAbsent(request, key ->  {
            registered.set(true);
            return subscriptionHandler;
        });
        if (!registered.get()) {
            throw new FixInitiatorException("Subscription exist: " + registered);
        }
    }

    public void unregister(String reqId) {
        responseProcessors.remove(reqId);
    }

    public void unregister(SubscriptionRequest request) {
        subscriptionHandlers.remove(request);
    }

    public void process(Message message, SessionID sessionId) {
        try {
            crack(message, sessionId);
        } catch (IncorrectTagValue | FieldNotFound e) {
            throw new FixInitiatorException(e);
        } catch (UnsupportedMessageType e) {
            log.info("Got unsupported message: {}", MessageUtils.toString(message));
        }
    }

    @Override
    public void onMessage(ClientResponse message, SessionID sessionId) throws FieldNotFound {
        var processor = responseProcessors.remove(message.getReqID().getValue());
        if (processor != null) {
            processor.onResponse(message, sessionId);
        } else {
            log.error("Got response for timed out request: {}", MessageUtils.toString(message));
        }
    }

    @Override
    public void onMessage(ClientQuote quote, SessionID sessionId) throws FieldNotFound {
        var instrument = quote.getClientInstrument().getValue();
        var clientId = session.clientId();
        var subscriptionRequest = new SubscriptionRequest(clientId, instrument);
        var handler = subscriptionHandlers.get(subscriptionRequest);
        if (handler != null) {
            handler.onQuote(quote);
        } else {
            log.error("No subscription found for quote: {}", MessageUtils.toString(quote));
        }
    }

    @Override
    public void onMessage(SubscriptionFinished message, SessionID sessionId) throws FieldNotFound {
        var instrument = message.getClientInstrument().getValue();
        var clientId = session.clientId();
        var subscriptionRequest = new SubscriptionRequest(clientId, instrument);
        var handler = subscriptionHandlers.remove(subscriptionRequest);
        if (handler != null) {
            if (message.getSubscriptionFinishedReason().getValue().equals("OK")) {
                handler.onFinish();
            } else {
                handler.onError(message.getSubscriptionFinishedReason().getValue());
            }
        } else {
            log.error("Got subscription finished request for non existing subscription: {}", MessageUtils.toString(message));
        }
    }
}
