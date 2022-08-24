package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteClass implements QuteSource {
    final String name;
    public final String source;
    public final List<String> tags;

    public QuteClass(String name, String source, List<String> tags) {
        this.name = name;
        this.source = source;
        this.tags = tags == null ? List.of() : tags;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSource() {
        return source;
    }
}
