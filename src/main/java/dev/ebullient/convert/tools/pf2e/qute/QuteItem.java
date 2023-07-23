package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteItem extends Pf2eQuteBase {

    public final Collection<String> traits;
    public final List<String> aliases;

    public final QuteItemActivate activate;
    public final String price;
    public final String craftReq;
    public final String ammunition;
    public final String onset;
    public final String level;
    public final String access;
    public final String duration;
    public final String category;
    public final String group;
    public final String hands;
    public final Map<String, String> usage;
    public final Map<String, String> contract;
    public final QuteItemShieldData shield;
    public final QuteItemArmorData armor;
    public final List<QuteItemWeaponData> weapons;
    public final List<QuteItemVariant> variants;

    public QuteItem(Pf2eSources sources, List<String> text, Tags tags,
            Collection<String> traits, List<String> aliases, QuteItemActivate activate,
            String price, String ammunition, String level, String onset, String access,
            String duration, String category, String group,
            String hands, Map<String, String> usage, Map<String, String> contract,
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

    @TemplateData
    public static class QuteItemActivate {
        public QuteDataActivity activity;
        public String components;
        public String trigger;
        public String frequency;
        public String requirements;

        public String toString() {
            List<String> lines = new ArrayList<>();
            if (activity != null || components != null) {
                lines.add(String.join(" ", List.of(
                        activity == null ? "" : activity.toString(),
                        components == null ? "" : components)).trim());
            }
            if (frequency != null) {
                lines.add("**Frequency** " + frequency);
            }
            if (trigger != null) {
                lines.add("**Trigger** " + trigger);
            }
            if (requirements != null) {
                lines.add("**Requirements** " + requirements);
            }

            return String.join("; ", lines);
        }
    }

    @TemplateData
    public static class QuteItemShieldData {
        public QuteDataArmorClass ac;
        public QuteDataHpHardness hpHardness;
        public String speedPenalty;

        public String toString() {
            List<String> parts = new ArrayList<>();
            if (ac != null) {
                parts.add(ac.toString());
            }
            if (hpHardness != null) {
                parts.add(hpHardness.toString());
            }
            if (speedPenalty != null) {
                parts.add("**Speed Penalty** " + speedPenalty);
            }
            return "- " + String.join("; ", parts);
        }
    }

    @TemplateData
    public static class QuteItemArmorData {
        public QuteDataArmorClass ac;
        public String strength;
        public String checkPenalty;
        public String speedPenalty;

        public String toString() {
            List<String> parts = new ArrayList<>();
            if (strength != null) {
                parts.add("**Strength** " + strength);
            }
            if (checkPenalty != null) {
                parts.add("**Check Penalty** " + checkPenalty);
            }
            if (speedPenalty != null) {
                parts.add("**Speed Penalty** " + speedPenalty);
            }
            return "- " + ac.toString()
                    + "\n- " + String.join("; ", parts);
        }
    }

    @TemplateData
    public static class QuteItemWeaponData {
        public String type;
        public Collection<String> traits;
        public Map<String, String> ranged;
        public String damage;
        public String group;

        public String toString() {
            String result = "";

            String prefix = type == null ? "" : "  ";

            if (type != null) {
                result += "- **" + type + "**  \n";
            }
            if (damage != null) {
                result += prefix + "- **Damage** " + damage;
            }
            if (ranged != null && !ranged.isEmpty()) {
                if (damage != null) {
                    result += "\n";
                }
                result += prefix + "- " + ranged.entrySet().stream()
                        .map(e -> "**" + e.getKey() + "** " + e.getValue())
                        .collect(Collectors.joining("; "));
            }
            return result;
        }
    }

    @TemplateData
    public static class QuteItemVariant {
        public String variantType;
        public int level;
        public String price;
        public List<String> entries;
        public List<String> craftReq;

        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("#### %s *Item %d*\n\n", variantType, level));
            sb.append(String.format("- **Price** %s\n", price));

            if (!craftReq.isEmpty()) {
                sb.append("- **Craft Requirements** ");
                sb.append(String.join("; ", craftReq));
            }
            if (!entries.isEmpty()) {
                sb.append("\n");
                sb.append(String.join("\n", entries));
            }

            return sb.toString();
        }
    }
}
