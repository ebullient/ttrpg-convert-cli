package dev.ebullient.json5e.qute;

import java.util.Collection;

public class QuteNote implements QuteSource {
    final String name;
    public final String source;
    public final String text;
    public final Collection<String> tags;

    public QuteNote(String name, String source, String text, Collection<String> tags) {
        this.name = name;
        this.source = source;
        this.text = text;
        this.tags = tags;
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
