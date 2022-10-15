package dev.ebullient.json5e.qute;

import java.util.List;

import dev.ebullient.json5e.tools5e.CompendiumSources;

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

    public QuteItem(CompendiumSources sources, String name, String source, String detail, String armorClass, String damage,
            String damage2h,
            String range, String properties, Integer strengthRequirement, boolean stealthPenalty,
            String costGp, Double weightLbs, String text, List<String> tags) {
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
    }

    @Override
    public String targetPath() {
        return QuteSource.ITEMS_PATH;
    }
}
