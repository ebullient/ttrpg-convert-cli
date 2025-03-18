package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.formatAsModifier;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.valueOrDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Item attributes
 *
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 */
@TemplateData
public class QuteItem extends Pf2eQuteBase {

    /** Collection of traits (collection of {@link QuteDataRef}) */
    public final Collection<QuteDataRef> traits;
    /** Aliases for this note */
    public final List<String> aliases;
    /**
     * Item activation attributes as {@link dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemActivate QuteItemActivate}
     */
    public final QuteItemActivate activate;
    /** Formatted string. Item price (pp, gp, sp, cp) */
    public final String price;
    /** Formatted string. Crafting requirements */
    public final String craftReq;
    /** Formatted string. Ammunition required */
    public final String ammunition;
    /** Formatted string. Onset attributes */
    public final String onset;
    /** Formatted string. Item power level */
    public final String level;
    /** Formatted string. Item access attributes */
    public final String access;
    /** Formatted string. How long will the item remain active */
    public final String duration;
    /** Formatted string. Item category */
    public final String category;
    /** Formatted string. Item group */
    public final String group;
    /** Formatted string. How many hands does this item require to use */
    public final String hands;
    /** Item use attributes as a list of {@link dev.ebullient.convert.qute.NamedText NamedText} */
    public final Collection<NamedText> usage;
    /** Item contract attributes as a list of {@link dev.ebullient.convert.qute.NamedText NamedText} */
    public final Collection<NamedText> contract;
    /**
     * Item shield attributes as {@link dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemShieldData QuteItemShieldData}
     */
    public final QuteItemShieldData shield;
    /** Item armor attributes as {@link dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemArmorData QuteItemArmorData} */
    public final QuteItemArmorData armor;
    /**
     * Item weapon attributes as list of {@link dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemWeaponData
     * QuteItemWeaponData}
     */
    public final List<QuteItemWeaponData> weapons;
    /** Item variants as list of {@link dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemVariant QuteItemVariant} */
    public final List<QuteItemVariant> variants;

    public QuteItem(Pf2eSources sources, List<String> text, Tags tags,
            Collection<QuteDataRef> traits, List<String> aliases, QuteItemActivate activate,
            String price, String ammunition, String level, String onset, String access,
            String duration, String category, String group,
            String hands, Collection<NamedText> usage, Collection<NamedText> contract,
            QuteItemShieldData shield, QuteItemArmorData armor, List<QuteItemWeaponData> weapons,
            List<QuteItemVariant> variants, String craftReq) {
        super(sources, text, tags);
        this.traits = traits;
        this.aliases = aliases;

        this.activate = activate;
        this.price = price;
        this.ammunition = ammunition;
        this.level = level;
        this.onset = onset;
        this.access = access;
        this.duration = duration;
        this.category = category;
        this.group = group;
        this.usage = usage;
        this.hands = hands;
        this.contract = contract;
        this.shield = shield;
        this.armor = armor;
        this.weapons = weapons;
        this.variants = variants;
        this.craftReq = craftReq;
    }

    /**
     * Pf2eTools item activation attributes.
     *
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.activate}`.
     */
    @TemplateData
    public static class QuteItemActivate implements QuteUtil {
        /** Item {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity activity/activation details} */
        public QuteDataActivity activity;
        /** Formatted string. Components required to activate this item */
        public String components;
        /** Formatted string. Trigger to activate this item */
        public String trigger;
        /**
         * {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataFrequency QuteDataFrequency}.
         * How often this item can be used/activated. Use directly to get a formatted string.
         */
        public QuteDataFrequency frequency;
        /** Formatted string. Requirements for activating this item */
        public String requirements;

        public String toString() {
            List<String> lines = new ArrayList<>();
            if (activity != null || isPresent(components)) {
                lines.add(String.join(" ", List.of(
                        activity == null ? "" : activity.toString(),
                        components == null ? "" : components)).trim());
            }
            if (isPresent(frequency)) {
                lines.add("**Frequency** " + frequency);
            }
            if (isPresent(trigger)) {
                lines.add("**Trigger** " + trigger);
            }
            if (isPresent(requirements)) {
                lines.add("**Requirements** " + requirements);
            }

            return String.join("; ", lines);
        }
    }

