package loxone;

public final class LoxoneCommandFailureException extends Exception {
    LoxoneCommandFailureException(String message) {
        super(message);
    }

    LoxoneCommandFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
