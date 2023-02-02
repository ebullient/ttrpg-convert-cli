package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.QuteDeity;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteDeity extends Json2QuteBase {

    public Json2QuteDeity(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.background, rootNode);
    }

    @Override
    protected QuteDeity buildQuteResource() {
        List<String> text = new ArrayList<>();
        Set<String> tags = new TreeSet<>(sources.getSourceTags());

        Pf2eDeity.domains.getListOfStrings(rootNode, tui()).forEach(d -> tags.add(cfg().tagOf("domain", d, "deity")));
        Pf2eDeity.alternateDomains.getListOfStrings(rootNode, tui()).forEach(d -> tags.add(cfg().tagOf("domain", d, "deity")));

        String category = Pf2eDeity.category.getTextOrDefault(rootNode, "Deity");
        tags.add(cfg().tagOf("deity", category));

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        JsonNode alignNode = Pf2eDeity.alignment.getFrom(rootNode);
        String alignment = join(", ", toAlignments(alignNode, Pf2eDeity.alignment));
        String followerAlignment = join(", ", toAlignments(alignNode, Pf2eDeity.followerAlignment));

        return new QuteDeity(sources, text, tags,
                Field.alias.transformListFrom(rootNode, this),
                category,
                join(", ", Pf2eDeity.pantheon.linkifyListFrom(rootNode, Pf2eIndexType.deity, this)),
                alignment, followerAlignment,
                Pf2eDeity.areasOfConcern.transformTextFrom(rootNode, ", ", this),
                commandmentToString(Pf2eDeity.edict.transformListFrom(rootNode, this)),
                commandmentToString(Pf2eDeity.anathema.transformListFrom(rootNode, this)),
                buildCleric(),
                buildAvatar(tags),
                buildIntercession());
    }

    private QuteDeity.QuteDivineIntercession buildIntercession() {
        JsonNode node = Pf2eDeity.intercession.getFrom(rootNode);
        if (node == null) {
            return null;
        }
        QuteDeity.QuteDivineIntercession intercession = new QuteDeity.QuteDivineIntercession();
        intercession.sourceText = Pf2eSources.createEmbeddedSource(node).getSourceText();
        intercession.flavor = Pf2eDeity.flavor.transformTextFrom(node, "\n", this);

        intercession.majorBoon = Pf2eDeity.majorBoon.transformTextFrom(node, "\n", this);
        intercession.moderateBoon = Pf2eDeity.moderateBoon.transformTextFrom(node, "\n", this);
        intercession.minorBoon = Pf2eDeity.minorBoon.transformTextFrom(node, "\n", this);

        intercession.majorCurse = Pf2eDeity.majorCurse.transformTextFrom(node, "\n", this);
        intercession.moderateCurse = Pf2eDeity.moderateCurse.transformTextFrom(node, "\n", this);
        intercession.minorCurse = Pf2eDeity.minorCurse.transformTextFrom(node, "\n", this);

        return intercession;
    }

    QuteDeity.QuteDeityCleric buildCleric() {
        QuteDeity.QuteDeityCleric cleric = new QuteDeity.QuteDeityCleric();
        cleric.divineFont = join(" or ", Pf2eDeity.font.linkifyListFrom(rootNode, Pf2eIndexType.spell, this));

        EntryAndSomething entryAndSomething = Pf2eDeity.divineAbility.fieldFromTo(rootNode, EntryAndSomething.class, tui());
        if (entryAndSomething != null) {
            cleric.divineAbility = entryAndSomething.buildAbilityString(this);
        }
        entryAndSomething = Pf2eDeity.divineSkill.fieldFromTo(rootNode, EntryAndSomething.class, tui());
        if (entryAndSomething != null) {
            cleric.divineSkill = entryAndSomething.buildSkillString(this);
        }

        entryAndSomething = Pf2eDeity.favoredWeapon.fieldFromTo(rootNode, EntryAndSomething.class, tui());
        if (entryAndSomething != null) {
            cleric.favoredWeapon = entryAndSomething.buildFavoredWeapon(this);
        }

        cleric.domains = join(", ", Pf2eDeity.domains.linkifyListFrom(rootNode, Pf2eIndexType.domain, this));
        cleric.alternateDomains = join(", ",
                Pf2eDeity.alternateDomains.linkifyListFrom(rootNode, Pf2eIndexType.domain, this));

        cleric.spells = new TreeMap<>();
        Map<String, List<String>> clericSpells = Pf2eDeity.spells.fieldFromTo(rootNode, Tui.MAP_STRING_LIST_STRING, tui());
        if (clericSpells != null) {
            clericSpells.forEach((k, v) -> cleric.spells.put(getOrdinalForm(k), v.stream()
                    .map(s -> linkify(Pf2eIndexType.spell, s))
                    .collect(Collectors.joining(", "))));
        }

        return cleric;
    }

    QuteDeity.QuteDivineAvatar buildAvatar(Collection<String> tags) {
        JsonNode avatarNode = Pf2eDeity.avatar.getFrom(rootNode);
        if (avatarNode == null) {
            return null;
        }

        QuteDeity.QuteDivineAvatar avatar = new QuteDeity.QuteDivineAvatar();
        avatar.preface = replaceText(Pf2eDeity.preface.getTextOrNull(avatarNode));
        avatar.name = linkify(Pf2eIndexType.spell, "avatar||Avatar") + " of " + sources.getName();

        Speed speed = Pf2eDeity.speed.fieldFromTo(avatarNode, Speed.class, tui());
        avatar.speed = speed.speedToString(this);

        List<String> notes = new ArrayList<>();
        if (Pf2eDeity.airWalk.booleanOrDefault(avatarNode, false)) {
            notes.add(linkify(Pf2eIndexType.spell, "air walk"));
        }
        String immunities = joinConjunct(" and ",
                Pf2eDeity.immune.linkifyListFrom(avatarNode, Pf2eIndexType.condition, this));
        if (!immunities.isEmpty()) {
            notes.add("immune to " + immunities);
        }
        if (Pf2eDeity.ignoreTerrain.booleanOrDefault(avatarNode, false)) {
            notes.add(replaceText(
                    "ignore {@quickref difficult terrain||3|terrain} and {@quickref greater difficult terrain||3|terrain}"));
        }
        if (speed.speedNote != null) {
            notes.add(speed.speedNote);
        }
        if (!notes.isEmpty()) {
            avatar.speed += ", " + join(", ", notes);
        }

        String shield = Pf2eDeity.shield.getTextOrNull(avatarNode);
        if (shield != null) {
            avatar.shield = "shield (" + shield + " Hardness, can't be damaged)";
        }

        avatar.melee = Pf2eDeity.melee.streamOf(avatarNode)
                .map(n -> buildAvatarAction(n, tags))
                .collect(Collectors.toList());
        avatar.ranged = Pf2eDeity.ranged.streamOf(avatarNode)
                .map(n -> buildAvatarAction(n, tags))
                .collect(Collectors.toList());
        avatar.ability = Pf2eDeity.ability.streamOf(avatarNode)
                .map(this::buildAvatarAbility)
                .collect(Collectors.toList());

        return avatar;
    }

    private QuteDeity.QuteDivineAvatarAbility buildAvatarAbility(JsonNode abilityNode) {
        QuteDeity.QuteDivineAvatarAbility ability = new QuteDeity.QuteDivineAvatarAbility();
        ability.name = Field.name.getTextOrNull(abilityNode);
        ability.text = Field.entries.transformTextFrom(abilityNode, "; ", this);
        return ability;
    }

    private QuteDeity.QuteDivineAvatarAction buildAvatarAction(JsonNode actionNode, Collection<String> tags) {
        QuteDeity.QuteDivineAvatarAction action = new QuteDeity.QuteDivineAvatarAction();

        action.name = Field.name.getTextOrDefault(actionNode, "attack");

        action.traits = collectTraitsFrom(actionNode, tags);
        action.traits.addAll(Pf2eDeity.preciousMetal.getListOfStrings(actionNode, tui()));
        String traitNote = Pf2eDeity.traitNote.getTextOrNull(actionNode);
        if (traitNote != null) {
            action.traits.add(traitNote);
        }
        String ranged = findRange(actionNode);

        action.actionType = ranged == null ? "Melee" : "Ranged";
        action.activityType = Pf2eTypeActivity.single.toQuteActivityType(this, null);
        action.damage = Pf2eWeaponData.getDamageString(actionNode, this);
        action.note = replaceText(Pf2eDeity.note.getTextOrNull(actionNode));
        return action;
    }

    private String findRange(JsonNode actionNode) {
        int range = Pf2eDeity.range.intOrDefault(actionNode, 0);
        int reload = Pf2eDeity.reload.intOrDefault(actionNode, 0);
        boolean rangedIncrement = Pf2eDeity.rangedIncrement.booleanOrDefault(actionNode, false);
        if (range == 0) {
            return null;
        }

        String rangeString = String.format("{@trait range||range %s%s feet}",
                (reload > 0 || rangedIncrement) ? "increment " : "",
                range);

        if (reload > 0) {
            rangeString += String.format(", {@trait reload||reload %s", reload);
        }
        return replaceText(rangeString);
    }

    private List<String> toAlignments(JsonNode alignNode, Pf2eDeity alignmentField) {
        return alignmentField.getListOfStrings(alignNode, tui()).stream()
                .map(a -> a.length() > 2 ? a : linkify(Pf2eIndexType.trait, a.toUpperCase()))
                .collect(Collectors.toList());
    }

    String commandmentToString(List<String> edictOrAnathema) {
        if (edictOrAnathema.stream().anyMatch(x -> x.contains(","))) {
            return String.join("; ", edictOrAnathema);
        }
        return String.join(", ", edictOrAnathema);
    }

    @RegisterForReflection
    static class EntryAndSomething {
        public String entry;
        public List<String> abilities;
        public List<String> skills;
        public List<String> weapons;

        String buildAbilityString(JsonSource convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            return convert.joinConjunct(" or ", abilities.stream()
                    .map(convert::toTitleCase).collect(Collectors.toList()));
        }

        public String buildSkillString(JsonSource convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            return skills.stream()
                    .map(s -> convert.linkify(Pf2eIndexType.spell, convert.toTitleCase(s)))
                    .collect(Collectors.joining(", "));
        }

        public String buildFavoredWeapon(JsonSource convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            return weapons.stream()
                    .map(w -> convert.linkify(Pf2eIndexType.item, w))
                    .collect(Collectors.joining(", "));
        }
    }

    enum Pf2eDeity implements NodeReader {
        ability, // avatar
        airWalk, // avatar
        alignment,
        alternateDomains,
        anathema,
        areasOfConcern,
        avatar,
        category,
        cleric,
        damage,
        damageType,
        damage2,
        damageType2,
        divineAbility, // cleric
        divineSkill, // cleric
        domains,
        edict,
        favoredWeapon, // cleric
        flavor, // intercession
        followerAlignment, // alignment
        font, // cleric
        ignoreTerrain, // avatar
        immune, // avatar
        intercession,
        majorBoon("Major Boon"),
        moderateBoon("Moderate Boon"),
        minorBoon("Minor Boon"),
        majorCurse("Major Curse"),
        moderateCurse("Moderate Curse"),
        minorCurse("Minor Curse"),
        melee, // avatar
        note, // avatar
        pantheon,
        preciousMetal, //avatar
        preface, // avatar
        range, // avatar
        ranged, // avatar
        rangedIncrement, // avatar
        reload, // avatar
        shield, // avatar
        speed, // avatar
        spells, // cleric
        traitNote; // avatar

        final String nodeName;

        Pf2eDeity() {
            this.nodeName = this.name();
        }

        Pf2eDeity(String nodeName) {
            this.nodeName = nodeName;
        }

        public String nodeName() {
            return nodeName;
        }
    }
}
