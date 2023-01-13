package dev.ebullient.convert.qute;

import java.util.List;

import dev.ebullient.convert.tools.dnd5e.CompendiumSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteItem extends QuteBase {
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

    public QuteItem(CompendiumSources sources, String name, String source, String detail,
            String armorClass, String damage, String damage2h,
            String range, String properties, Integer strengthRequirement, boolean stealthPenalty,
            String costGp, Double weightLbs, String text,
            List<ImageRef> images, List<String> tags) {
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
    public String targetPath() {
        return QuteSource.ITEMS_PATH;
    }

    @Override
    public List<ImageRef> images() { // not usable by qute templates
        return images;
    }

    public List<ImageRef> getFluffImages() {
        return images;
    }
}
