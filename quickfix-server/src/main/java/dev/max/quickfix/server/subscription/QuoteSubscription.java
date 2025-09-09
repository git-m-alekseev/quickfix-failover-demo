package dev.max.quickfix.server.subscription;

import dev.max.fix.requests.Quote;

public interface QuoteSubscription {

    Quote lastQuote();

    void cancel();

    void subscribe(QuoteStreamSubscriber streamSubscriber);
}
