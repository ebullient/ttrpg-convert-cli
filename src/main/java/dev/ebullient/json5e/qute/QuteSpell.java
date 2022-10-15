package dev.ebullient.json5e.qute;

import java.util.List;

import dev.ebullient.json5e.tools5e.CompendiumSources;

public class QuteSpell extends QuteBase {
    public final String level;
    public final String school;
    public final boolean ritual;
    public final String time;
    public final String range;
    public final String components;
    public final String duration;
    public final String classes;

    public QuteSpell(CompendiumSources sources, String name, String source, String level,
            String school, boolean ritual, String time, String range,
            String components, String duration,
            String classes, String text, List<String> tags) {
        super(sources, name, source, text, tags);

        this.level = level;
        this.school = school;
        this.ritual = ritual;
        this.time = time;
        this.range = range;
        this.components = components;
        this.duration = duration;
        this.classes = classes;
    }

    @Override
    public String targetPath() {
        return QuteSource.SPELLS_PATH;
    }
}
