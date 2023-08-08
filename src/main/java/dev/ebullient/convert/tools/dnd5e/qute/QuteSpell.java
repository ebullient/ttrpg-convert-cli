package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools spell attributes ({@code spell2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase Tools5eQuteBase}.
 * </p>
 */
@TemplateData
@RegisterForReflection
public class QuteSpell extends Tools5eQuteBase {

    /** Spell level */
    public final String level;
    /** Spell school */
    public final String school;
    /** true for ritual spells */
    public final boolean ritual;
    /** Formatted: casting time */
    public final String time;
    /** Formatted: spell range */
    public final String range;
    /** Formatted: spell components */
    public final String components;
    /** Formatted: spell range */
    public final String duration;
    /** List of links to classes that can use this spell. May be incomplete or empty. */
    public final String classes;
    /** List of images for this spell (as {@link dev.ebullient.convert.qute.ImageRef ImageRef}) */
    public final List<ImageRef> fluffImages;

    public QuteSpell(Tools5eSources sources, String name, String source, String level,
            String school, boolean ritual, String time, String range,
            String components, String duration,
            String classes, String text, List<ImageRef> fluffImages, Tags tags) {
        super(sources, name, source, text, tags);

        this.level = level;
        this.school = school;
        this.ritual = ritual;
        this.time = time;
        this.range = range;
        this.components = components;
        this.duration = duration;
        this.classes = classes;
        this.fluffImages = fluffImages;
    }
}
