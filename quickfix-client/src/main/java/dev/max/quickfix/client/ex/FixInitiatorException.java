package dev.max.quickfix.client.ex;

public class FixInitiatorException extends RuntimeException{

    public FixInitiatorException(Exception cause) {
        super(cause);
    }

    public FixInitiatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public FixInitiatorException(String message) {
        super(message);
    }
}
