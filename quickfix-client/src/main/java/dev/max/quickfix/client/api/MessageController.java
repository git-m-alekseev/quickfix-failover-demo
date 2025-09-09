package dev.max.quickfix.client.api;

import dev.max.quickfix.client.api.dto.Ping;
import dev.max.quickfix.client.api.dto.Pong;
import dev.max.quickfix.client.fix.initiator.FixInitiator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static dev.max.fix.utils.ClientRequestTypes.PING;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class MessageController {

    private final FixInitiator initiator;

    @PostMapping
    public Pong ping(Ping ping) {
        return initiator.request(ping, PING, Pong.class);
    }
}
