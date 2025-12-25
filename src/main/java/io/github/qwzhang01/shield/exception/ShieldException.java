package io.github.qwzhang01.shield.exception;

public class ShieldException extends RuntimeException {
    public ShieldException(String message) {
        super(message);
    }

    public ShieldException(String message, Throwable cause) {
        super(message, cause);
    }
}
