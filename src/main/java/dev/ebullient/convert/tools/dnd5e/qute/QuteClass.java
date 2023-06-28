package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.Collection;

import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteClass extends QuteSource {
    public final int hitDice;
    public final String classProgression;
    public final String startingEquipment;
    public final String multiclassing;

    public QuteClass(Tools5eSources sources, String name, String source,
            int hitDice, String classProgression,
            String startingEquipment, String multiclassing,
            String text, Collection<String> tags) {
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
        return QuteSource.CLASSES_PATH;
    }
}
