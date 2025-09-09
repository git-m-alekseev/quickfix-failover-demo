package dev.max.quickfix.client.api.dto;

public record SubscribeRequest(
        String clientId,
        String instrument
) { }
