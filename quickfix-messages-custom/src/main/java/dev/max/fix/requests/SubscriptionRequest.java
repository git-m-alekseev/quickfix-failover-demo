package dev.max.fix.requests;

public record SubscriptionRequest(
        String clientId,
        String instrument
) { }
