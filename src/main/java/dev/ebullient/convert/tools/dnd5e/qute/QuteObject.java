package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools object attributes ({@code object2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase Tools5eQuteBase}.
 * </p>
 */
@TemplateData
public class QuteObject extends Tools5eQuteBase {
    /** True if this is an NPC */
    public final boolean isNpc;
    /** Object size (capitalized) */
    public final String size;
    /** Creature type (lowercase); optional */
    public final String creatureType;
    /** Object type */
    public final String objectType;
    /** Armor class (number) */
    public final Integer ac;
    /** Additional armor class text: natural armor. May link to related items. */
    public final String acText;

    /** Hit points (number); optional */
    public final Integer hp;
    /** Additional hit point text. May link to related items. */
    public final String hpText;

    /** Object speed as a comma-separated list */
    public final String speed;
    /** Object ability scores ({@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores AbilityScores}) */
    public final AbilityScores scores;
    /** Comma-separated string of object senses (if present). */
    public final String senses;
    /** Comma-separated string of creature damage vulnerabilities (if present). */
    public final String vulnerable;
    /** Comma-separated string of creature damage resistances (if present). */
    public final String resist;
    /** Comma-separated string of creature damage immunities (if present). */
    public final String immune;
    /** Comma-separated string of creature condition immunities (if present). */
    public final String conditionImmune;
    /** List of object ({@link dev.ebullient.convert.qute.NamedText actions}) */
    public final Collection<NamedText> action;

    /** Token image as {@link dev.ebullient.convert.qute.ImageRef ImageRef} */
    public final ImageRef token;
    /** List of {@link dev.ebullient.convert.qute.ImageRef ImageRef} related to the creature */
    public final List<ImageRef> fluffImages;

    public QuteObject(CompendiumSources sources,
            String name, String source,
            boolean isNpc, String size,
            String creatureType, String objectType,
            Integer ac, String acText,
            Integer hp, String hpText,
            String speed,
            AbilityScores scores,
            String senses,
            String vulnerable, String resist, String immune, String conditionImmune,
            Collection<NamedText> actions,
            ImageRef tokenImage, List<ImageRef> fluffImages,
            String text, Tags tags) {
        super(sources, name, source, text, tags);
        this.isNpc = isNpc;
        this.size = size;
        this.creatureType = creatureType;
        this.objectType = objectType;

        this.ac = ac;
        this.acText = acText;
        this.hp = hp;
        this.hpText = hpText;

        this.speed = speed;
        this.scores = scores;
        this.senses = senses;
        this.vulnerable = vulnerable;
        this.immune = immune;
        this.conditionImmune = conditionImmune;
        this.resist = resist;
        this.action = actions;

        this.token = tokenImage;
        this.fluffImages = fluffImages;
    }

    /** List of source books (abbreviated name). Fantasy statblock uses this list. */
    public final List<String> getBooks() {
        return getSourceAndPage().stream()
                .map(x -> x.source)
                .toList();
    }

    /**
     * A minimal YAML snippet containing object attributes required by the
     * Initiative Tracker plugin. Use this in frontmatter.
     */
    public String get5eInitiativeYaml() {
        Map<String, Object> map = new LinkedHashMap<>();
        addUnlessEmpty(map, "name", name);
        addIntegerUnlessEmpty(map, "ac", ac);
        addIntegerUnlessEmpty(map, "hp", hp);
        map.put("stats", scores.toArray()); // for initiative
        addUnlessEmpty(map, "source", getBooks());
        return Tui.plainYaml().dump(map).trim();
    }

    /**
     * Complete object attributes in the format required by the Fantasy statblock plugin.
     * Uses double-quoted syntax to deal with a variety of characters occuring in
     * trait descriptions. Usable in frontmatter or Fantasy Statblock code blocks.
     */
    public String get5eStatblockYaml() {
        Map<String, Object> map = new LinkedHashMap<>();
        addUnlessEmpty(map, "name", name);
        addUnlessEmpty(map, "size", size);
        addUnlessEmpty(map, "type", creatureType);

        addIntegerUnlessEmpty(map, "ac", ac);
        addIntegerUnlessEmpty(map, "hp", hp);

        map.put("stats", scores.toArray());
        addUnlessEmpty(map, "speed", speed);

        addUnlessEmpty(map, "damage_vulnerabilities", vulnerable);
        addUnlessEmpty(map, "damage_resistances", resist);
        addUnlessEmpty(map, "damage_immunities", immune);
        addUnlessEmpty(map, "condition_immunities", conditionImmune);
        addUnlessEmpty(map, "senses", senses);

        addUnlessEmpty(map, "actions", action);
        addUnlessEmpty(map, "source", getBooks());
        if (token != null) {
            map.put("image", token.getVaultPath());
        }

        // De-markdown-ify
        return Tui.quotedYaml().dump(map).trim()
                .replaceAll("`", "")
                .replaceAll("\\*([^*]+)\\*", "$1") // em
                .replaceAll("\\*([^*]+)\\*", "$1") // bold
                .replaceAll("\\*([^*]+)\\*", "$1"); // bold em
    }
}
