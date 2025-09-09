package dev.max.quickfix.client.fix.initiator.subscription;

import dev.max.fix44.custom.messages.ClientQuote;

public interface SubscriptionHandler {

    void onQuote(ClientQuote clientQuote);

    void onFinish();

    void onError(String errorMessage);
}
