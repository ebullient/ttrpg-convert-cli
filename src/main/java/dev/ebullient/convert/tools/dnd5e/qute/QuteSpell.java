package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteSpell extends QuteBase {
    public final String level;
    public final String school;
    public final boolean ritual;
    public final String time;
    public final String range;
    public final String components;
    public final String duration;
    public final String classes;
    final List<ImageRef> fluffImages;

    public QuteSpell(Tools5eSources sources, String name, String source, String level,
            String school, boolean ritual, String time, String range,
            String components, String duration,
            String classes, String text, List<ImageRef> fluffImages, List<String> tags) {
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

    @Override
    public String targetPath() {
        return QuteSource.SPELLS_PATH;
    }

    @Override
    public List<ImageRef> images() { // not usable by Qute templates
        return fluffImages;
    }

    public List<ImageRef> getFluffImages() {
        return fluffImages;
    }
}
