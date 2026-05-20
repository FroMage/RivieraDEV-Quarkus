package graphql.buffer;

public class BufferException extends RuntimeException {
    public BufferException(String message) {
        super(message);
    }

    public BufferException(String message, Throwable cause) {
        super(message, cause);
    }
}