    /**
     * Pf2eTools item shield attributes. When referenced directly, provides a default formatting, e.g.
     *
     * ```md
     * **AC Bonus** +2; **Speed Penalty** —; **Hardness** 3; **HP (BT)** 12 (6)
     * ```
     *
     * @param ac AC bonus for the shield, as {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass QuteDataArmorClass}
     *        (required)
     * @param hpHardnessBt HP, hardness, and broken threshold of the shield, as
     *        {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataHpHardnessBt QuteDataHpHardnessBt}
     *        (required)
     * @param speedPenalty Speed penalty for the shield, as a formatted string (string, required)
     */
    @TemplateData
    public record QuteItemShieldData(
            QuteDataArmorClass ac,
            QuteDataHpHardnessBt hpHardnessBt,
            String speedPenalty) implements QuteUtil {

        @Override
        public String toString() {
            return String.join("; ",
                    "**AC Bonus** " + ac.bonus(), "**Speed Penalty** " + speedPenalty, hpHardnessBt.toString());
        }
    }

    /**
     * Armor statistics
     * <blockquote>
     *     <b>AC Bonus</b> +2; <b>Dex Cap</b> +0; <b>Check Penalty</b> -3; <b>Speed Penalty</b> -10 ft; <b>Strength</b> 14
     * </blockquote>
     *
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.armor}`.
     *
     * @param acBonus AC bonus granted by the armor as a {@link QuteDataArmorClass}. Never null.
     * @param dexCap The dex modifier cap that applies while wearing this armor, or null.
     * @param strengthReq The strength requirement to reduce some penalties while wearing the armor, or null.
     * @param checkPenalty The penalty to Strength-and-Dex-based skill checks that apply if the strength requirement is not met.
     *                     Integer, always negative or 0.
     * @param speedPenalty The penalty to speed that applies when wearing this armor. Integer, always negative or 0.
     */
    @TemplateData
    public record QuteItemArmorData(
        QuteDataArmorClass acBonus,
        Integer dexCap,
        Integer strengthReq,
        int checkPenalty,
        int speedPenalty
    ) implements QuteUtil {
        @Override
        public String toString() {
            return join("; ",
                "**AC Bonus** " + acBonus.bonus(),
                "**Dex Cap** " + valueOrDefault(formatAsModifier(dexCap), "—"),
                "**Check Penalty** " + (checkPenalty != 0 ? formatAsModifier(checkPenalty) : "—"),
                "**Speed Penalty** " + (speedPenalty != 0 ? (formatAsModifier(speedPenalty) + " ft.") : "—"),
                "**Strength** " + valueOrDefault(strengthReq, "—"));
        }
    }

    /**
     * Pf2eTools item weapon attributes
     *
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     *
     * To use it, reference it directly:
     *
     * ```md
     * {#for weapons in resource.weapons}
     * {weapons}
     * {/for}
     * ```
     *
     * or, using `{#each}` instead:
     *
     * ```md
     * {#each resource.weapons}
     * {it}
     * {/each}
     * ```
     */
    @TemplateData
    public static class QuteItemWeaponData implements QuteUtil {
        /** Formatted string. Weapon type */
        public String type;
        /** Traits as a {@link QuteDataTraits} */
        public QuteDataTraits traits;
        public Collection<NamedText> ranged;
        public String damage;
        public String group;

        public String toString() {
            String result = "";
            String prefix = type == null ? "" : "  ";

            if (isPresent(type)) {
                result += "- **" + type + "**:  \n";
            }
            if (isPresent(damage)) {
                result += prefix + "- **Damage**: " + damage;
            }
            if (isPresent(ranged)) {
                if (isPresent(damage)) {
                    result += "\n";
                }
                result += prefix + "- " + ranged.stream()
                        .map(e -> e.toString())
                        .collect(Collectors.joining("; "));
            }
            return result;
        }
    }

    /**
     * Pf2eTools item variant attributes
     *
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     *
     * To use it, reference it directly:
     *
     * ```md
     * {#for variants in resource.variants}
     * {variants}
     * {/for}
     * ```
     *
     * or, using `{#each}` instead:
     *
     * ```md
     * {#each resource.variants}
     * {it}
     * {/each}
     * ```
     */
    @TemplateData
    public static class QuteItemVariant implements QuteUtil {
        public String variantType;
        public int level;
        public String price;
        public List<String> entries;
        public List<String> craftReq;

        public String toString() {
            List<String> text = new ArrayList<>();
            text.add(String.format("#### %s *Item %d*", variantType, level));
            text.add("");
            if (isPresent(price)) {
                text.add(String.format("- **Price**: %s", price));
            }
            if (isPresent(craftReq)) {
                text.add("- **Craft Requirements**: " +
                        String.join("; ", craftReq));
            }
            String bodyText = String.join("\n", this.entries);
            if (!bodyText.isBlank()) {
                text.add("");
                text.add(bodyText);
            }

            return String.join("\n", text);
        }
    }
}
