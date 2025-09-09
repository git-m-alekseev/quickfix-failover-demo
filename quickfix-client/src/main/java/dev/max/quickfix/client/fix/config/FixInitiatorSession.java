package dev.max.quickfix.client.fix.config;

import quickfix.SessionID;
import quickfix.SessionSettings;

public record FixInitiatorSession(
        String clientId,
        SessionID sessionId,
        SessionSettings settings
) { }
