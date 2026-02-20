package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.pluralize;
import static dev.ebullient.convert.StringUtil.toOrdinal;
import static dev.ebullient.convert.StringUtil.uppercaseFirst;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.SpellEntry.SpellReference;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteSpell extends Json2QuteCommon {
    final String decoratedName;

    private static final Map<String, String> AREA_TAG_LABELS = Map.ofEntries(
            Map.entry("C", "Cube"),
            Map.entry("H", "Hemisphere"),
            Map.entry("L", "Line"),
            Map.entry("MT", "Multiple Targets"),
            Map.entry("N", "Cone"),
            Map.entry("Q", "Square"),
            Map.entry("R", "Circle"),
            Map.entry("S", "Sphere"),
            Map.entry("ST", "Single Target"),
            Map.entry("W", "Wall"),
            Map.entry("Y", "Cylinder"));

    private static final Map<String, String> MISC_TAG_LABELS = Map.ofEntries(
            Map.entry("AAD", "Additional Attack Damage"),
            Map.entry("ADV", "Grants Advantage"),
            Map.entry("DFT", "Difficult Terrain"),
            Map.entry("FMV", "Forced Movement"),
            Map.entry("HL", "Healing"),
            Map.entry("LGT", "Creates Light"),
            Map.entry("LGTS", "Creates Sunlight"),
            Map.entry("MAC", "Modifies AC"),
            Map.entry("OBJ", "Affects Objects"),
            Map.entry("OBS", "Obscures Vision"),
            Map.entry("PRM", "Permanent Effects"),
            Map.entry("PIR", "Permanent If Repeated"),
            Map.entry("PS", "Plane Shifting"),
            Map.entry("RO", "Rollable Effects"),
            Map.entry("SCL", "Scaling Effects"),
            Map.entry("SCT", "Scaling Targets"),
            Map.entry("SGT", "Requires Sight"),
            Map.entry("SMN", "Summons Creature"),
            Map.entry("THP", "Grants Temporary Hit Points"),
            Map.entry("TP", "Teleportation"),
            Map.entry("UBA", "Uses Bonus Action"));

    Json2QuteSpell(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        decoratedName = linkifier().decoratedName(type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        SpellEntry spellEntry = index().getSpellIndex().getSpellEntry(getSources().getKey());

        Tags tags = new Tags(getSources());

        tags.add("spell", "school", spellEntry.school.name());
        tags.add("spell", "level", JsonSource.spellLevelToText(spellEntry.level));
        if (spellEntry.ritual) {
            tags.add("spell", "ritual");
        }

        // 🔧 Spell: spell|fireball|phb,
        //    references: {subclass|destruction domain|cleric|phb|vss=subclass|destruction domain|cleric|phb|vss;c:5;s:null;null, ...}
        //    expanded: {subclass|the fiend|warlock|phb|phb=subclass|the fiend|warlock|phb|phb;c:null;s:3;null, ...}
        Set<String> referenceLinks = new HashSet<>();
        Set<SpellReference> allRefs = new TreeSet<>(Comparator.comparing(x -> x.refererKey));
        allRefs.addAll(spellEntry.references.values());
        allRefs.addAll(spellEntry.expandedList.values());

        for (var r : allRefs) {
            tags.addRaw(r.tagifyReference());
            referenceLinks.add(r.linkifyReference());
        }

        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, "##");
        if (SpellFields.entriesHigherLevel.existsIn(rootNode)) {
            maybeAddBlankLine(text);
            appendToText(text, SpellFields.entriesHigherLevel.getFrom(rootNode),
                    textContains(text, "## ") ? "##" : null);
        }
        return new QuteSpell(sources,
                decoratedName,
                getSourceText(sources),
                JsonSource.spellLevelToText(spellEntry.level),
                spellEntry.school.name(),
                spellEntry.ritual,
                spellCastingTime(),
                spellRange(),
                spellComponents(),
                spellDuration(),
                spellAbilityChecks(),
                spellAffectsCreatureTypes(),
                spellAreaTags(),
                spellConditionImmune(),
                spellConditionInflict(),
                spellDamageImmune(),
                spellDamageInflict(),
                spellDamageResist(),
                spellDamageVulnerable(),
                spellMiscTags(),
                spellSavingThrows(),
                spellScalingLevelDice(),
                spellAttacks(),
                spellHigherLevelEntries(),
                referenceLinks,
                getFluffImages(Tools5eIndexType.spellFluff),
                String.join("\n", text),
                tags);
    }

    SpellSchool getSchool() {
        String code = SpellFields.school.getTextOrEmpty(rootNode);
        return index().findSpellSchool(code, getSources());
    }

    boolean spellIsRitual() {
        boolean ritual = false;
        JsonNode meta = SpellFields.meta.getFrom(rootNode);
        if (meta != null) {
            ritual = SpellFields.ritual.booleanOrDefault(meta, false);
        }
        return ritual;
    }

    String spellComponents() {
        JsonNode components = SpellFields.components.getFrom(rootNode);
        if (components == null) {
            return "";
        }

        List<String> list = new ArrayList<>();
        for (Entry<String, JsonNode> f : iterableFields(components)) {
            switch (f.getKey().toLowerCase()) {
                case "v" -> list.add("V");
                case "s" -> list.add("S");
                case "m" -> {
                    list.add(materialComponents(f.getValue()));
                }
                case "r" -> list.add("R"); // Royalty. Acquisitions Incorporated
            }
        }
        return String.join(", ", list);
    }

    String materialComponents(JsonNode source) {
        return "M (%s)".formatted(
                source.isObject()
                        ? SpellFields.text.replaceTextFrom(source, this)
                        : replaceText(source.asText()));
    }

    String spellDuration() {
        StringBuilder result = new StringBuilder();
        JsonNode durations = SpellFields.duration.ensureArrayIn(rootNode);
        if (durations.size() > 0) {
            addDuration(durations.get(0), result);
        }
        if (durations.size() > 1) {
            JsonNode ends = durations.get(1);
            result.append(", ");
            String type = SpellFields.type.getTextOrEmpty(ends);
            if ("timed".equals(type)) {
                result.append("up to ");
            }
            addDuration(ends, result);
        }
        return result.toString();
    }

    void addDuration(JsonNode element, StringBuilder result) {
        String type = SpellFields.type.getTextOrEmpty(element);
        switch (type) {
            case "instant" -> result.append("Instantaneous");
            case "permanent" -> {
                result.append("Until dispelled");
                if (element.withArray("ends").size() > 1) {
                    result.append(" or triggered");
                }
            }
            case "special" -> result.append("Special");
            case "timed" -> {
                if (booleanOrDefault(element, "concentration", false)) {
                    result.append("Concentration, up to ");
                }
                JsonNode duration = element.get("duration");
                String amount = SpellFields.amount.getTextOrEmpty(duration);
                result.append(amount)
                        .append(" ")
                        .append(pluralize(
                                SpellFields.type.getTextOrEmpty(duration),
                                Integer.valueOf(amount)));
            }
            default -> tui().errorf("What is this? %s", element.toPrettyString());
        }
    }

    String spellRange() {
        StringBuilder result = new StringBuilder();
        JsonNode range = SpellFields.range.getFrom(rootNode);
        if (range != null) {
            String type = SpellFields.type.getTextOrEmpty(range);
            JsonNode distance = SpellFields.distance.getFrom(range);
            String distanceType = SpellFields.type.getTextOrEmpty(distance);
            String amount = SpellFields.amount.getTextOrEmpty(distance);

            switch (type) {
                case "cube", "cone", "emanation", "hemisphere", "line", "radius", "sphere" -> {// Self (xx-foot yy)
                    result.append("Self (")
                            .append(amount)
                            .append("-")
                            .append(pluralize(distanceType, 1))
                            .append(" ")
                            .append(uppercaseFirst(type))
                            .append(")");
                }
                case "point" -> {
                    switch (distanceType) {
                        case "self", "sight", "touch", "unlimited" ->
                            result.append(uppercaseFirst(distanceType));
                        default -> result.append(amount)
                                .append(" ")
                                .append(distanceType);
                    }
                }
                case "special" -> result.append("Special");
            }
        }
        return result.toString();
    }

    String spellCastingTime() {
        StringBuilder result = new StringBuilder();
        JsonNode time = rootNode.withArray("time").get(0);
        String number = SpellFields.number.getTextOrEmpty(time);
        String unit = SpellFields.unit.getTextOrEmpty(time);
        result.append(number).append(" ");
        switch (unit) {
            case "action", "reaction" ->
                result.append(uppercaseFirst(unit));
            case "bonus" ->
                result.append(uppercaseFirst(unit))
                        .append(" Action");
            default ->
                result.append(unit);
        }
        return pluralize(result.toString(), Integer.valueOf(number));
    }

    String spellAreaTags() {
        return joinList(SpellFields.areaTags.getListOfStrings(rootNode, tui()).stream()
                .map(tag -> AREA_TAG_LABELS.getOrDefault(tag, uppercaseFirst(tag)))
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellDamageInflict() {
        return joinList(SpellFields.damageInflict.getListOfStrings(rootNode, tui()).stream()
                .map(this::formatKeyword)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellSavingThrows() {
        return joinList(SpellFields.savingThrow.getListOfStrings(rootNode, tui()).stream()
                .map(this::formatKeyword)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellConditionInflict() {
        return joinList(SpellFields.conditionInflict.getListOfStrings(rootNode, tui()).stream()
                .map(this::formatKeyword)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellAbilityChecks() {
        return joinList(SpellFields.abilityCheck.getListOfStrings(rootNode, tui()).stream()
                .map(this::formatAbility)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellMiscTags() {
        return joinList(SpellFields.miscTags.getListOfStrings(rootNode, tui()).stream()
                .map(Json2QuteSpell::formatMiscTag)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellAffectsCreatureTypes() {
        return joinList(SpellFields.affectsCreatureType.getListOfStrings(rootNode, tui()).stream()
                .map(this::formatKeyword)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellConditionImmune() {
        return joinList(SpellFields.conditionImmune.getListOfStrings(rootNode, tui()).stream()
                .map(this::formatKeyword)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellDamageImmune() {
        return joinList(SpellFields.damageImmune.getListOfStrings(rootNode, tui()).stream()
                .map(this::formatKeyword)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellDamageResist() {
        return joinList(SpellFields.damageResist.getListOfStrings(rootNode, tui()).stream()
                .map(this::formatKeyword)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellDamageVulnerable() {
        return joinList(SpellFields.damageVulnerable.getListOfStrings(rootNode, tui()).stream()
                .map(this::formatKeyword)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellAttacks() {
        return joinList(SpellFields.spellAttack.getListOfStrings(rootNode, tui()).stream()
                .map(Json2QuteSpell::formatSpellAttack)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    String spellScalingLevelDice() {
        JsonNode node = SpellFields.scalingLevelDice.getFrom(rootNode);
        if (node == null || node.isNull()) {
            return null;
        }

        List<JsonNode> blocks = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode n : node) {
                blocks.add(n);
            }
        } else if (node.isObject()) {
            blocks.add(node);
        } else {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (JsonNode scalingNode : blocks) {
            JsonNode scaling = SpellFields.scaling.getFrom(scalingNode);
            if (scaling == null || scaling.isEmpty()) {
                continue;
            }

            List<Entry<String, JsonNode>> entries = new ArrayList<>();
            for (Entry<String, JsonNode> e : iterableFields(scaling)) {
                entries.add(e);
            }
            entries.sort(Comparator.comparingInt(e -> Integer.parseInt(e.getKey())));

            List<String> pieces = new ArrayList<>();
            for (Entry<String, JsonNode> entry : entries) {
                String level = entry.getKey();
                String dice = entry.getValue().asText();
                if (dice == null || dice.isBlank()) {
                    continue;
                }
                pieces.add("%s level: %s".formatted(toOrdinal(level), dice));
            }

            if (pieces.isEmpty()) {
                continue;
            }

            String label = SpellFields.label.getTextOrEmpty(scalingNode);
            if (label == null || label.isBlank()) {
                result.add(String.join("; ", pieces));
            } else {
                result.add("%s: %s".formatted(uppercaseFirst(label.trim()), String.join("; ", pieces)));
            }
        }

        return result.isEmpty() ? null : String.join(", ", result);
    }

    String spellHigherLevelEntries() {
        if (!SpellFields.entriesHigherLevel.existsIn(rootNode)) {
            return null;
        }
        String value = SpellFields.entriesHigherLevel.transformTextFrom(rootNode, "\n", this, null).strip();
        return value.isBlank() ? null : value;
    }

    private String formatKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return uppercaseFirst(value.trim());
    }

    private String formatAbility(String ability) {
        if (ability == null || ability.isBlank()) {
            return null;
        }
        return uppercaseFirst(ability.trim());
    }

    private static String formatMiscTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        return MISC_TAG_LABELS.getOrDefault(tag, uppercaseFirst(tag.trim()));
    }

    private static String formatSpellAttack(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return switch (code.trim()) {
            case "M" -> "Melee Spell Attack";
            case "R" -> "Ranged Spell Attack";
            default -> null;
        };
    }

    enum SpellFields implements JsonNodeReader {
        abilityCheck,
        affectsCreatureType,
        amount,
        areaTags,
        className,
        classSource,
        classes,
        components,
        conditionImmune,
        conditionInflict,
        damageImmune,
        damageInflict,
        damageResist,
        damageVulnerable,
        distance,
        duration,
        entriesHigherLevel,
        label,
        level,
        meta,
        miscTags,
        number,
        range,
        ritual,
        savingThrow,
        scaling,
        scalingLevelDice,
        school,
        self,
        sight,
        special,
        spellAttack,
        text,
        touch,
        type,
        unit,
        unlimited,
    }

    private static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(", ", values);
    }
}
