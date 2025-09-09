package dev.max.quickfix.server.subscription;

import dev.max.fix.requests.Quote;
import lombok.Builder;

import java.util.function.Consumer;

@Builder
public class QuoteStreamSubscriberImpl implements QuoteStreamSubscriber {

    private final Consumer<Quote> onQuote;
    private final Runnable onComplete;
    private final Consumer<Throwable> onError;

    @Override
    public void onQuote(Quote quote) {
        onQuote.accept(quote);
    }

    @Override
    public void onComplete() {
        if (onComplete != null) {
            onComplete.run();
        }
    }

    @Override
    public void onError(Throwable error) {
        if (onError != null) {
            onError.accept(error);
        }
    }
}
