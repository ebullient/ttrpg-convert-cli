package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;
import java.util.stream.Collectors;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools item attributes ({@code item2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 * </p>
 */
@TemplateData
public class QuteItem extends Tools5eQuteBase {

    /** Formatted string of item details. Will include some combination of tier, rarity, category, and attunement */
    public final String detail;
    /** Changes to armor class provided by the item, if applicable */
    public final String armorClass;
    /** One-handed Damage string, if applicable. Contains dice formula and damage type */
    public final String damage;
    /** Two-handed Damage string, if applicable. Contains dice formula and damage type */
    public final String damage2h;
    /** Item's range, if applicable */
    public final String range;
    /** Formatted string listing item's properties (with links to rules if the source is present) */
    public final String properties;
    /** Strength requirement as a numerical value, if applicable */
    public final Integer strengthRequirement;
    /** True if the item imposes a stealth penalty, if applicable */
    public final boolean stealthPenalty;
    /** Cost of the item (gp, sp, cp). Usually missing for magic items. */
    public final String cost;
    /** Cost of the item (cp) as number. Usually missing for magic items. */
    public final Integer costCp;
    /** Weight of the item (pounds) as a decimal value */
    public final Double weight;
    /** List of images for this item (as {@link dev.ebullient.convert.qute.ImageRef}) */
    public final List<ImageRef> fluffImages;
    /** Formatted text listing other prerequisite conditions (optional) */
    public final String prerequisite;
    /** List of magic item variants (as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteItem.Variant}, optional) */
    public final List<Variant> variants;

    public QuteItem(Tools5eSources sources, String name, String source, String detail,
            String armorClass, String damage, String damage2h,
            String range, String properties, Integer strengthRequirement, boolean stealthPenalty,
            String costGp, Integer costCp, Double weightLbs, String prerequisite,
            String text, List<ImageRef> images, List<Variant> variants, Tags tags) {
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
        this.costCp = costCp;
        this.weight = weightLbs;
        this.fluffImages = images == null ? List.of() : images;
        this.prerequisite = prerequisite; // optional
        this.variants = variants == null ? List.of() : variants;
    }

    /**
     * String: list (`- "alias"`) of aliases for variants. Use in YAML frontmatter with `aliases:`.
     * Will return an empty string if there are no variants
     */
    public String getVariantAliases() {
        if (variants.isEmpty()) {
            return "";
        }
        return variants.stream()
                .map(x -> String.format("- \"%s\"", x.name))
                .collect(Collectors.joining("\n"));
    }

    /**
     * String: list (`- [name](#anchor)`) of links to variant sections.
     * Will return an empty string if there are no variants.
     */
    public String getVariantSectionLinks() {
        if (variants.isEmpty()) {
            return "";
        }
        return variants.stream()
                .map(x -> String.format("- [%s](#%s)", x.name, Tui.toAnchorTag(x.name)))
                .collect(Collectors.joining("\n"));
    }

    @TemplateData
    @RegisterForReflection
    public static class Variant {
        /** Name of the variant */
        public final String name;
        /** Changes to armor class provided by the item, if applicable */
        public final String armorClass;
        /** One-handed Damage string, if applicable. Contains dice formula and damage type */
        public final String damage;
        /** Two-handed Damage string, if applicable. Contains dice formula and damage type */
        public final String damage2h;
        /** Item's range, if applicable */
        public final String range;
        /** Formatted string listing item's properties (with links to rules if the source is present) */
        public final String properties;
        /** Strength requirement as a numerical value, if applicable */
        public final Integer strengthRequirement;
        /** True if the item imposes a stealth penalty, if applicable */
        public final boolean stealthPenalty;
        /** Cost of the item (gp, sp, cp). Usually missing for magic items. */
        public final String cost;
        /** Cost of the item (cp) as number. Usually missing for magic items. */
        public final Integer costCp;
        /** Weight of the item (pounds) as a decimal value */
        public final Double weight;
        /** Formatted text listing other prerequisite conditions (optional) */
        public final String prerequisite;

        public Variant(String name, String armorClass, String damage, String damage2h,
                String range, String properties, Integer strengthRequirement, boolean stealthPenalty,
                String cost, Integer costCp, Double weightLbs, String prerequisite) {
            this.name = name;
            this.armorClass = armorClass;
            this.damage = damage;
            this.damage2h = damage2h;
            this.range = range;
            this.properties = properties;
            this.strengthRequirement = strengthRequirement;
            this.stealthPenalty = stealthPenalty;
            this.cost = cost;
            this.costCp = costCp;
            this.weight = weightLbs;
            this.prerequisite = prerequisite; // optional
        }
    }
}
