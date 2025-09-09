package dev.max.quickfix.server.subscription;

public class SubscriberException extends RuntimeException {

    private boolean stopSubscription;

    public SubscriberException(String message, boolean stopSubscription) {
        super(message);
        this.stopSubscription = stopSubscription;
    }

    public SubscriberException(String message, boolean stopSubscription, Throwable cause) {
        super(message, cause);
        this.stopSubscription = stopSubscription;
    }
}
