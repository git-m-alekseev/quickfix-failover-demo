package dev.max.quickfix.client.fix.initiator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.max.fix.requests.Quote;
import dev.max.fix.requests.SubscriptionRequest;
import dev.max.fix.utils.ClientRequestTypes;
import dev.max.fix.utils.ClientResponseStatuses;
import dev.max.fix44.custom.fields.ClientID;
import dev.max.fix44.custom.fields.ClientRequestType;
import dev.max.fix44.custom.fields.ReqID;
import dev.max.fix44.custom.fields.Text;
import dev.max.fix44.custom.messages.ClientRequest;
import dev.max.fix44.custom.messages.ClientResponse;
import dev.max.quickfix.client.ex.FixInitiatorException;
import dev.max.quickfix.client.fix.config.FixInitiatorSession;
import dev.max.quickfix.client.fix.initiator.subscription.SubscriptionHandlerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import quickfix.FieldNotFound;
import quickfix.Message;

import java.io.IOException;
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

    public String clientId() {
        return session.clientId();
    }

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
            var status = response.getClientResponseStatus().getValue();
            if (status.equals(ClientResponseStatuses.ERROR)) {
                throw new FixInitiatorException(response.getText().getValue());
            }
            return objectMapper.readValue(response.getText().getValue(), responseBodyType);
        } catch (FieldNotFound e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Field not found in response: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse response body for request: " + reqId, e);
        }
    }

    public void request(Object requestBody, ClientRequestTypes type) {
        var reqId = requestIdGenerator.nextId();
        var future = requestAsync(reqId, type, requestBody);
        var response = awaitResponse(future, reqId);
        try {
            var status = response.getClientResponseStatus().getValue();
            if (status.equals(ClientResponseStatuses.ERROR)) {
                throw new FixInitiatorException(response.getText().getValue());
            }
        } catch (FieldNotFound e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Field not found in response: " + e.getMessage(), e);
        }
    }

    public SseEmitter subscribe(SubscriptionRequest subscriptionRequest) {
        var sseEmitter = new SseEmitter();
        var subscriptionHandler = createSubscriptionHandler(subscriptionRequest, sseEmitter);
        responseProcessor.register(subscriptionRequest, subscriptionHandler);
        var reqId = requestIdGenerator.nextId();
        var type = ClientRequestTypes.SUBSCRIBE;
        var future = requestAsync(reqId, type, subscriptionRequest);
        var response = awaitResponse(future, reqId);
        try {
            var status = response.getClientResponseStatus().getValue();
            if (status.equals(ClientResponseStatuses.ERROR)) {
                responseProcessor.unregister(subscriptionRequest);
                sseEmitter.completeWithError(new RuntimeException(response.getText().getValue()));
            }
            return sseEmitter;
        } catch (FieldNotFound e) {
            throw new FixInitiatorException("Response body is empty", e);
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
            clientRequest.set(new ClientID(session.clientId()));
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

    private SubscriptionHandlerImpl createSubscriptionHandler(SubscriptionRequest subscriptionRequest, SseEmitter emitter) {
        return SubscriptionHandlerImpl.builder()
                .quoteConsumer(quote -> {
                    try {
                        emitter.send(Quote.fromClientQuote(quote));
                    } catch (IOException e) {
                        log.error("Failed to emmit quote: {}", quote, e);
                    }
                })
                .onError(err -> {
                    log.error("Subscription {} error: {}", subscriptionRequest, err);
                    emitter.completeWithError(new RuntimeException(err));
                })
                .onFinish(() -> {
                    log.info("Subscription {} finished", subscriptionRequest);
                    emitter.complete();
                })
                .build();
    }
}
