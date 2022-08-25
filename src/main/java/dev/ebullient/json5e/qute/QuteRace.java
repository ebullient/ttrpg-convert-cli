package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteRace implements QuteSource {
    final String name;
    public final String source;
    public final String ability;
    public final String type;
    public final String size;
    public final String speed;
    public final String spellcasting;
    public final String traits;
    public final String description;
    public final List<String> tags;

    public QuteRace(String name, String source,
            String ability, String type, String size, String speed,
            String spellcasting, String traits, String description,
            List<String> tags) {
        this.name = name;
        this.source = source;
        this.ability = ability;
        this.type = type;
        this.size = size;
        this.speed = speed;
        this.spellcasting = spellcasting;
        this.traits = traits;
        this.description = description;
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
