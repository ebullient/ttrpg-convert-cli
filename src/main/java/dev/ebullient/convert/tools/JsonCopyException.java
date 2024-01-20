package dev.ebullient.convert.tools;

public class JsonCopyException extends RuntimeException {
    public JsonCopyException(String message) {
        super(message);
    }

    public JsonCopyException(String message, Throwable cause) {
        super(message, cause);
    }
}
