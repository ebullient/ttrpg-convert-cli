package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools class attributes ({@code class2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase}.
 * </p>
 */
@TemplateData
public class QuteClass extends Tools5eQuteBase {

    /** Hit dice for this class as a single digit: 8 */
    public final int hitDice;

    /** Formatted callout containing class and feature progressions. */
    public final String classProgression;

    /** Formatted text describing starting equipment */
    public final String startingEquipment;

    /** Formatted text section describing how to multiclass with this class */
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

    /**
     * The average roll for a hit die of this class, for example: `add {resource.hitRollAverage}...`
     */
    public int getHitRollAverage() {
        return (hitDice + 1) / 2;
    }
}
