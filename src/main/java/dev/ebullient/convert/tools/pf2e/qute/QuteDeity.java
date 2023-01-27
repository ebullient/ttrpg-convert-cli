package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QuteDeity extends Pf2eQuteBase {

    public final List<String> aliases;
    public final String category;
    public final String pantheon;

    // Morality
    public String alignment;
    public String followerAlignment;

    public final String areasOfConcern;
    public final String edicts;
    public final String anathema;

    public final QuteDeityCleric cleric;
    public final QuteDivineAvatar avatar;
    public final QuteDivineIntercession intercession;

    public QuteDeity(Pf2eSources sources, List<String> text, Collection<String> tags,
            List<String> aliases, String category, String pantheon,
            String alignment, String followerAlignment, String areasOfConcern, String edicts, String anathema,
            QuteDeityCleric cleric, QuteDivineAvatar avatar, QuteDivineIntercession intercession) {
        super(sources, text, tags);
        this.aliases = aliases;
        this.category = category;
        this.pantheon = pantheon;

        this.alignment = alignment;
        this.followerAlignment = followerAlignment;
        this.areasOfConcern = areasOfConcern;
        this.edicts = edicts;
        this.anathema = anathema;

        this.cleric = cleric;
        this.avatar = avatar;
        this.intercession = intercession;
    }

    @RegisterForReflection
    public static class QuteDeityCleric {
        public String divineFont;
        public String divineAbility;
        public String divineSkill;
        public String domains;
        public String alternateDomains;
        public Map<String, String> spells;
        public String favoredWeapon;

        public String toString() {
            List<String> lines = new ArrayList<>();
            if (divineAbility != null) {
                lines.add("- **Divine Ability**: " + divineAbility);
            }
            if (divineFont != null) {
                lines.add("- **Divine Font**: " + divineFont);
            }
            if (divineSkill != null) {
                lines.add("- **Divine Skill**: " + divineSkill);
            }
            if (favoredWeapon != null) {
                lines.add("- **Favored Weapon**: " + favoredWeapon);
            }
            if (domains != null) {
                lines.add("- **Domains**: " + domains);
            }
            if (alternateDomains != null) {
                lines.add("- **Alternate Domains**: " + alternateDomains);
            }
            if (spells != null) {
                lines.add("- **Cleric Spells**: " + spells.entrySet().stream()
                        .map(e -> String.format("%s: %s", e.getKey(), e.getValue()))
                        .collect(Collectors.joining("; ")));
            }
            return String.join("\n", lines);
        }
    }

    @RegisterForReflection
    public static class QuteDivineAvatar {
        public String preface;
        public String name;
        public String speed;
        public String shield;
        public List<QuteDivineAvatarAction> melee;
        public List<QuteDivineAvatarAction> ranged;
        public List<QuteDivineAvatarAbility> ability;

        public String toString() {
            List<String> lines = new ArrayList<>();
            if (preface != null) {
                lines.add(preface);
                lines.add("");
            }
            lines.add("```ad-embed-avatar");
            lines.add("title: " + name);
            lines.add("");
            if (speed != null) {
                lines.add("- **Speed**: " + speed);
            }
            if (shield != null) {
                lines.add("- **Shield**: " + shield);
            }
            melee.forEach(m -> lines.add("- " + m));
            ranged.forEach(r -> lines.add("- " + r));
            ability.forEach(a -> lines.add("- " + a));
            lines.add("```");

            return String.join("\n", lines);
        }
    }

    @RegisterForReflection
    public static class QuteDivineAvatarAction {
        public String actionType;
        public String name;
        public QuteActivityType activityType;
        public List<String> traits;
        public String range;
        public String damage;
        public String note;

        public String toString() {
            List<String> parts = new ArrayList<>();
            parts.add(String.format("**%s**: %s", actionType, activityType));
            parts.add(name);
            if (!traits.isEmpty()) {
                parts.add("(" + String.join(", ", traits) + "),");
            }
            parts.add("**Damage** " + damage);
            if (note != null) {
                parts.add(note);
            }
            return String.join(" ", parts);
        }
    }

    @RegisterForReflection
    public static class QuteDivineAvatarAbility {
        public String name;
        public String text;

        public String toString() {
            return String.format("**%s**: %s", name, text);
        }
    }

    @RegisterForReflection
    public static class QuteDivineIntercession {
        public String sourceText;
        public String flavor;
        public String majorBoon;
        public String moderateBoon;
        public String minorBoon;
        public String majorCurse;
        public String moderateCurse;
        public String minorCurse;
    }
}
