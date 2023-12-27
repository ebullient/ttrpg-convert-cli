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
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
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

    /** Object AC and HP as {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp} */
    public AcHp acHp;
    /** Object immunities and resistances as {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist} */
    public final ImmuneResist immuneResist;

    /** Object speed as a comma-separated list */
    public final String speed;
    /** Object ability scores as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores}) */
    public final AbilityScores scores;
    /** Comma-separated string of object senses (if present). */
    public final String senses;

    /** Object actions as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public final Collection<NamedText> action;

    /** Token image as {@link dev.ebullient.convert.qute.ImageRef} */
    public final ImageRef token;
    /** List of {@link dev.ebullient.convert.qute.ImageRef} related to the creature */
    public final List<ImageRef> fluffImages;

    public QuteObject(CompendiumSources sources,
            String name, String source,
            boolean isNpc, String size,
            String creatureType, String objectType,
            AcHp acHp, String speed,
            AbilityScores scores,
            String senses,
            ImmuneResist immuneResist,
            Collection<NamedText> actions,
            ImageRef tokenImage, List<ImageRef> fluffImages,
            String text, Tags tags) {
        super(sources, name, source, text, tags);
        this.isNpc = isNpc;
        this.size = size;
        this.creatureType = creatureType;
        this.objectType = objectType;

        this.acHp = acHp;
        this.immuneResist = immuneResist;

        this.speed = speed;
        this.scores = scores;
        this.senses = senses;
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

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#hp} */
    public String getHp() {
        return acHp.getHp();
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#ac} */
    public Integer getAc() {
        return acHp.ac;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#acText} */
    public String getAcText() {
        return acHp.acText;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#hpText} */
    public String getHpText() {
        return acHp.hpText;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#hitDice} */
    public String getHitDice() {
        return acHp.hitDice;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist#vulnerable} */
    public String getVulnerable() {
        return immuneResist.vulnerable;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist#resist} */
    public String getResist() {
        return immuneResist.resist;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist#immune} */
    public String getImmune() {
        return immuneResist.immune;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist#conditionImmune} */
    public String getConditionImmune() {
        return immuneResist.conditionImmune;
    }

    /**
     * A minimal YAML snippet containing object attributes required by the
     * Initiative Tracker plugin. Use this in frontmatter.
     */
    public String get5eInitiativeYaml() {
        Map<String, Object> map = new LinkedHashMap<>();
        addUnlessEmpty(map, "name", name);
        addIntegerUnlessEmpty(map, "ac", acHp.ac);
        addIntegerUnlessEmpty(map, "hp", acHp.hp);
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

        addIntegerUnlessEmpty(map, "ac", acHp.ac);
        addIntegerUnlessEmpty(map, "hp", acHp.hp);

        map.put("stats", scores.toArray());
        addUnlessEmpty(map, "speed", speed);

        addUnlessEmpty(map, "damage_vulnerabilities", immuneResist.vulnerable);
        addUnlessEmpty(map, "damage_resistances", immuneResist.resist);
        addUnlessEmpty(map, "damage_immunities", immuneResist.immune);
        addUnlessEmpty(map, "condition_immunities", immuneResist.conditionImmune);
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
