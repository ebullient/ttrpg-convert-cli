package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools race attributes ({@code race2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteRace extends Tools5eQuteBase {

    /** Ability scores associated with this race (comma-separated list of scores or choices) */
    public final String ability;
    /** type of race or subrace (humanoid, ooze, undead, etc.) */
    public final String type;
    /** Size: Small or Medium */
    public final String size;
    /** Speed: 30 ft. May include additional values, like flight or swim speed. */
    public final String speed;
    /** Spellcasting ability score */
    public final String spellcasting;
    /** Formatted text with subsections describing racial traits */
    public final String traits;
    /** Formatted text describing the race. Optional. Same as {resource.text} */
    public final String description;
    /** List of images for this race (as {@link dev.ebullient.convert.qute.ImageRef}) */
    public final List<ImageRef> fluffImages;

    public QuteRace(Tools5eSources sources, String name, String source,
            String ability, String type, String size, String speed,
            String spellcasting, String traits, String description,
            List<ImageRef> images, Tags tags) {
        super(sources, name, source, description, tags);
        this.ability = ability;
        this.type = type;
        this.size = size;
        this.speed = speed;
        this.spellcasting = spellcasting;
        this.traits = traits;
        this.description = description;
        this.fluffImages = images;
    }
}
