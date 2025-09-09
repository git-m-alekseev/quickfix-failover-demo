package dev.max.fix.requests;

public record SubscribeRequest(
        String clientId,
        String instrument
) { }
