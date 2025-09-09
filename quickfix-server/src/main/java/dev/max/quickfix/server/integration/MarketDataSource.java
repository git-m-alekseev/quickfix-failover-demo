package dev.max.quickfix.server.integration;

public interface MarketDataSource {

    SubscribeResult subscribe(String instrument, MarketDataListener listener);

    void unsubscribe(String instrument);

    void completeWithError(String instrument, String reason);

    enum SubscribeResult {
        OK, ALREADY_EXISTS
    }
}
