package dev.max.quickfix.client.fix.initiator;

import dev.max.quickfix.client.ex.FixInitiatorException;
import dev.max.quickfix.client.fix.config.FixInitiatorSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import quickfix.ConfigError;
import quickfix.Connector;
import quickfix.SessionID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixInitiatorConnector {
    private final Connector fixConnector;
    private final FixInitiatorSession session;

    public void start() {
        try {
            fixConnector.start();
            log.info("FIX Initiator started for session: {}", session.sessionId());
        } catch (ConfigError e) {
            log.error("Failed to start FIX Initiator for session: {}", session.sessionId(), e);
            throw new FixInitiatorException(e);
        }
    }

    public void stop() {
        fixConnector.stop();
        log.info("FIX Initiator stopped for session: {}", session.sessionId());
    }

    public SessionID sessionId() {
        return session.sessionId();
    }
}
