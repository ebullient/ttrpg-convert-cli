package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteRace extends Tools5eQuteBase {

    public final String ability;
    public final String type;
    public final String size;
    public final String speed;
    public final String spellcasting;
    public final String traits;
    public final String description;
    final List<ImageRef> images;

    public QuteRace(Tools5eSources sources, String name, String source,
            String ability, String type, String size, String speed,
            String spellcasting, String traits, String description,
            List<ImageRef> images, Tags tags) {
        super(sources, name, source, null, tags);
        this.ability = ability;
        this.type = type;
        this.size = size;
        this.speed = speed;
        this.spellcasting = spellcasting;
        this.traits = traits;
        this.description = description;
        this.images = images;
    }

    @Override
    public List<ImageRef> images() { // not usable by Qute templates
        return images;
    }

    public List<ImageRef> getFluffImages() {
        return images;
    }
}
