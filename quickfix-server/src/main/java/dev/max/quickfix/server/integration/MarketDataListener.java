package dev.max.quickfix.server.integration;

import dev.max.fix.requests.Quote;

public interface MarketDataListener {

    void onQuote(Quote quote);

    void onError(Throwable error);

    void onFinish();
}
