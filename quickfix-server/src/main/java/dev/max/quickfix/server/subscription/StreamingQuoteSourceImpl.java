package dev.max.quickfix.server.subscription;

import dev.max.fix.requests.Quote;
import dev.max.quickfix.server.integration.MarketDataListener;
import dev.max.quickfix.server.integration.MarketDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class StreamingQuoteSourceImpl implements StreamingQuoteSource {

    private final ConcurrentMap<String, QuoteSubscriptionImpl> subscriptions = new ConcurrentHashMap<>();
    private final MarketDataSource marketDataSource;

    @Override
    public QuoteSubscription createSubscription(String instrument) {
        return subscriptions.compute(instrument, (rq, sub) -> {
            if (sub == null) {
                var subscription = new QuoteSubscriptionImpl();
                var listener = new MarketDataListenerImpl(subscription);
                marketDataSource.subscribe(instrument, listener);
                return subscription;
            } else {
                return sub;
            }
        });
    }

    @RequiredArgsConstructor
    private static class MarketDataListenerImpl implements MarketDataListener {

        private final QuoteSubscriptionSink sink;

        @Override
        public void onQuote(Quote quote) {
            sink.emmit(quote);
        }

        @Override
        public void onError(Throwable error) {
            sink.emmitError(error);
        }

        @Override
        public void onFinish() {
            sink.emmitComplete();
        }
    }

    @RequiredArgsConstructor
    static class QuoteSubscriptionImpl implements QuoteSubscriptionSink, QuoteSubscription {

        private final AtomicReference<Quote> lastQuote = new AtomicReference<>();
        private final BlockingDeque<Quote> quotes = new LinkedBlockingDeque<>(256);
        private final AtomicReference<SubscriptionContext> ctxRef = new AtomicReference<>();
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final ExecutorService emitter = Executors.newVirtualThreadPerTaskExecutor();

        @Override
        public void emmit(Quote quote) {
            while (!quotes.offerLast(quote)) {
                quotes.pollFirst();
            }
        }

        @Override
        public void emmitError(Throwable error) {
            var ctx = ctxRef.getAndSet(null);
            ctx.publishTask.cancel(true);
            ctx.subscriber.onError(error);
        }

        @Override
        public void emmitComplete() {
            var ctx = ctxRef.getAndSet(null);
            ctx.publishTask.cancel(true);
            ctx.subscriber.onComplete();
        }


        @Override
        public Quote lastQuote() {
            return lastQuote.get();
        }

        @Override
        public void cancel() {
            var ctx = ctxRef.getAndSet(null);
            ctx.publishTask.cancel(true);
            ctx.subscriber.onComplete();
        }

        @Override
        public void subscribe(QuoteStreamSubscriber streamSubscriber) {
            var task = scheduler.scheduleAtFixedRate(() -> consumeQuote(streamSubscriber), 1000L, 2000L, TimeUnit.SECONDS);
            var ctx = new SubscriptionContext(streamSubscriber, task);
            ctxRef.set(ctx);
        }

        private void consumeQuote(QuoteStreamSubscriber subscriber) {
            while (true) {
                try {
                    var quote = quotes.take();
                    lastQuote.set(quote);
                    emitter.execute(() -> subscriber.onQuote(quote));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private record SubscriptionContext(
            QuoteStreamSubscriber subscriber,
            ScheduledFuture<?> publishTask
    ) { }

    interface QuoteSubscriptionSink {

        void emmit(Quote quote);

        void emmitError(Throwable error);

        void emmitComplete();
    }
}
