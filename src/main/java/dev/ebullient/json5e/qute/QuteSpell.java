package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteSpell implements QuteSource {
    final String name;
    public final String source;
    public final String level;
    public final String school;
    public final boolean ritual;
    public final String time;
    public final String range;
    public final String components;
    public final String duration;
    public String classes;
    public final String text;
    public final List<String> tags;

    public QuteSpell(String name, String source, String level,
            String school, boolean ritual, String time, String range,
            String components, String duration,
            String classes, String text, List<String> tags) {
        this.name = name;
        this.source = source;
        this.level = level;
        this.school = school;
        this.ritual = ritual;
        this.time = time;
        this.range = range;
        this.components = components;
        this.duration = duration;
        this.classes = classes;
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
