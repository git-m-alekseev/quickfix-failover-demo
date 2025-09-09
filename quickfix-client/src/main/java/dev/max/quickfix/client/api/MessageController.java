package dev.max.quickfix.client.api;

import dev.max.fix.requests.ExecutionRequest;
import dev.max.fix.requests.ExecutionResponse;
import dev.max.fix.requests.Ping;
import dev.max.fix.requests.Pong;
import dev.max.fix.requests.SubscriptionRequest;
import dev.max.quickfix.client.fix.initiator.FixInitiator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static dev.max.fix.utils.ClientRequestTypes.EXECUTE;
import static dev.max.fix.utils.ClientRequestTypes.PING;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class MessageController {

    private final FixInitiator initiator;

    @PostMapping("/requests/ping")
    public Pong ping(@RequestBody Ping ping) {
        return initiator.request(ping, PING, Pong.class);
    }

    @PostMapping("/requests/execution")
    public ExecutionResponse execute(@RequestBody ExecutionRequest executionRequest) {
        if (!executionRequest.clientId().equals(initiator.clientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client ID mismatch");
        }
        return initiator.request(executionRequest, EXECUTE, ExecutionResponse.class);
    }

    @PostMapping(
            value = "/subscriptions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(SubscriptionRequest request) {
        return initiator.subscribe(request);
    }
}
