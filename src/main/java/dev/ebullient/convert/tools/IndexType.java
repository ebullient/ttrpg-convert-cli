package dev.ebullient.convert.tools;

public interface IndexType {

    String name();

    String templateName();

    enum IndexElement implements NodeReader {
        name,
        source,
    }
}
