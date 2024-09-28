package dev.ebullient.convert.tools.dnd5e.qute;

import static dev.ebullient.convert.StringUtil.join;

import java.util.List;
import java.util.stream.Collectors;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools item attributes ({@code item2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteItem extends Tools5eQuteBase {

    /** Detailed information about this item as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteItem.Variant} */
    public final Variant rootVariant;

    /** List of images for this item as {@link dev.ebullient.convert.qute.ImageRef} */
    public final List<ImageRef> fluffImages;
    /** List of magic item variants as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteItem.Variant}. Optional. */
    public final List<Variant> variants;

    public QuteItem(Tools5eSources sources, String source,
            Variant rootVariant, String text, List<ImageRef> images,
            List<Variant> variants, Tags tags) {
        super(sources, rootVariant.name, source, text, tags);
        withTemplate("item2md.txt");

        this.rootVariant = rootVariant;
        this.fluffImages = images == null ? List.of() : images;
        this.variants = variants == null ? List.of() : variants;
    }

    /** Formatted string of item details. Will include some combination of tier, rarity, category, and attunement */
    public String getDetail() {
        return rootVariant.detail();
    }

    /** Formatted string of additional item attributes. Optional. */
    public String getSubtypeString() {
        return rootVariant.subtypeString();
    }

    /** Formatted string listing item's properties (with links to rules if the source is present) */
    public String getProperties() {
        return rootVariant.getProperties();
    }

    /** Formatted string listing applicable item mastery (with links to rules if the source is present) */
    public String getMastery() {
        return rootVariant.getMastery();
    }

    /** Changes to armor class provided by the item, if applicable */
    public String getArmorClass() {
        return rootVariant.armorClass;
    }

    /** One-handed Damage string, if applicable. Contains dice formula and damage type */
    public String getDamage() {
        return rootVariant.damage;
    }

    /** Two-handed Damage string, if applicable. Contains dice formula and damage type */
    public String getDamage2h() {
        return rootVariant.damage2h;
    }

    /** Item's range, if applicable */
    public String getRange() {
        return rootVariant.range;
    }

    /** Strength requirement as a numerical value, if applicable */
    public Integer getStrengthRequirement() {
        return rootVariant.strengthRequirement;
    }

    /** True if the item imposes a stealth penalty, if applicable */
    public boolean getStealthPenalty() {
        return rootVariant.stealthPenalty;
    }

    /** Formatted text listing other prerequisite conditions (optional) */
    public String getPrerequisite() {
        return rootVariant.prerequisite;
    }

    /** Cost of the item (gp, sp, cp). Optional. */
    public String getCost() {
        return rootVariant.cost;
    }

    /** Cost of the item (cp) as number. Optional. */
    public Integer getCostCp() {
        return rootVariant.costCp;
    }

    /** Weight of the item (pounds) as a decimal value */
    public Double getWeight() {
        return rootVariant.weight;
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

    /**
     * @param name Name of the variant.
     * @param detail Formatted string of item details. Will include some combination of tier, rarity, category, and attunement
     * @param subtypeString Item subtype string. Optional.
     * @param baseItem Markdown link to base item. Optional.
     * @param type Item type
     * @param typeAlt Alternate item type. Optional.
     * @param propertiesList List of item's properties (with links to rules if the source is present).
     * @param masteryList List of item mastery that apply to this item.
     *
     * @param armorClass Changes to armor class provided by the item. Optional.
     * @param weaponCategory Weapon category. Optional. One of: "simple", "martial".
     * @param damage One-handed Damage string. Contains dice formula and damage type. Optional.
     * @param damage2h Two-handed Damage string. Contains dice formula and damage type. Optional.
     * @param range Item's range. Optional.
     * @param strengthRequirement Strength requirement as a numerical value. Optional.
     * @param stealthPenalty True if the item imposes a stealth penalty. Optional.
     * @param prerequisite Formatted text listing other prerequisite conditions. Optional.
     *
     * @param age Age/Era of item. Optional. Known values: futuristic, industrial, modern, renaissance, victorian.
     * @param cost Cost of the item (gp, sp, cp). Usually missing for magic items.
     * @param costCp Cost of the item (cp) as number. Usually missing for magic items.
     * @param weight Weight of the item (pounds) as a decimal value.
     * @param rarity Item rarity. Optional. One of: "none": mundane items; "unknown (magic)": miscellaneous magical items;
     *        "unknown": miscellaneous mundane items; "varies": item groups or magic variants.
     * @param tier Item tier. Optional. One of: "minor", "major".
     * @param attunement Attunement requirements. Optional. One of: required, optional, prerequisites/conditions (implies
     *        required).
     *
     * @param ammo True if this is ammunition
     * @param cursed True if this is a cursed item
     * @param firearm True if this is a firearm
     * @param focus True if this is a spellcasting focus.
     * @param focusType Spellcasting focus type. Optional. One of: "arcane", "druid", "holy", and/or a list of required classes.
     * @param poison True if this is a poison.
     * @param poisonTypes Poison type(s). Optional.
     * @param staff True if this is a staff
     * @param tattoo True if this is a tattoo
     * @param wondrous True if this is a wondrous item
     */
    @TemplateData
    public static record Variant(
            String name,
            String detail,
            String subtypeString,
            String baseItem,
            String type,
            String typeAlt,
            List<String> propertiesList,
            List<String> masteryList,
            // ---
            String armorClass,
            String weaponCategory,
            String damage,
            String damage2h,
            String range,
            Integer strengthRequirement,
            boolean stealthPenalty,
            String prerequisite,
            // ---
            String age,
            String cost,
            Integer costCp,
            Double weight,
            String rarity,
            String tier,
            String attunement,
            // ---
            boolean ammo,
            boolean cursed,
            boolean firearm,
            boolean focus,
            String focusType,
            boolean poison,
            String poisonTypes,
            boolean staff,
            boolean tattoo,
            boolean wondrous) {
        /** Formatted string listing item's properties (with links to rules if the source is present) */
        public String getProperties() {
            return join(", ", propertiesList);
        }

        /** Formatted string listing applicable item mastery (with links to rules if the source is present) */
        public String getMastery() {
            return join(", ", masteryList);
        }
    }
}
