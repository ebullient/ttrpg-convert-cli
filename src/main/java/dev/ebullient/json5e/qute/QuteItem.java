package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteItem implements QuteSource {
    final String name;
    public final String source;
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
    public final String text;
    public final List<String> tags;

    public QuteItem(String name, String source, String detail, String armorClass, String damage, String damage2h,
            String range, String properties, Integer strengthRequirement, boolean stealthPenalty,
            String costGp, Double weightLbs, String text, List<String> tags) {
        this.name = name;
        this.source = source;
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
        this.text = text;
        this.tags = tags == null ? List.of() : tags;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSource() {
        return source;
    }
}
