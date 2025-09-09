package dev.max.quickfix.client.fix.initiator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.max.fix.utils.ClientRequestTypes;
import dev.max.fix44.custom.fields.ClientRequestType;
import dev.max.fix44.custom.fields.ReqID;
import dev.max.fix44.custom.fields.Text;
import dev.max.fix44.custom.messages.ClientRequest;
import dev.max.fix44.custom.messages.ClientResponse;
import dev.max.quickfix.client.ex.FixInitiatorException;
import dev.max.quickfix.client.fix.config.FixInitiatorSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import quickfix.FieldNotFound;
import quickfix.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixInitiator {
    private final ObjectMapper objectMapper;
    private final RequestIdGenerator requestIdGenerator = new RequestIdGenerator();
    private final FixInitiatorSession session;
    private final ResponseProcessor responseProcessor;

    public void send(Message message) {
        try {
            if (quickfix.Session.sendToTarget(message, session.sessionId())) {
                log.info("Sent message to session {}: {}", session.sessionId(), message);
            } else {
                log.info("Failed to send message to session {}: {}", session.sessionId(), message);
                throw new FixInitiatorException("Failed to send message to session: " + session.sessionId());
            }
        } catch (quickfix.SessionNotFound e) {
            log.error("Session not found for sending message: {}", session.sessionId(), e);
            throw new FixInitiatorException("Session not found: " + session.sessionId(), e);
        }
    }

    public <R> R request(Object requestBody, ClientRequestTypes type, Class<? extends R> responseBodyType) {
        var reqId = requestIdGenerator.nextId();
        var future = requestAsync(reqId, type, requestBody);
        var response = awaitResponse(future, reqId);
        try {
            return objectMapper.readValue(response.getText().getValue(), responseBodyType);
        } catch (FieldNotFound e) {
            throw new FixInitiatorException("Response body is empty: " + responseBodyType, e);
        } catch (JsonProcessingException e) {
            throw new FixInitiatorException("Failed to parse response body for request: " + reqId, e);
        }
    }

    private ClientResponse awaitResponse(CompletableFuture<ClientResponse> future, String reqId) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseProcessor.unregister(reqId);
            throw new FixInitiatorException("Failed to get response for request: " + reqId, e);
        } catch (ExecutionException e) {
            responseProcessor.unregister(reqId);
            if (e.getCause() instanceof FixInitiatorException fie) {
                throw fie;
            }
            throw new FixInitiatorException("Failed to get response for request: " + reqId, e.getCause());
        }
    }

    private CompletableFuture<ClientResponse> requestAsync(String reqId, ClientRequestTypes type, Object requestBody) {
        var request = createRequest(reqId, type, requestBody);
        var future = new java.util.concurrent.CompletableFuture<ClientResponse>();
        responseProcessor.register(reqId, new ResponseHandler() {
            @Override
            public void onResponse(ClientResponse response, quickfix.SessionID sessionId) {
                future.complete(response);
            }

            @Override
            public void onTimeout() {
                future.completeExceptionally(new FixInitiatorException("Request timed out: " + reqId));
            }
        });
        sendRequest(request, reqId);
        return future;
    }

    private void sendRequest(ClientRequest request, String reqId) {
        try {
            send(request);
        } catch (FixInitiatorException e) {
            responseProcessor.unregister(reqId);
            throw e;
        }
    }

    private ClientRequest createRequest(String reqId, ClientRequestTypes type, Object request) {
        try {
            var json = objectMapper.writeValueAsString(request);
            var clientRequest = new ClientRequest();
            clientRequest.set(new ReqID(reqId));
            clientRequest.set(new Text(json));
            clientRequest.set(new ClientRequestType(type.name()));
            return clientRequest;
        } catch (JsonProcessingException e) {
            throw new FixInitiatorException(e);
        }
    }

    private static class RequestIdGenerator {
        private final AtomicLong requestId = new AtomicLong();

        public String nextId() {
            return String.valueOf(requestId.incrementAndGet());
        }
    }
}
