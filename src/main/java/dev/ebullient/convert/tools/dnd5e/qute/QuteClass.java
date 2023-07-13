package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteClass extends Tools5eQuteBase {

    public final int hitDice;
    public final String classProgression;
    public final String startingEquipment;
    public final String multiclassing;

    public QuteClass(Tools5eSources sources, String name, String source,
            int hitDice, String classProgression,
            String startingEquipment, String multiclassing,
            String text, Tags tags) {
        super(sources, name, source, text, tags);

        this.hitDice = hitDice;
        this.classProgression = classProgression;
        this.startingEquipment = startingEquipment;
        this.multiclassing = multiclassing;
    }

    public int getHitRollAverage() {
        return hitDice / 2 + 1;
    }
}
