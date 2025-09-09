package dev.max.fix.requests;

public record ExecutionRequest(
        String clientId,
        String instrument,
        double price,
        double amount
) {
}
