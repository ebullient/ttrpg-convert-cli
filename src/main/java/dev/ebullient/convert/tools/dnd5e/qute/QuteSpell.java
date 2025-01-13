package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;
import java.util.stream.Stream;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools spell attributes ({@code spell2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
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
    /** String: rendered list of links to classes that can use this spell. May be incomplete or empty. */
    public final String classes;

    public QuteSpell(Tools5eSources sources, String name, String source, String level,
            String school, boolean ritual, String time, String range,
            String components, String duration,
            String classes, List<ImageRef> images, String text, Tags tags) {
        super(sources, name, source, images, text, tags);

        this.level = level;
        this.school = school;
        this.ritual = ritual;
        this.time = time;
        this.range = range;
        this.components = components;
        this.duration = duration;
        this.classes = classes;
    }

    /** List of class names that can use this spell. May be incomplete or empty. */
    public List<String> getClassList() {
        return classes == null || classes.isEmpty()
                ? List.of()
                : Stream.of(classes.split(",\\s*"))
                        .map(s -> s.replaceAll("\\[(.*?)\\].*", "$1"))
                        .toList();
    }
}
