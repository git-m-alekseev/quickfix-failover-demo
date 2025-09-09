package dev.max.quickfix.client.api.dto;

public record Quote(
        String instrument,
        double bid,
        double ask
) { }
