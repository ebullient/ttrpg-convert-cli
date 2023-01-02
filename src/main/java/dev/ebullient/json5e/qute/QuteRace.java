package dev.ebullient.json5e.qute;

import java.util.List;

import dev.ebullient.json5e.tools5e.CompendiumSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteRace extends QuteBase {

    public final String ability;
    public final String type;
    public final String size;
    public final String speed;
    public final String spellcasting;
    public final String traits;
    public final String description;
    final List<ImageRef> images;

    public QuteRace(CompendiumSources sources, String name, String source,
            String ability, String type, String size, String speed,
            String spellcasting, String traits, String description,
            List<ImageRef> images, List<String> tags) {
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
    public String targetPath() {
        return QuteSource.RACES_PATH;
    }

    @Override
    public List<ImageRef> images() { // not usable by Qute templates
        return images;
    }

    public List<ImageRef> getFluffImages() {
        return images;
    }
}
