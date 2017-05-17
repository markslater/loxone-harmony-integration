package loxone;

public interface Loxone extends AutoCloseable {
    void dimLights() throws LoxoneCommandFailureException;
}
