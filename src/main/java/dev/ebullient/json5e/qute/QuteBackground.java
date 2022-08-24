package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteBackground implements QuteSource {
    final String name;
    public final String source;
    public final List<String> tags;
    public final String text;

    public QuteBackground(String name, String source, String text, List<String> tags) {
        this.name = name;
        this.source = source;
        this.tags = tags == null ? List.of() : tags;
        this.text = text;
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
