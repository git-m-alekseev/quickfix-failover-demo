package dev.max.quickfix.server.subscription;

public interface StreamingQuoteSource {
    QuoteSubscription createSubscription(String instrument);
}
