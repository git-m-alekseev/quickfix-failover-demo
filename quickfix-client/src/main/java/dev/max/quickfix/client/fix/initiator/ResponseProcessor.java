package dev.max.quickfix.client.fix.initiator;

import dev.max.fix.utils.MessageUtils;
import dev.max.fix44.custom.messages.ClientResponse;
import dev.max.fix44.custom.messages.MessageCracker;
import dev.max.quickfix.client.ex.FixInitiatorException;
import dev.max.quickfix.client.time.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class ResponseProcessor extends MessageCracker {

    private final ConcurrentMap<String, ResponseHandler> responseProcessors = new ConcurrentHashMap<>();

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

    public void unregister(String reqId) {
        responseProcessors.remove(reqId);
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
}
