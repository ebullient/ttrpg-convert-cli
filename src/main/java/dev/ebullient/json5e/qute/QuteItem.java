package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteItem extends QuteNote {
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

    public QuteItem(String name, String source, String detail, String armorClass, String damage, String damage2h,
            String range, String properties, Integer strengthRequirement, boolean stealthPenalty,
            String costGp, Double weightLbs, String text, List<String> tags) {
        super(name, source, text, tags);

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
}
