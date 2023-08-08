package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools feat attributes ({@code feat2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase Tools5eQuteBase}.
 * </p>
 */
@TemplateData
@RegisterForReflection
public class QuteFeat extends Tools5eQuteBase {

    /** Prerequisite level */
    public final String level;
    /** Formatted text listing other prerequisite conditions */
    public final String prerequisite;

    public QuteFeat(Tools5eSources sources, String name, String source, String prerequisite, String level, String text,
            Tags tags) {
        super(sources, name, source, text, tags);
        this.level = level;
        this.prerequisite = prerequisite; // optional
    }
}
