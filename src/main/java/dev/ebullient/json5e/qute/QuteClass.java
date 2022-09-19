package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteClass extends QuteNote {
    public final int hitDice;
    public final String classProgression;
    public final String startingEquipment;
    public final String multiclassing;

    public QuteClass(String name, String source,
            int hitDice, String classProgression,
            String startingEquipment, String multiclassing,
            String text, List<String> tags) {
        super(name, source, text, tags);

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
