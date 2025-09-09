package dev.max.quickfix.server.integration.stub;

import dev.max.fix.requests.Quote;
import dev.max.quickfix.server.integration.MarketDataListener;
import dev.max.quickfix.server.integration.MarketDataSource;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;

import static dev.max.quickfix.server.integration.MarketDataSource.SubscribeResult.ALREADY_EXISTS;
import static dev.max.quickfix.server.integration.MarketDataSource.SubscribeResult.OK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class StubMarketDataSource implements MarketDataSource {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, Double> prices = Map.of(
            "USD/RUB", 75.0,
            "EUR/RUB", 90.0,
            "GBP/RUB", 105.0,
            "JPY/RUB", 0.65
    );

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, getThreadFactory());

    private final ExecutorService quotesProviderExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final ConcurrentMap<String, Subscription> listeners = new ConcurrentHashMap<>();

    @Override
    public SubscribeResult subscribe(String instrument, MarketDataListener listener) {
        if (!prices.containsKey(instrument)) {
            var ex = new RuntimeException("Unknown instrument: " + instrument);
            listener.onError(ex);
        }

        var created = new BooleanWrapper();
        listeners.computeIfAbsent(instrument, key -> {
            log.info("Creating new subscription for {}", key);
            var scheduledFuture = scheduler.scheduleAtFixedRate(() -> supplyQuotes(instrument), 1000L, 2000L, MILLISECONDS);
            created.value = true;
            return new Subscription(scheduledFuture, listener);
        });
        if (created.value) {
            return OK;
        }
        return ALREADY_EXISTS;
    }

    @Override
    public void unsubscribe(String instrument) {
        log.info("Unsubscribing from {}", instrument);
        var subscription = listeners.remove(instrument);
        if (subscription != null) {
            subscription.scheduledFuture.cancel(true);
            subscription.listener.onFinish();
        }
    }

    @Override
    public void completeWithError(String instrument, String reason) {
        var subscription = listeners.remove(instrument);
        if (subscription != null) {
            subscription.scheduledFuture.cancel(true);
            subscription.listener.onError(new RuntimeException(reason));
        }
    }

    private void supplyQuotes(String instrument) {
        log.info("Supplying quotes from {}", instrument);
        listeners.forEach((istr, subscription) -> {
            log.info("Checking the quote instrument {} - {}", istr, instrument);
            if (instrument.equals(istr)) {
                var quote = StubQuoteGenerator.generate(instrument);
                quotesProviderExecutor.execute(() -> {
                    log.info("Supplying quote {}", quote);
                    subscription.listener.onQuote(quote);
                });
            }
        });
    }

    private static ThreadFactory getThreadFactory() {
        return r -> {
            var thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("MarketDataProviderScheduler");
            return thread;
        };
    }

    private static class StubQuoteGenerator {
        public static Quote generate(String instrument) {
            var price = prices.get(instrument);
            var bid = RANDOM.nextDouble(price, price + 10.0);
            var ask = RANDOM.nextDouble(price, price - 10.0);
            return new Quote(
                    instrument,
                    bid,
                    ask);
        }
    }

    private static class BooleanWrapper {
        private boolean value;
    }

    @Data
    @RequiredArgsConstructor
    private static class Subscription {
        private final ScheduledFuture<?> scheduledFuture;
        private final MarketDataListener listener;
    }
}
