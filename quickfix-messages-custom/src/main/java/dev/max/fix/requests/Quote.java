package dev.max.fix.requests;

public record Quote(
        String instrument,
        double bid,
        double ask
) { }
