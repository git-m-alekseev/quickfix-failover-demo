package dev.max.quickfix.server.subscription;

import dev.max.fix.requests.Quote;

public interface QuoteStreamSubscriber {

    void onQuote(Quote quote);

    void onComplete();

    void onError(Throwable error);
}
