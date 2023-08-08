package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Deity attributes ({@code deity2md.txt})
 * <p>
 * Deities are rendered both standalone and inline (as an admonition block).
 * The default template can render both. It contains
 * some special syntax to handle the inline case.
 * </p>
 * <p>
 * Use `%%--` to mark the end of the preamble (frontmatter and
 * other leading content only appropriate to the standalone case).
 * </p>
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 * </p>
 */
@TemplateData
public class QuteDeity extends Pf2eQuteBase {

    /** Aliases for this note */
    public final List<String> aliases;
    public final String category;
    public final String pantheon;

    // Morality
    public final String alignment;
    public final String followerAlignment;

    public final String areasOfConcern;
    public final String edicts;
    public final String anathema;

    public final QuteDeityCleric cleric;
    public final QuteDivineAvatar avatar;
    public final QuteDivineIntercession intercession;

    public QuteDeity(Pf2eSources sources, List<String> text, Tags tags,
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

    /**
     * Pf2eTools cleric divine attributes
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.actionType}`.
     * </p>
     */
    @TemplateData
    public static class QuteDeityCleric implements QuteUtil {
        public String divineFont;
        public String divineAbility;
        public String divineSkill;
        public String domains;
        public String alternateDomains;
        public Map<String, String> spells;
        public String favoredWeapon;

        public String toString() {
            List<String> lines = new ArrayList<>();
            if (isPresent(divineAbility)) {
                lines.add("- **Divine Ability**: " + divineAbility);
            }
            if (isPresent(divineFont)) {
                lines.add("- **Divine Font**: " + divineFont);
            }
            if (isPresent(divineSkill)) {
                lines.add("- **Divine Skill**: " + divineSkill);
            }
            if (isPresent(favoredWeapon)) {
                lines.add("- **Favored Weapon**: " + favoredWeapon);
            }
            if (domains != null && !domains.isEmpty()) {
                lines.add("- **Domains**: " + domains);
            }
            if (alternateDomains != null && !alternateDomains.isEmpty()) {
                lines.add("- **Alternate Domains**: " + alternateDomains);
            }
            if (isPresent(spells)) {
                lines.add("- **Cleric Spells**: " + spells.entrySet().stream()
                        .map(e -> String.format("%s: %s", e.getKey(), e.getValue()))
                        .collect(Collectors.joining("; ")));
            }
            return String.join("\n", lines);
        }
    }

    /**
     * Pf2eTools avatar attributes
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.actionType}`.
     * </p>
     */
    @TemplateData
    public static class QuteDivineAvatar implements QuteUtil {
        public String preface;
        public String name;
        public String speed;
        public String shield;
        public List<QuteDivineAvatarAction> melee;
        public List<QuteDivineAvatarAction> ranged;
        public Collection<NamedText> ability;

        public String toString() {
            List<String> lines = new ArrayList<>();
            if (isPresent(preface)) {
                lines.add(preface);
                lines.add("");
            }
            lines.add("```ad-embed-avatar");
            lines.add("title: " + name);
            lines.add("");
            if (isPresent(speed)) {
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

    /**
     * Pf2eTools Divine avatar action attributes
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.actionType}`.
     * </p>
     */
    @TemplateData
    public static class QuteDivineAvatarAction {
        public String actionType;
        public String name;
        public QuteDataActivity activityType;
        public Collection<String> traits;
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

    /**
     * Pf2eTools divine avatar ability attributes
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.actionType}`.
     * </p>
     */
    @TemplateData
    public static class QuteDivineAvatarAbility {
        public String name;
        public String text;

        public String toString() {
            return String.format("**%s**: %s", name, text);
        }
    }

    /**
     * Pf2eTools divine intercession attributes.
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.actionType}`.
     * </p>
     */
    @TemplateData
    public static class QuteDivineIntercession {
        public String source;
        public String flavor;
        public String majorBoon;
        public String moderateBoon;
        public String minorBoon;
        public String majorCurse;
        public String moderateCurse;
        public String minorCurse;

        @Deprecated
        public String getSourceText() {
            return source;
        }
    }
}
