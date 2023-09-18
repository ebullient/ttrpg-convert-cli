package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools item attributes ({@code item2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase}.
 * </p>
 */
@TemplateData
@RegisterForReflection
public class QuteItem extends Tools5eQuteBase {

    /** Item details: tier, rarity, category, attunement */
    public final String detail;
    /** Changes to armor class provided by the item, if applicable */
    public final String armorClass;
    /** One-handed Damage string, if applicable. Contains dice formula and damage type */
    public final String damage;
    /** Two-handed Damage string, if applicable. Contains dice formula and damage type */
    public final String damage2h;
    /** Item's range, if applicable */
    public final String range;
    /** List of item's properties (with links to rules if the source is present) */
    public final String properties;
    /** Strength requirement as a numerical value, if applicable */
    public final Integer strengthRequirement;
    /** True if the item imposes a stealth penalty, if applicable */
    public final boolean stealthPenalty;
    /** Cost of the item (gp, sp, cp). Usually missing for magic items. */
    public final String cost;
    /** Weight of the item (pounds) as a decimal value */
    public final Double weight;
    /** List of images for this item (as {@link dev.ebullient.convert.qute.ImageRef}) */
    public final List<ImageRef> fluffImages;
    /** Formatted text listing other prerequisite conditions (optional) */
    public final String prerequisite;

    public QuteItem(Tools5eSources sources, String name, String source, String detail,
            String armorClass, String damage, String damage2h,
            String range, String properties, Integer strengthRequirement, boolean stealthPenalty,
            String costGp, Double weightLbs, String prerequisite,
            String text, List<ImageRef> images, Tags tags) {
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
        this.fluffImages = images;
        this.prerequisite = prerequisite; // optional
    }
}
