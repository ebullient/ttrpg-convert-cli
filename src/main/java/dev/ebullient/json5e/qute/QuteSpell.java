package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteSpell extends QuteNote {
    public final String level;
    public final String school;
    public final boolean ritual;
    public final String time;
    public final String range;
    public final String components;
    public final String duration;
    public String classes;

    public QuteSpell(String name, String source, String level,
            String school, boolean ritual, String time, String range,
            String components, String duration,
            String classes, String text, List<String> tags) {
        super(name, source, text, tags);

        this.level = level;
        this.school = school;
        this.ritual = ritual;
        this.time = time;
        this.range = range;
        this.components = components;
        this.duration = duration;
        this.classes = classes;
    }
}
