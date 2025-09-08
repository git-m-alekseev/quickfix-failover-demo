package dev.max.quickfix.client.fix.initiator;

import dev.max.quickfix.client.ex.FixInitiatorException;
import dev.max.quickfix.client.fix.config.FixInitiatorSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import quickfix.ConfigError;
import quickfix.Message;
import quickfix.ThreadedSocketInitiator;

@Slf4j
@RequiredArgsConstructor
public class FixInitiator {
    private FixInitiatorSession session;
    private final ThreadedSocketInitiator initiator;

    public void start() {
        try {
            initiator.start();
            log.info("FIX Initiator started for session: {}", session.sessionId());
        } catch (ConfigError e) {
            log.error("Failed to start FIX Initiator for session: {}", session.sessionId(), e);
            throw new FixInitiatorException(e);
        }
    }

    public void stop() {
        initiator.stop();
        log.info("FIX Initiator stopped for session: {}", session.sessionId());
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
}
