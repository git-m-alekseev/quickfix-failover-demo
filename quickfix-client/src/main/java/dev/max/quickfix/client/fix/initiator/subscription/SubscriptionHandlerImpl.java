package dev.max.quickfix.client.fix.initiator.subscription;

import dev.max.fix.utils.MessageUtils;
import dev.max.fix44.custom.messages.ClientQuote;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
@Builder
@RequiredArgsConstructor
public class SubscriptionHandlerImpl implements SubscriptionHandler {

    private final Consumer<ClientQuote> quoteConsumer;
    private final Runnable onFinish;
    private final Consumer<String> onError;

    @Override
    public void onQuote(ClientQuote clientQuote) {
        try {
            quoteConsumer.accept(clientQuote);
        } catch (RuntimeException e) {
            log.error("Couldn't process quote: {}", MessageUtils.toString(clientQuote), e);
        }
    }

    @Override
    public void onFinish() {
        onFinish.run();
    }

    @Override
    public void onError(String errorMessage) {
        onError.accept(errorMessage);
    }
}
