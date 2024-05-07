package dev.ebullient.convert.tools.pf2e.qute;

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
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 * </p>
 */
@TemplateData
public class QuteItem extends Pf2eQuteBase {

    /** Collection of traits (decorated links) */
    public final Collection<String> traits;
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
            Collection<String> traits, List<String> aliases, QuteItemActivate activate,
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
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.activate}`.
     * </p>
     */
    @TemplateData
    public static class QuteItemActivate implements QuteUtil {
        /** Item {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity activity/activation details} */
        public QuteDataActivity activity;
        /** Formatted string. Components required to activate this item */
        public String components;
        /** Formatted string. Trigger to activate this item */
        public String trigger;
        /** Formatted string. How often this item can be used/activated */
        public String frequency;
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
     * <p>
     * <b>AC Bonus</b> +2; <b>Speed Penalty</b> —; <b>Hardness</b> 3; <b>HP (BT)</b> 12 (6)
     * </p>
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
     * Pf2eTools item armor attributes
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.armor}`.
     * </p>
     */
    @TemplateData
    public static class QuteItemArmorData implements QuteUtil {
        /** {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass Item armor class details} */
        public QuteDataArmorClass ac;
        /** Formatted string. Dex cap */
        public String dexCap;
        /** Formatted string. Armor strength */
        public String strength;
        /** Formatted string. Check penalty */
        public String checkPenalty;
        /** Formatted string. Speed penalty */
        public String speedPenalty;

        public String toString() {
            List<String> parts = new ArrayList<>();
            parts.add("**AC Bonus** " + ac.bonus());
            if (isPresent(dexCap)) {
                parts.add("**Dex Cap** " + dexCap);
            }
            if (isPresent(strength)) {
                parts.add("**Strength** " + strength);
            }
            if (isPresent(checkPenalty)) {
                parts.add("**Check Penalty** " + checkPenalty);
            }
            if (isPresent(speedPenalty)) {
                parts.add("**Speed Penalty** " + speedPenalty);
            }
            return "- " + String.join("; ", parts);
        }
    }

    /**
     * Pf2eTools item weapon attributes
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly:<br />
     * ```<br />
     * {#for weapons in resource.weapons}<br />
     * {weapons}<br />
     * {/for}<br />
     * ```<br />
     * or, using `{#each}` instead:<br />
     * ```<br />
     * {#each resource.weapons}<br />
     * {it}<br />
     * {/each}<br />
     * ```
     * </p>
     */

    @TemplateData
    public static class QuteItemWeaponData implements QuteUtil {
        /** Formatted string. Weapon type */
        public String type;
        /** Formatted string. List of traits (links) */
        public Collection<String> traits;
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
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly:<br />
     * ```<br />
     * {#for variants in resource.variants}<br />
     * {variants}<br />
     * {/for}<br />
     * ```<br />
     * or, using `{#each}` instead:<br />
     * ```<br />
     * {#each resource.variants}<br />
     * {it}<br />
     * {/each}<br />
     * ```
     * </p>
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
