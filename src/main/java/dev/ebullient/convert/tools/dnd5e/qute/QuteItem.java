package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteItem extends Tools5eQuteBase {

    public final String detail;
    public final String armorClass;
    public final String damage;
    public final String damage2h;
    public final String range;
    public final String properties;
    public final Integer strengthRequirement;
    public final boolean stealthPenalty;
    public final String cost;
    public final Double weight;
    public final List<ImageRef> images;

    public QuteItem(Tools5eSources sources, String name, String source, String detail,
            String armorClass, String damage, String damage2h,
            String range, String properties, Integer strengthRequirement, boolean stealthPenalty,
            String costGp, Double weightLbs, String text,
            List<ImageRef> images, Tags tags) {
        super(sources, name, source, text, tags);

        this.detail = detail;
        this.armorClass = armorClass;
        this.damage = damage;
        this.damage2h = damage2h;
        this.range = range;
        this.properties = properties;
        this.strengthRequirement = strengthRequirement;
        this.stealthPenalty = stealthPenalty;
        this.cost = costGp;
        this.weight = weightLbs;
        this.images = images;
    }

    @Override
    public List<ImageRef> images() { // not usable by qute templates
        return images;
    }

    public List<ImageRef> getFluffImages() {
        return images;
    }
}
