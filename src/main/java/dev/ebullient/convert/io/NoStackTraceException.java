package dev.ebullient.convert.io;

import java.util.ArrayList;
import java.util.List;

public class NoStackTraceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoStackTraceException(Throwable cause) {
        super(flattenMessage(cause));
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }

    private static String flattenMessage(Throwable cause) {
        List<String> sb = new ArrayList<>();
        while (cause != null) {
            sb.add(cause.toString());
            cause = cause.getCause();
        }
        return String.join("\n", sb);
    }
}
