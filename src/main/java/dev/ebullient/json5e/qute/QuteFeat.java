package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteFeat implements QuteSource {

    final String name;
    public final String source;
    public final String level;
    public final String prerequisite;
    public final String text;
    public final List<String> tags;

    public QuteFeat(String name, String source, String prerequisite, String level, String text, List<String> tags) {
        this.name = name;
        this.source = source;
        this.level = level;
        this.prerequisite = prerequisite; // optional
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
