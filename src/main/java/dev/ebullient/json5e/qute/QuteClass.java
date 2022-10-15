package dev.ebullient.json5e.qute;

import java.util.List;

import dev.ebullient.json5e.tools5e.CompendiumSources;

public class QuteClass extends QuteBase {
    public final int hitDice;
    public final String classProgression;
    public final String startingEquipment;
    public final String multiclassing;

    public QuteClass(CompendiumSources sources, String name, String source,
            int hitDice, String classProgression,
            String startingEquipment, String multiclassing,
            String text, List<String> tags) {
        super(sources, name, source, text, tags);

        this.hitDice = hitDice;
        this.classProgression = classProgression;
        this.startingEquipment = startingEquipment;
        this.multiclassing = multiclassing;
    }

    public int getHitRollAverage() {
        return hitDice / 2 + 1;
    }

    @Override
    public String targetPath() {
        return "classes";
    }
}
