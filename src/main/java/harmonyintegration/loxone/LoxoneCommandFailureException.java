package harmonyintegration.loxone;

public final class LoxoneCommandFailureException extends Exception {
    LoxoneCommandFailureException(final String message) {
        super(message);
    }

    LoxoneCommandFailureException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
