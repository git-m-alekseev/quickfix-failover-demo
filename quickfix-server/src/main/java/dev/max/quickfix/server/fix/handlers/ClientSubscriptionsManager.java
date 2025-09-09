package dev.max.quickfix.server.fix.handlers;

import dev.max.fix.requests.Quote;
import dev.max.fix.requests.SubscriptionRequest;
import dev.max.quickfix.server.subscription.QuoteStreamSubscriber;
import dev.max.quickfix.server.subscription.QuoteSubscription;
import dev.max.quickfix.server.subscription.StreamingQuoteSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ClientSubscriptionsManager {

    private final ConcurrentMap<SubscriptionRequest, QuoteSubscription> subscriptions = new ConcurrentHashMap<>();
    private final StreamingQuoteSource quoteSource;

    public void subscribe(SubscriptionRequest request, QuoteStreamSubscriber subscriber) {
        var created = new AtomicBoolean(false);
        subscriptions.computeIfAbsent(request, key -> {
            created.set(true);
            var quoteSubscription = quoteSource.createSubscription(request.instrument());
            quoteSubscription.subscribe(new QuoteStreamSubscriberImpl(request, subscriber));
            return quoteSubscription;
        });
        if (!created.get()) {
            throw new RuntimeException("Already exists");
        }
    }

    public QuoteSubscription getSubscription(SubscriptionRequest request) {
        return subscriptions.get(request);
    }

    public void cancelSubscription(SubscriptionRequest request) {
        var sub = subscriptions.remove(request);
        if (sub != null) {
            sub.cancel();
        }
    }

    @RequiredArgsConstructor
    private class QuoteStreamSubscriberImpl implements QuoteStreamSubscriber {

        private final SubscriptionRequest request;
        private final QuoteStreamSubscriber subscriber;

        @Override
        public void onQuote(Quote quote) {
            subscriber.onQuote(quote);
        }

        @Override
        public void onComplete() {
            subscriptions.remove(request);
            subscriber.onComplete();
        }

        @Override
        public void onError(Throwable error) {
            subscriptions.remove(request);
            subscriber.onError(error);
        }
    }
}
