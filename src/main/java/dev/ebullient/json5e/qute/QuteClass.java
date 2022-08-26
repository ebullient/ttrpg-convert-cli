package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteClass implements QuteSource {
    final String name;
    public final String source;
    public final int hitDice;
    public final String classProgression;
    public final String startingEquipment;
    public final String multiclassing;
    public final String text;
    public final List<String> tags;

    public QuteClass(String name, String source,
            int hitDice, String classProgression,
            String startingEquipment, String multiclassing,
            String text, List<String> tags) {
        this.name = name;
        this.source = source;
        this.hitDice = hitDice;
        this.classProgression = classProgression;
        this.startingEquipment = startingEquipment;
        this.multiclassing = multiclassing;
        this.text = text;
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

    public int getHitRollAverage() {
        return hitDice / 2 + 1;
    }
}
