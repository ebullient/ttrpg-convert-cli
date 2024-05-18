package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joinConjunct;
import static dev.ebullient.convert.StringUtil.joiningConjunct;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteDeity;
import dev.ebullient.convert.tools.pf2e.qute.QuteInlineAttack;
import dev.ebullient.convert.tools.pf2e.qute.QuteInlineAttack.AttackRangeType;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteDeity extends Json2QuteBase {

    public Json2QuteDeity(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.background, rootNode);
    }

    @Override
    protected QuteDeity buildQuteResource() {
        List<String> text = new ArrayList<>();
        Tags tags = new Tags(sources);

        Pf2eDeity.domains.getListOfStrings(rootNode, tui()).forEach(d -> tags.add("domain", d, "deity"));
        Pf2eDeity.alternateDomains.getListOfStrings(rootNode, tui()).forEach(d -> tags.add("domain", d, "deity"));

        String category = Pf2eDeity.category.getTextOrDefault(rootNode, "Deity");
        tags.add("deity", category);

        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        JsonNode alignNode = Pf2eDeity.alignment.getFrom(rootNode);
        String alignment = join(", ", getAlignments(Pf2eDeity.alignment.getFrom(alignNode)));
        String followerAlignment = join(", ", getAlignments(Pf2eDeity.followerAlignment.getFrom(alignNode)));

        return new QuteDeity(sources, text, tags,
                Field.alias.replaceTextFromList(rootNode, this),
                category,
                join(", ", Pf2eDeity.pantheon.linkifyListFrom(rootNode, Pf2eIndexType.deity, this)),
                alignment, followerAlignment,
                Pf2eDeity.areasOfConcern.transformTextFrom(rootNode, ", ", this),
                commandmentToString(Pf2eDeity.edict.replaceTextFromList(rootNode, this)),
                commandmentToString(Pf2eDeity.anathema.replaceTextFromList(rootNode, this)),
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
        intercession.source = Pf2eSources.createEmbeddedSource(node).getSourceText();
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

    QuteDeity.QuteDivineAvatar buildAvatar(Tags tags) {
        JsonNode avatarNode = Pf2eDeity.avatar.getFrom(rootNode);
        if (avatarNode == null) {
            return null;
        }

        QuteDeity.QuteDivineAvatar avatar = new QuteDeity.QuteDivineAvatar();
        avatar.preface = replaceText(Pf2eDeity.preface.getTextOrEmpty(avatarNode));
        avatar.name = linkify(Pf2eIndexType.spell, "avatar||Avatar") + " of " + sources.getName();

        avatar.speed = Pf2eTypeReader.Pf2eSpeed.getSpeed(Pf2eDeity.speed.getFrom(avatarNode), this);
        if (Pf2eDeity.airWalk.booleanOrDefault(avatarNode, false)) {
            avatar.speed.addAbility(linkify(Pf2eIndexType.spell, "air walk"));
        }
        String immunities = joinConjunct(" and ",
                Pf2eDeity.immune.linkifyListFrom(avatarNode, Pf2eIndexType.condition, this));
        if (!immunities.isEmpty()) {
            avatar.speed.addAbility("immune to " + immunities);
        }
        if (Pf2eDeity.ignoreTerrain.booleanOrDefault(avatarNode, false)) {
            avatar.speed.addAbility(replaceText(
                    "ignore {@quickref difficult terrain||3|terrain} and {@quickref greater difficult terrain||3|terrain}"));
        }

        avatar.shield = Pf2eDeity.shield.getIntFrom(avatarNode)
                .map("shield (%d Hardness, can't be damaged)"::formatted).orElse(null);

        avatar.attacks = Stream.concat(
                Pf2eDeity.melee.streamFrom(avatarNode).map(n -> Map.entry(n, AttackRangeType.MELEE)),
                Pf2eDeity.ranged.streamFrom(avatarNode).map(n -> Map.entry(n, AttackRangeType.RANGED)))
                .map(e -> buildAvatarAttack(e.getKey(), tags, e.getValue()))
                .toList();
        avatar.ability = Pf2eDeity.ability.streamFrom(avatarNode)
                .map(this::buildAvatarAbility)
                .collect(Collectors.toList());

        return avatar;
    }

    private NamedText buildAvatarAbility(JsonNode abilityNode) {
        return new NamedText(
                SourceField.name.getTextOrEmpty(abilityNode),
                SourceField.entries.transformTextFrom(abilityNode, "; ", this));
    }

    private QuteInlineAttack buildAvatarAttack(JsonNode actionNode, Tags tags, AttackRangeType rangeType) {
        Collection<String> traits = collectTraitsFrom(actionNode, tags);
        traits.addAll(Pf2eDeity.preciousMetal.getListOfStrings(actionNode, tui()));
        Pf2eDeity.traitNote.getTextFrom(actionNode).ifPresent(traits::add);

        return new QuteInlineAttack(
                Pf2eAttack.name.getTextOrDefault(actionNode, "attack"),
                Pf2eActivity.single.toQuteActivity(this, null),
                rangeType,
                Pf2eWeaponData.getDamageString(actionNode, this),
                Stream.of(Pf2eWeaponData.damageType, Pf2eWeaponData.damageType2)
                        .map(field -> field.getTextOrEmpty(actionNode))
                        .filter(StringUtil::isPresent)
                        .toList(),
                traits,
                Pf2eDeity.note.replaceTextFrom(actionNode, this),
                this);
    }

    String commandmentToString(List<String> edictOrAnathema) {
        return String.join(
                edictOrAnathema.stream().anyMatch(x -> x.contains(",")) ? "; " : ", ",
                edictOrAnathema);
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
            return abilities.stream().map(StringUtil::toTitleCase).collect(joiningConjunct(" or "));
        }

        public String buildSkillString(JsonSource convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            return skills.stream()
                    .map(s -> convert.linkify(Pf2eIndexType.spell, toTitleCase(s)))
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

    enum Pf2eDeity implements JsonNodeReader {
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
