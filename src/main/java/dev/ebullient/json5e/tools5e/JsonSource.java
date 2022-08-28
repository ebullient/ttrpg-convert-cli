package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.json5e.io.Json5eTui;

public interface JsonSource {
    static final Pattern backgroundPattern = Pattern.compile("\\{@(background) ([^}]+)}");
    static final Pattern classPattern1 = Pattern.compile("\\{@(class) ([^}]+)}");
    static final Pattern featPattern = Pattern.compile("\\{@(feat) ([^}]+)}");
    static final Pattern itemPattern = Pattern.compile("\\{@(item) ([^}]+)}");
    static final Pattern racePattern = Pattern.compile("\\{@(race) ([^}]+)}");
    static final Pattern spellPattern = Pattern.compile("\\{@(spell) ([^}]+)}");
    static final Pattern creaturePattern = Pattern.compile("\\{@(creature) ([^}]+)}");

    static final Pattern dicePattern = Pattern.compile("\\{@(dice|damage) ([^|}]+)[^}]*\\}");
    static final Pattern chancePattern = Pattern.compile("\\{@chance ([^}]+)\\}");
    static final Pattern notePattern = Pattern.compile("\\{@note (\\*|Note:)?\\s?([^}]+)}");

    static final Pattern condPattern = Pattern.compile("\\{@condition ([^|}]+)\\|?[^}]*}");
    static final Pattern skillPattern = Pattern.compile("\\{@skill ([^}]+)}");
    static final Pattern sensePattern = Pattern.compile("\\{@sense ([^}]+)}");

    final static int CR_UNKNOWN = 100001;
    final static int CR_CUSTOM = 100000;

    JsonIndex index();

    CompendiumSources getSources();

    default Json5eTui tui() {
        return index().tui;
    }

    default String slugify(String s) {
        return index().tui.slugify(s);
    }

    static boolean isReprinted(JsonIndex index, String finalKey, JsonNode jsonSource) {
        if (jsonSource.has("reprintedAs")) {
            for (Iterator<JsonNode> i = jsonSource.withArray("reprintedAs").elements(); i.hasNext();) {
                String[] ra = i.next().asText().split("\\|");
                if (index.sourceIncluded(ra[1])) {
                    index.tui().verbosef("🔹 Skipping %s; Reprinted as %s in %s%n", finalKey, ra[0], ra[1]);
                    return true; // the reprint will be used instead (stop parsing this one)
                }
            }
        }
        if (jsonSource.has("isReprinted")) {
            index.tui().verbosef("🔹 Skipping %s (has been reprinted)%n", finalKey);
            return true; // the reprint will be used instead of this one.
        }
        return false;
    }

    default boolean isReprinted(JsonNode jsonSource) {
        return isReprinted(index(), getSources().key, jsonSource);
    }

    default Stream<JsonNode> streamOf(ArrayNode array) {
        return StreamSupport.stream(array.spliterator(), false);
    }

    default List<String> findAndReplace(JsonNode jsonSource, String field) {
        return findAndReplace(jsonSource, field, s -> s);
    }

    default List<String> findAndReplace(JsonNode jsonSource, String field, Function<String, String> replacement) {
        JsonNode node = jsonSource.get(field);
        if (node == null || node.isNull()) {
            return List.of();
        } else if (node.isTextual()) {
            return List.of(replaceText(node.asText()));
        } else if (node.isObject()) {
            throw new IllegalArgumentException(
                    String.format("Unexpected object node (expected array): %s (referenced from %s)", node, getSources()));
        }
        return streamOf(jsonSource.withArray(field))
                .map(x -> replaceText(x.asText()).trim())
                .map(x -> replacement.apply(x))
                .filter(x -> !x.isBlank())
                .collect(Collectors.toList());
    }

    default String joinAndReplace(JsonNode jsonSource, String field) {
        JsonNode node = jsonSource.get(field);
        if (node == null || node.isNull()) {
            return "";
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isObject()) {
            throw new IllegalArgumentException(
                    String.format("Unexpected object node (expected array): %s (referenced from %s)", node, getSources()));
        }
        return joinAndReplace((ArrayNode) node);
    }

    default String joinAndReplace(ArrayNode array) {
        List<String> list = new ArrayList<>();
        array.forEach(v -> list.add(replaceText(v.asText())));
        return String.join(", ", list);
    }

    default String getTextOrEmpty(JsonNode x, String field) {
        if (x.has(field)) {
            return x.get(field).asText();
        }
        return "";
    }

    default String getTextOrDefault(JsonNode x, String field, String value) {
        if (x.has(field)) {
            return x.get(field).asText();
        }
        return value;
    }

    default String getOrEmptyIfEqual(JsonNode x, String field, String expected) {
        if (x.has(field)) {
            String value = x.get(field).asText().trim();
            return value.equalsIgnoreCase(expected) ? "" : value;
        }
        return "";
    }

    default boolean booleanOrDefault(JsonNode source, String key, boolean value) {
        JsonNode result = source.get(key);
        return result == null ? value : result.asBoolean(value);
    }

    default int intOrDefault(JsonNode source, String key, int value) {
        JsonNode result = source.get(key);
        return result == null ? value : result.asInt();
    }

    default JsonNode handleCopy(IndexType type, JsonNode jsonSource) {
        if (type == IndexType.race) {
            return copyAndMergeRace(jsonSource);
        }
        if (type == IndexType.classtype) {
            return copyAndMergeClass(jsonSource);
        }
        JsonNode _copy = jsonSource.get("_copy");
        if (_copy != null) {
            // Fix infinite loop: self-referencing copy
            if (type == IndexType.monsterfluff
                    && jsonSource.get("name").asText().equalsIgnoreCase("Derro Savant")
                    && _copy.get("name").asText().equalsIgnoreCase("Derro Savant")) {
                ((ObjectNode) _copy).set("name", new TextNode("Derro"));
            }
            JsonNode baseNode = index().getNode(type, _copy);
            if (baseNode != null) {
                // is the copy a copy?
                baseNode = handleCopy(type, baseNode);
                try {
                    String originKey = index().getKey(type, jsonSource);
                    jsonSource = mergeNodes(originKey, baseNode, jsonSource);
                    index().replace(originKey, jsonSource); // REPLACE IN THE INDEX (xml, then markdown... won't redo)
                } catch (IllegalStateException | StackOverflowError e) {
                    throw new IllegalStateException("Unable to resolve copy " + _copy.toPrettyString());
                }
            }
        }
        return jsonSource;
    }

    default JsonNode copyAndMergeRace(JsonNode jsonNode) {
        if (jsonNode.has("raceName") || jsonNode.has("_copy")) {
            CompendiumSources sources = index().constructSources(IndexType.race, jsonNode);
            jsonNode = cloneOrCopy(sources.getKey(),
                    jsonNode, IndexType.race,
                    getTextOrDefault(jsonNode, "raceName", null),
                    getTextOrDefault(jsonNode, "raceSource", null));
        }
        return jsonNode;
    }

    default JsonNode copyAndMergeClass(JsonNode jsonSource) {
        if (jsonSource.has("className") || jsonSource.has("_copy")) {
            CompendiumSources sources = index().constructSources(IndexType.classtype, jsonSource);
            jsonSource = index().cloneOrCopy(sources.getKey(),
                    jsonSource, IndexType.classtype,
                    getTextOrDefault(jsonSource, "className", null),
                    getTextOrDefault(jsonSource, "classSource", null));
        }
        return jsonSource;
    }

    default JsonNode cloneOrCopy(String originKey, JsonNode value, IndexType parentType, String parentName,
            String parentSource) {
        JsonNode parentNode = parentName == null ? null : index().getNode(parentType, parentName, parentSource);
        JsonNode copyNode = index().getNode(parentType, value.get("_copy"));
        if (parentNode == null && copyNode == null) {
            tui().errorf("both parent and requested copy are null? (from %s with _copy=%s)", originKey,
                    value.get("_copy").toPrettyString());
        } else if (parentNode == null) {
            value = mergeNodes(originKey, copyNode, value);
        } else if (copyNode == null) {
            value = mergeNodes(originKey, parentNode, value);
        } else {
            // base type first
            JsonNode mergeNode = mergeNodes(originKey, parentNode, copyNode);
            value = mergeNodes(originKey, mergeNode, value);
            tui().errorf("Check my work: %s is a copy of %s based on %s", originKey,
                    getTextOrEmpty(copyNode, "name"), getTextOrEmpty(parentNode, "name"));
        }
        index().replace(originKey, value); // REPLACE IN THE INDEX (xml, then markdown... won't redo)
        return value;
    }

    default JsonNode mergeNodes(String originKey, JsonNode baseNode, JsonNode overlayNode) {
        ObjectNode target = (ObjectNode) copyNode(baseNode);
        target.put("merged", true);
        target.remove("srd");
        target.remove("basicRules");
        target.remove("_versions");
        target.remove("_copy");

        JsonNode _copy = overlayNode.get("_copy");
        JsonNode _mod = _copy == null ? null : _copy.get("_mod");
        JsonNode _preserve = _copy == null ? null : _copy.get("_preserve");
        JsonNode overwrite = _copy == null ? null : _copy.get("overwrite");

        if (_preserve == null || !_preserve.has("reprintedAs")) {
            target.remove("reprintedAs");
        }

        for (Iterator<String> it = overlayNode.fieldNames(); it.hasNext();) {
            String f = it.next();
            JsonNode overlayField = overlayNode.get(f);
            switch (f) {
                case "_copy":
                case "_mod":
                case "_versions":
                    // skip -- do not copy
                    break;
                case "ability":
                    if (overlayNode.has("raceName")) {
                        target.set("ability", copyNode(overlayField));
                    } else if ((overwrite != null && overwrite.has("ability"))
                            || !baseNode.has("ability")) {
                        target.set("ability", copyNode(overlayField));
                    } else {
                        ArrayNode cpyAbility = target.withArray("ability");
                        if (cpyAbility.size() == 0) {
                            target.set("ability", copyNode(overlayField));
                            break;
                        }
                        if (cpyAbility.size() != overlayField.size()) {
                            tui().errorf("Copy/Merge: Ability array lengths did not match (from %s):%nBASE:%n%s%nCOPY:%n%s",
                                    originKey, cpyAbility.toPrettyString(), overlayField.toPrettyString());
                            continue;
                        }
                        for (int i = 0; i < overlayField.size(); i++) {
                            mergeFields(originKey, overlayField.get(i), (ObjectNode) cpyAbility.get(i));
                        }
                    }
                    break;
                case "skillProficiencies":
                case "weaponProficiencies":
                case "armorProficiencies":
                case "languageProficiencies": {
                    if (overlayNode.has("raceName")) {
                        target.set(f, copyNode(overlayField));
                    } else {
                        ArrayNode cpyArray = target.withArray(f);
                        if ((overwrite != null && overwrite.has(f)) || cpyArray.isEmpty()) {
                            target.set(f, copyNode(overlayField));
                        } else {
                            // usually size of one... so just append fields
                            for (int i = 0; i < overlayField.size(); i++) {
                                mergeFields(originKey, overlayField.get(i), (ObjectNode) cpyArray.get(i));
                            }
                        }
                    }
                    break;
                }
                case "entries":
                    if (_mod == null) {
                        ArrayNode targetEntries = target.withArray("entries");
                        appendToArray(targetEntries, overlayField);
                    }
                    break;
                default:
                    if (overlayField == null) {
                        target.remove(f);
                    } else if (_preserve == null || !_preserve.has(f)) {
                        target.replace(f, copyNode(overlayField));
                    } else {
                        tui().debugf("Copy/Merge: Skip field %s (from %s)", f, originKey);
                    }
                    break;
            }
        }

        if (_mod != null) {
            _mod.fields().forEachRemaining(field -> {
                if (field.getKey().equals("*")) {
                    List.of("action", "bonus", "reaction", "trait", "legendary", "mythic", "variant", "spellcasting",
                            "legendaryHeader").forEach(x -> {
                                if (target.has(x)) {
                                    handleModifications(originKey, x, field.getValue(), target);
                                }
                            });
                } else if (field.getKey().equals("_")) {
                    handleModifications(originKey, null, field.getValue(), target);
                } else {
                    handleModifications(originKey, field.getKey(), field.getValue(), target);
                }
            });
        }

        return target;
    }

    default JsonNode copyNode(JsonNode sourceNode) {
        try {
            return Json5eTui.MAPPER.readTree(sourceNode.toString());
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default ArrayNode sortArrayNode(ArrayNode array) {
        if (array == null || array.size() <= 1) {
            return array;
        }
        Set<JsonNode> elements = new TreeSet<>(Comparator.comparing(a -> a.asText().toLowerCase()));
        array.forEach(elements::add);
        ArrayNode sorted = Json5eTui.MAPPER.createArrayNode();
        elements.forEach(sorted::add);
        return sorted;
    }

    default JsonNode copyReplaceNode(JsonNode sourceNode, Pattern replace, String with) {
        try {
            String modified = replace.matcher(sourceNode.toString()).replaceAll(with);
            return Json5eTui.MAPPER.readTree(modified);
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default void mergeFields(String originKey, JsonNode sourceNode, ObjectNode targetNode) {
        sourceNode.fields().forEachRemaining(f -> {
            targetNode.set(f.getKey(), copyNode(f.getValue()));
        });
    }

    default boolean sameValue(JsonNode source, JsonNode target) {
        return source.toString().equals(target.toString());
    }

    default void handleModifications(String originKey, String prop, JsonNode modInfo, JsonNode target) {
        if (modInfo.isTextual()) {
            if ("remove".equals(modInfo.asText())) {
                ((ObjectNode) target).remove(prop);
            } else {
                tui().errorf("Unknown modification mode: %s (from %s)", modInfo.toPrettyString(), originKey);
            }
        } else if (modInfo.isArray()) {
            modInfo.forEach(modItem -> doMod(originKey, modItem, prop, target));
        } else {
            doMod(originKey, modInfo, prop, target);
        }
    }

    default void doMod(String originKey, JsonNode modItem, String modFieldName, JsonNode target) {
        switch (modItem.get("mode").asText()) {
            case "prependArr":
            case "appendArr":
            case "insertArr":
            case "removeArr":
            case "replaceArr":
            case "replaceOrAppendArr":
            case "appendIfNotExistsArr":
                doModArray(originKey, modItem, modFieldName, target);
                break;
            case "replaceTxt":
                doReplaceText(originKey, modItem, modFieldName, target);
                break;
            case "addSkills":
                doAddSkills(originKey, modItem, modFieldName, target);
                break;
            case "addSpells":
                doAddSpells(originKey, modItem, modFieldName, target);
                break;
            case "removeSpells":
                doRemoveSpells(originKey, modItem, modFieldName, target);
                break;
            case "replaceSpells":
                doReplaceSpells(originKey, modItem, modFieldName, target);
                break;
            default:
                tui().errorf("Unknown modification mode: %s (from %s)", modItem.toPrettyString(), originKey);
                break;
        }
    }

    default void doRemoveSpells(String originKey, JsonNode modItem, String modFieldName, JsonNode target) {
        if (!target.has("spellcasting")) {
            throw new IllegalStateException("Can't remove spells from a monster without spellcasting: " + originKey);
        }

        JsonNode arrayNode = target.get("spellcasting");
        arrayNode.forEach(targetSpellcasting -> {
            List.of("rest", "daily", "weekly", "yearly").forEach(prop -> {
                if (!modItem.has(prop)) {
                    return;
                }
                ObjectNode targetGroup = targetSpellcasting.with(prop);
                for (int i = 1; i <= 9; ++i) {
                    String key = i + "";
                    if (modItem.get(prop).has(key)) {
                        ArrayNode tgtArray = targetGroup.withArray(key);
                        modItem.withArray(key).forEach(spell -> {
                            int index = findIndexByName(originKey, tgtArray, spell.asText());
                            if (index >= 0) {
                                tgtArray.remove(index);
                            }
                        });
                        targetGroup.set(key, tgtArray);
                    }

                    String e = i + "e";
                    if (modItem.get(prop).has(e)) {
                        ArrayNode tgtArray = targetGroup.withArray(e);
                        modItem.withArray(e).forEach(spell -> {
                            int index = findIndexByName(originKey, tgtArray, spell.asText());
                            if (index >= 0) {
                                tgtArray.remove(index);
                            }
                        });
                        targetGroup.set(key, tgtArray);
                    }
                }
            });
        });
    }

    default void doReplaceSpells(String originKey, JsonNode modItem, String modFieldName, JsonNode target) {
        if (!target.has("spellcasting")) {
            throw new IllegalStateException("Can't replace spells for a monster without spellcasting: " + originKey);
        }

        JsonNode arrayNode = target.get("spellcasting");
        arrayNode.forEach(targetSpellcasting -> {
            if (modItem.has("spells")) {
                JsonNode spells = modItem.get("spells");
                ObjectNode targetSpells = targetSpellcasting.with("spells");
                spells.fields().forEachRemaining(ss -> {
                    if (targetSpells.has(ss.getKey())) {
                        JsonNode levelMetas = ss.getValue();
                        ObjectNode targetLevel = targetSpells.with(ss.getKey());
                        ArrayNode targetLevelSpells = targetLevel.withArray("spells");
                        levelMetas.forEach(x -> replaceArray(originKey, x, targetLevelSpells,
                                x.get("replace"), x.get("with")));
                        targetSpells.set(ss.getKey(), sortArrayNode(targetLevelSpells));
                    }
                });
            }

            List.of("rest", "daily", "weekly", "yearly").forEach(prop -> {
                if (!modItem.has(prop)) {
                    return;
                }
                ObjectNode targetGroup = targetSpellcasting.with(prop);
                for (int i = 1; i <= 9; ++i) {
                    String key = i + "";
                    if (modItem.get(prop).has(key)) {
                        modItem.get(prop).get(key).forEach(
                                sp -> replaceArray(originKey, sp, targetGroup.withArray(key),
                                        sp.get("replace"), sp.get("with")));
                        targetGroup.set(key, sortArrayNode(targetGroup.withArray(key)));
                    }

                    String e = i + "e";
                    if (modItem.get(prop).has(e)) {
                        modItem.get(prop).get(e).forEach(
                                sp -> replaceArray(originKey, sp, targetGroup.withArray(e),
                                        sp.get("replace"), sp.get("with")));
                        targetGroup.set(e, sortArrayNode(targetGroup.withArray(e)));
                    }
                }
            });
        });
    }

    default void doAddSpells(String originKey, JsonNode modItem, String modFieldName, JsonNode target) {
        if (!target.has("spellcasting")) {
            throw new IllegalStateException("Can't add spells to a monster without spellcasting: " + originKey);
        }
        tui().debugf("Add spells %s", originKey);
        ObjectNode targetSpellcasting = (ObjectNode) target.get("spellcasting").get(0);
        if (modItem.has("spells")) {
            JsonNode spells = modItem.get("spells");
            ObjectNode targetSpells = targetSpellcasting.with("spells");
            spells.fields().forEachRemaining(s -> {
                if (!targetSpells.has(s.getKey())) {
                    targetSpells.set(s.getKey(), sortArrayNode((ArrayNode) s.getValue()));
                } else {
                    JsonNode spellsNew = spells.get(s.getKey());
                    ObjectNode spellsTgt = targetSpells.with(s.getKey());
                    spellsNew.fields().forEachRemaining(ss -> {
                        if (!spellsTgt.has(ss.getKey())) {
                            spellsTgt.set(ss.getKey(), sortArrayNode((ArrayNode) ss.getValue()));
                        } else if (spellsTgt.get(ss.getKey()).isArray()) {
                            ArrayNode spellsArray = spellsTgt.withArray(ss.getKey());
                            appendToArray(spellsArray, copyNode(ss.getValue()));
                            targetSpells.set(s.getKey(), sortArrayNode(spellsArray));
                        } else if (spellsTgt.get(ss.getKey()).isObject()) {
                            throw new IllegalArgumentException(
                                    String.format("Object %s is not an array (referenced from %s)", ss.getKey(), originKey));
                        }
                    });
                }
            });
        }

        List.of("constant", "will", "ritual").forEach(prop -> {
            if (!modItem.has(prop)) {
                return;
            }
            ArrayNode targetGroup = targetSpellcasting.withArray(prop);
            modItem.get(prop).forEach(targetGroup::add);
            targetSpellcasting.set(prop, sortArrayNode(targetGroup));
        });

        List.of("rest", "daily", "weekly", "yearly").forEach(prop -> {
            if (!modItem.has(prop)) {
                return;
            }

            ObjectNode targetGroup = targetSpellcasting.with(prop);
            for (int i = 1; i <= 9; ++i) {
                String key = i + "";
                if (modItem.get(prop).has(key)) {
                    modItem.get(prop).get(key).forEach(
                            sp -> targetGroup.withArray(key).add(sp));
                    targetGroup.set(key, sortArrayNode(targetGroup.withArray(key)));
                }

                String e = i + "e";
                if (modItem.get(prop).has(e)) {
                    modItem.get(prop).get(e).forEach(
                            sp -> targetGroup.withArray(e).add(sp));
                    targetGroup.set(e, sortArrayNode(targetGroup.withArray(e)));
                }
            }
        });

    }

    default void doAddSkills(String originKey, JsonNode modItem, String modFieldName, JsonNode target) {
        tui().debugf("Add skills %s", originKey);
        ObjectNode targetSkills = target.with("skill");
        modItem.get("skills").fields().forEachRemaining(e -> {
            // mode: 1 = proficient; 2 = expert
            int mode = e.getValue().asInt();
            String skill = e.getKey();
            String ability = getAbilityForSkill(skill);
            int score = target.get(ability).asInt();
            String cr = monsterCr(target);
            double total = mode * crToPb(cr) + getAbilityModNumber(score);
            String totalAsText = (total >= 0 ? "+" : "") + ((int) total);

            if (targetSkills.has(skill)) {
                if (targetSkills.get(skill).asDouble() < total) {
                    targetSkills.set(skill, new TextNode(totalAsText));
                }
            } else {
                targetSkills.set(skill, new TextNode(totalAsText));
            }
        });
    }

    default void doReplaceText(String originKey, JsonNode modItem, String modFieldName, JsonNode target) {
        String replace = modItem.get("replace").asText();
        String with = modItem.get("with").asText();
        JsonNode flags = modItem.get("flags");

        final Pattern pattern;
        if (flags != null) {
            int pFlags = 0;
            if (flags.asText().contains("i")) {
                pFlags |= Pattern.CASE_INSENSITIVE;
            }
            pattern = Pattern.compile(replace, pFlags);
        } else {
            pattern = Pattern.compile(replace);
        }

        JsonNode targetField = target.get(modFieldName);
        List<String> properties;
        final boolean findPlainText;
        if (modItem.has("props")) {
            properties = new ArrayList<>();
            modItem.withArray("props").forEach(x -> properties.add(x.isNull() ? "null" : x.asText()));
            properties.remove("null");
            findPlainText = properties.contains("null");
        } else {
            properties = List.of("entries", "headerEntries", "footerEntries");
            findPlainText = true;
        }

        if (findPlainText && targetField.isTextual()) {
            ((ObjectNode) target).set(modFieldName, copyReplaceNode(targetField, pattern, with));
            return;
        }

        targetField.forEach(x -> {
            if (!x.isObject()) {
                return;
            }
            properties.forEach(prop -> {
                if (x.has(prop)) {
                    ((ObjectNode) x).set(prop, copyReplaceNode(x.get(prop), pattern, with));
                }
            });
        });
    }

    default void doModArray(String originKey, JsonNode modItem, String fieldName, JsonNode target) {
        JsonNode items = modItem.has("items") ? copyNode(modItem.get("items")) : null;
        ArrayNode tgtArray = target.withArray(fieldName);
        switch (modItem.get("mode").asText()) {
            case "prependArr":
                insertIntoArray(tgtArray, 0, items);
                break;
            case "appendArr":
                appendToArray(tgtArray, items);
                break;
            case "appendIfNotExistsArr":
                if (tgtArray.size() == 0) {
                    appendToArray(tgtArray, items);
                } else {
                    assert items != null;
                    if (items.isArray()) {
                        List<JsonNode> filtered = streamOf((ArrayNode) items)
                                .filter(it -> streamOf(tgtArray).noneMatch(it::equals))
                                .collect(Collectors.toList());
                        tgtArray.addAll(filtered);
                    } else {
                        if (streamOf(tgtArray).noneMatch(items::equals)) {
                            tgtArray.add(items);
                        }
                    }
                }
                break;
            case "insertArr": {
                int index = modItem.get("index").asInt();
                insertIntoArray(tgtArray, index, items);
                break;
            }
            case "removeArr": {
                removeFromArray(originKey, modItem, tgtArray, items);
                break;
            }
            case "replaceArr": {
                JsonNode replace = modItem.get("replace");
                replaceArray(originKey, modItem, tgtArray, replace, items);
                break;
            }
            case "replaceOrAppendArr": {
                JsonNode replace = modItem.get("replace");
                if (!replaceArray(originKey, modItem, tgtArray, replace, items)) {
                    appendToArray(tgtArray, items);
                }
                break;
            }
            default:
                tui().errorf("Unknown modification mode: %s (from %s)", modItem.toPrettyString(), originKey);
                break;
        }
    }

    default void appendToArray(ArrayNode tgtArray, JsonNode items) {
        if (items == null) {
            return;
        }
        if (items.isArray()) {
            tgtArray.addAll((ArrayNode) items);
        } else {
            tgtArray.add(items);
        }
    }

    default void insertIntoArray(ArrayNode tgtArray, int index, JsonNode items) {
        if (items == null) {
            return;
        }
        if (items.isArray()) {
            // iterate backwards so that items end up in the right order @ desired index
            for (int i = items.size() - 1; i >= 0; i--) {
                tgtArray.insert(index, items.get(i));
            }
        } else {
            tgtArray.insert(index, items);
        }
    }

    default void removeFromArray(String originKey, JsonNode modItem, ArrayNode tgtArray, JsonNode items) {
        JsonNode names = modItem.get("names");
        if (modItem.has("names")) {
            if (names.isTextual()) {
                int index = findIndexByName(originKey, tgtArray, names.asText());
                if (index >= 0) {
                    tgtArray.remove(index);
                }
            } else if (names.isArray()) {
                modItem.withArray("names").forEach(name -> {
                    int index = findIndexByName(originKey, tgtArray, name.asText());
                    if (index >= 0) {
                        tgtArray.remove(index);
                    }
                });
            }
        } else if (items != null && items.isArray()) {
            items.forEach(x -> {
                int index = findIndex(tgtArray, x);
                if (index >= 0) {
                    tgtArray.remove(index);
                }
            });
        }
    }

    default boolean replaceArray(String originKey, JsonNode modItem, ArrayNode tgtArray, JsonNode replace, JsonNode items) {
        if (items == null) {
            return false;
        }
        int index = -1;
        if (replace.isTextual()) {
            index = findIndexByName(originKey, tgtArray, replace.asText());
        } else if (replace.isObject() && replace.has("index")) {
            index = replace.get("index").asInt();
        } else {
            tui().errorf("Unknown modification mode: %s (from %s)", modItem.toPrettyString(), originKey);
            return false;
        }

        if (index >= 0) {
            tgtArray.remove(index);
            insertIntoArray(tgtArray, index, items);
            return true;
        }
        return false;
    }

    default int findIndexByName(String originKey, ArrayNode haystack, String needle) {
        int index = -1;
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).isObject()) {
                if (haystack.get(i).has("name") && haystack.get(i).get("name").asText().equals(needle)) {
                    index = i;
                    break;
                }
            } else if (haystack.get(i).isTextual()) {
                if (haystack.get(i).asText().equals(needle)) {
                    index = i;
                    break;
                }
            } else {
                tui().errorf("Unknown entry type: %s (from %s)", haystack.get(i), originKey);
            }
        }
        return index;
    }

    default int findIndex(ArrayNode haystack, JsonNode needle) {
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).equals(needle)) {
                return i;
            }
        }
        return -1;
    }

    default int levelToPb(int level) {
        //2 + (¼  * (Level – 1))
        return 2 + ((int) (.25 * (level - 1)));
    }

    default String monsterCr(JsonNode monster) {
        if (monster.has("cr")) {
            JsonNode crNode = monster.get("cr");
            if (crNode.isTextual()) {
                return crNode.asText();
            } else if (crNode.has("cr")) {
                return crNode.get("cr").asText();
            } else {
                tui().errorf("Unable to parse cr value from %s", crNode.toPrettyString());
            }
        }
        return null;
    }

    default int crToPb(String crValue) {
        double crDouble = crToNumber(crValue);
        if (crDouble < 5)
            return 2;
        return (int) Math.ceil(crDouble / 4) + 1;
    }

    default double crToNumber(String crValue) {
        if (crValue.equals("Unknown") || crValue.equals("\u2014") || crValue == null) {
            return CR_UNKNOWN;
        }
        String[] parts = crValue.trim().split("/");
        try {
            if (parts.length == 1) {
                return Double.valueOf(parts[0]);
            } else if (parts.length == 2) {
                return Double.valueOf(parts[0]) / Double.valueOf(parts[1]);
            }
        } catch (NumberFormatException nfe) {
            return CR_CUSTOM;
        }
        return 0;
    }

    default int getAbilityModNumber(int abilityScore) {
        return (int) Math.floor((abilityScore - 10) / 2);
    };

    default String getAbilityForSkill(String skill) {
        switch (skill) {
            case "athletics":
                return "str";
            case "acrobatics":
                return "dex";
            case "sleight of hand":
                return "dex";
            case "stealth":
                return "dex";
            case "arcana":
                return "int";
            case "history":
                return "int";
            case "investigation":
                return "int";
            case "nature":
                return "int";
            case "religion":
                return "int";
            case "animal handling":
                return "wis";
            case "insight":
                return "wis";
            case "medicine":
                return "wis";
            case "perception":
                return "wis";
            case "survival":
                return "wis";
            case "deception":
                return "cha";
            case "intimidation":
                return "cha";
            case "performance":
                return "cha";
            case "persuasion":
                return "cha";
        }
        throw new IllegalArgumentException("Unknown skill: " + skill);
    }

    default String getSize(JsonNode value) {
        JsonNode size = value.get("size");
        if (size == null) {
            throw new IllegalArgumentException("Missing size attribute from " + getSources());
        }
        try {
            if (size.isTextual()) {
                return sizeToString(size.asText());
            } else if (size.isArray()) {
                String merged = streamOf((ArrayNode) size).map(JsonNode::asText).collect(Collectors.joining());
                return sizeToString(merged);
            }
        } catch (IllegalArgumentException ignored) {
        }
        tui().errorf("Unable to parse size for %s from %s", getSources(), size.toPrettyString());
        return "Unknown";
    }

    default String sizeToString(String size) {
        switch (size) {
            case "T":
                return "Tiny";
            case "S":
                return "Small";
            case "M":
                return "Medium";
            case "L":
                return "Large";
            case "H":
                return "Huge";
            case "G":
                return "Gargantuan";
            case "V":
                return "Varies";
            case "SM":
                return "Small or Medium";
        }
        return "Unknown";
    }

    default String getSpeed(JsonNode value) {
        JsonNode speed = value.get("speed");
        try {
            if (speed == null) {
                return "30 ft.";
            } else if (speed.isTextual()) {
                return speed.asText();
            } else if (speed.isIntegralNumber()) {
                return speed.asText() + " ft.";
            } else if (speed.isObject()) {
                List<String> list = new ArrayList<>();
                speed.fields().forEachRemaining(f -> {
                    if (f.getValue().isIntegralNumber()) {
                        list.add(String.format("%s: %s ft.",
                                f.getKey(), f.getValue().asText()));
                    } else if (f.getValue().isBoolean()) {
                        list.add(f.getKey() + " equal to your walking speed");
                    }
                });
                return String.join("; ", list);
            }
        } catch (IllegalArgumentException ignored) {
        }
        tui().errorf("Unable to parse speed for %s from %s", getSources(), speed);
        return "30 ft.";
    }

    default String raceToText(JsonNode race) {
        StringBuilder str = new StringBuilder();
        str.append(race.get("name").asText());
        if (race.has("subrace")) {
            str.append(" (").append(race.get("subrace").asText()).append(")");
        }
        return str.toString();
    }

    default String levelToText(JsonNode levelNode) {
        if (levelNode.isObject()) {
            List<String> levelText = new ArrayList<>();
            levelText.add(levelToText(levelNode.get("level").asText()));
            if (levelNode.has("class") || levelNode.has("subclass")) {
                JsonNode classNode = levelNode.get("class");
                if (classNode == null) {
                    classNode = levelNode.get("subclass");
                }
                boolean visible = !classNode.has("visible") || classNode.get("visible").asBoolean();
                JsonNode source = classNode.get("source");
                boolean included = source == null || index().sourceIncluded(source.asText());
                if (visible && included) {
                    levelText.add(classNode.get("name").asText());
                }
            }
            return String.join(" ", levelText);
        } else {
            return levelToText(levelNode.asText());
        }
    }

    default String levelToText(String level) {
        switch (level) {
            case "0":
                return "cantrip";
            case "1":
                return "1st-level";
            case "2":
                return "2nd-level";
            case "3":
                return "3rd-level";
            default:
                return level + "th-level";
        }
    }

    static String levelToString(int level) {
        switch (level) {
            case 1:
                return "1st";
            case 2:
                return "2nd";
            case 3:
                return "3rd";
            default:
                return level + "th";
        }
    }

    default void maybeAddBlankLine(List<String> text) {
        if (text.size() > 0 && !text.get(text.size() - 1).isBlank()) {
            text.add("");
        }
    }

    default void appendEntryToText(List<String> text, JsonNode node, String heading) {
        if (node == null) {
            // do nothing
        } else if (node.isTextual()) {
            text.add(replaceText(node.asText()));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(f -> {
                maybeAddBlankLine(text);
                appendEntryToText(text, f, heading);
            });
        } else if (node.isObject()) {
            appendEntryObjectToText(text, node, heading);
        } else {
            tui().errorf("Unknown entry type in %s: %s", getSources(), node.toPrettyString());
        }
    }

    default void appendEntryObjectToText(List<String> text, JsonNode node, String heading) {
        if (node.has("source") && !index().sourceIncluded(node.get("source").asText())) {
            return;
        } else if (!index().sourceIncluded(getSources().alternateSource())) {
            return;
        }

        if (node.has("type")) {
            String objectType = node.get("type").asText();
            switch (objectType) {
                case "section":
                case "entries": {
                    if (heading == null) {
                        List<String> inner = new ArrayList<>();
                        appendEntryToText(inner, node.get("entries"), null);
                        if (prependField(node, "name", inner)) {
                            maybeAddBlankLine(text);
                        }
                        text.addAll(inner);
                    } else if (node.has("name")) {
                        maybeAddBlankLine(text);
                        text.add(heading + " " + node.get("name").asText());
                        text.add("");
                        appendEntryToText(text, node.get("entries"), "#" + heading);
                    } else {
                        appendEntryToText(text, node.get("entries"), heading);
                    }
                    break;
                }
                case "entry":
                case "itemSpell":
                case "item": {
                    List<String> inner = new ArrayList<>();
                    appendEntryToText(inner, node.get("entry"), null);
                    appendEntryToText(inner, node.get("entries"), null);
                    if (prependField(node, "name", inner)) {
                        maybeAddBlankLine(text);
                    }
                    text.addAll(inner);
                    break;
                }
                case "link": {
                    text.add(node.get("text").asText());
                    break;
                }
                case "list": {
                    appendList(text, node.withArray("items"));
                    break;
                }
                case "table": {
                    appendTable(text, node);
                    break;
                }
                case "options":
                    appendOptions(text, node);
                    break;
                case "inset":
                case "insetReadaloud": {
                    appendInset(text, node);
                    break;
                }
                case "quote": {
                    appendQuote(text, node);
                    break;
                }
                case "abilityDc":
                    text.add(String.format("**Spell save DC**: 8 + your proficiency bonus + your %s modifier",
                            asAbilityEnum(node.withArray("attributes").get(0))));
                    break;
                case "abilityAttackMod":
                    text.add(String.format("**Spell attack modifier**: your proficiency bonus + your %s modifier",
                            asAbilityEnum(node.withArray("attributes").get(0))));
                    break;
                case "image":
                    // TODO: maybe someday?
                    break;
                default:
                    tui().errorf("Unknown entry object type %s from %s: %s", objectType, getSources(), node.toPrettyString());
            }
            // any entry/entries handled by type..
            return;
        }

        if (node.has("entry")) {
            appendEntryToText(text, node.get("entry"), heading);
        }
        if (node.has("entries")) {
            appendEntryToText(text, node.get("entries"), heading);
        }

        if (node.has("additionalEntries")) {
            String altSource = getSources().alternateSource();
            node.withArray("additionalEntries").forEach(entry -> {
                if (entry.has("source") && !index().sourceIncluded(entry.get("source").asText())) {
                    return;
                } else if (!index().sourceIncluded(altSource)) {
                    return;
                }
                appendEntryToText(text, entry, heading);
            });
        }
    }

    default boolean prependField(JsonNode entry, String fieldName, List<String> inner) {
        if (entry.has(fieldName)) {
            String n = entry.get(fieldName).asText();
            if (inner.isEmpty()) {
                inner.add(n);
            } else {
                n = replaceText(n.trim().replace(":", ""));
                n = "**" + n + ".** ";
                inner.set(0, n + inner.get(0));
                return true;
            }
        }
        return false;
    }

    default void prependText(String prefix, List<String> inner) {
        if (inner.isEmpty()) {
            inner.add(prefix);
        } else {
            if (inner.get(0).isEmpty() && inner.size() > 1) {
                inner.set(1, prependText(prefix, inner.get(1)));
            } else {
                inner.set(0, prependText(prefix, inner.get(0)));
            }
        }
    }

    default String prependText(String prefix, String text) {
        return text.startsWith(prefix) ? text : prefix + text;
    }

    default void appendList(List<String> text, ArrayNode itemArray) {
        maybeAddBlankLine(text);
        itemArray.forEach(e -> {
            List<String> item = new ArrayList<>();
            appendEntryToText(item, e, null);
            if (item.size() > 0) {
                prependText("- ", item);
                text.add(String.join("  \n    ", item)); // preserve line items
            }
        });
    }

    default void appendTable(List<String> text, JsonNode entry) {
        StringBuilder table = new StringBuilder();

        String caption = getTextOrEmpty(entry, "caption");
        if (!caption.isBlank()) {
            table.append("**").append(caption).append("**\n\n");
        }

        String header = StreamSupport.stream(entry.withArray("colLabels").spliterator(), false)
                .map(x -> replaceText(x.asText()))
                .collect(Collectors.joining(" | "));

        header = "| " + header.replaceAll("^(d[0-9]+.*)", "dice: $1") + " |";
        table.append(header).append("\n");
        table.append(header.replaceAll("[^|]", "-")).append("\n");

        entry.withArray("rows").forEach(r -> table
                .append("| ")
                .append(StreamSupport.stream(r.spliterator(), false)
                        .map(x -> replaceText(x.asText()))
                        .collect(Collectors.joining(" | ")))
                .append(" |\n"));

        if (!caption.isBlank()) {
            table.append("^").append(slugify(caption));
        } else {
            String blockid = header.replaceAll("dice: d[0-9]+", "")
                    .replace("|", "")
                    .replaceAll("\\s+", " ")
                    .trim();
            table.append("^").append(slugify(blockid));
        }

        maybeAddBlankLine(text);
        text.add(table.toString());
    }

    default void appendOptions(List<String> text, JsonNode entry) {
        List<String> list = new ArrayList<>();
        entry.withArray("entries").forEach(e -> {
            List<String> item = new ArrayList<>();
            appendEntryToText(item, e, null);
            if (item.size() > 0) {
                prependText("- ", item);
                list.add(String.join("  \n    ", item)); // preserve line items
            }
        });
        if (list.size() > 0) {
            maybeAddBlankLine(text);
            int count = intOrDefault(entry, "count", 0);
            text.add(String.format("Options%s:",
                    count > 0 ? " (choose " + count + ")" : ""));
            maybeAddBlankLine(text);
            text.addAll(list);
        }
    }

    default void appendInset(List<String> text, JsonNode entry) {
        List<String> insetText = new ArrayList<>();
        String id = null;
        if (entry.has("name")) {
            id = entry.get("name").asText();
            insetText.add("[!excerpt]- " + id);
            appendEntryToText(insetText, entry.get("entries"), null);
        } else if (getSources().type == IndexType.race) {
            appendEntryToText(insetText, entry.get("entries"), null);
            id = insetText.remove(0);
            insetText.add(0, "[!excerpt]- " + id);
        } else {
            id = entry.get("id").asText();
            insetText.add("[!excerpt]- ...");
            appendEntryToText(insetText, entry.get("entries"), null);
        }

        maybeAddBlankLine(text);
        insetText.forEach(x -> text.add("> " + x));
        if (id != null) {
            text.add("^" + slugify(id));
        }
    }

    default void appendQuote(List<String> text, JsonNode entry) {
        List<String> quoteText = new ArrayList<>();
        if (entry.has("by")) {
            String by = replaceText(entry.get("by").asText());
            quoteText.add("[!quote]- A quote from " + by + "  ");
        } else {
            quoteText.add("[!quote]-  ");
        }
        appendEntryToText(quoteText, entry.get("entries"), null);

        maybeAddBlankLine(text);
        quoteText.forEach(x -> text.add("> " + x));
        maybeAddBlankLine(text);
    }

    default String decoratedTypeName(CompendiumSources sources) {
        return decoratedTypeName(sources.name, sources);
    }

    default String decoratedTypeName(String name, CompendiumSources sources) {
        if (sources.isPrimarySource("DMG") && !name.contains("(DMG)")) {
            return name + " (DMG)";
        }
        if (sources.isFromUA() && !name.contains("(UA)")) {
            return name + " (UA)";
        }
        return name;
    }

    default String decoratedFeatureTypeName(CompendiumSources valueSources, JsonNode value) {
        String name = decoratedTypeName(value.get("name").asText(), valueSources);
        String type = getTextOrEmpty(value, "featureType");

        if (!type.isEmpty()) {
            switch (type) {
                case "ED":
                    return "Elemental Discipline: " + name;
                case "EI":
                    return "Eldritch Invocation: " + name;
                case "MM":
                    return "Metamagic: " + name;
                case "MV":
                case "MV:B":
                case "MV:C2-UA":
                    return "Maneuver: " + name;
                case "FS:F":
                case "FS:B":
                case "FS:R":
                case "FS:P":
                    return "Fighting Style: " + name;
                case "AS":
                case "AS:V1-UA":
                case "AS:V2-UA":
                    return "Arcane Shot: " + name;
                case "PB":
                    return "Pact Boon: " + name;
                case "AI":
                    return "Artificer Infusion: " + name;
                case "SHP:H":
                case "SHP:M":
                case "SHP:W":
                case "SHP:F":
                case "SHP:O":
                    return "Ship Upgrade: " + name;
                case "IWM:W":
                    return "Infernal War Machine Variant: " + name;
                case "IWM:A":
                case "IWM:G":
                    return "Infernal War Machine Upgrade: " + name;
                case "OR":
                    return "Onomancy Resonant: " + name;
                case "RN":
                    return "Rune Knight Rune: " + name;
                case "AF":
                    return "Alchemical Formula: " + name;
                default:
                    tui().errorf("Unknown feature type %s for class feature %s", type, name);
            }
        }

        return name;
    }

    default String asAbilityEnum(JsonNode textNode) {
        return SkillOrAbility.format(textNode.asText());
    }

    default String replaceText(String input) {
        String result = input;

        // "{@atk mw} {@hit 1} to hit, reach 5 ft., one target. {@h}1 ({@damage 1d4 ‒1}) piercing damage."
        // "{@atk mw} {@hit 4} to hit, reach 5 ft., one target. {@h}1 ({@damage 1d4+2}) slashing damage."
        // "{@atk mw} {@hit 14} to hit, one target. {@h}22 ({@damage 3d8}) piercing damage. Target must make a {@dc 19} Dexterity save, or be swallowed by the worm!"
        result = dicePattern.matcher(result)
                .replaceAll((match) -> match.group(2));
        result = chancePattern.matcher(result)
                .replaceAll((match) -> match.group(1) + "% chance");

        result = backgroundPattern.matcher(result)
                .replaceAll((match) -> linkify(match));

        result = classPattern1.matcher(result)
                .replaceAll((match) -> linkify(match));

        result = creaturePattern.matcher(result)
                .replaceAll((match) -> linkify(match));

        result = featPattern.matcher(result)
                .replaceAll((match) -> linkify(match));

        result = itemPattern.matcher(result)
                .replaceAll((match) -> linkify(match));

        result = racePattern.matcher(result)
                .replaceAll((match) -> linkify(match));

        result = spellPattern.matcher(result)
                .replaceAll((match) -> linkify(match));

        result = condPattern.matcher(result)
                .replaceAll((match) -> linkifyRules(match.group(1), "conditions"));

        result = skillPattern.matcher(result)
                .replaceAll((match) -> linkifyRules(match.group(1), "skills"));

        result = sensePattern.matcher(result)
                .replaceAll((match) -> linkifyRules(match.group(1), "senses"));

        result = notePattern.matcher(result)
                .replaceAll((match) -> {
                    List<String> text = new ArrayList<>();
                    text.add("> [!note]");
                    for (String line : match.group(2).split("\n")) {
                        text.add("> " + line);
                    }
                    return String.join("\n", text);
                });

        result = result
                .replace("{@hitYourSpellAttack}", "the summoner's spell attack modifier")
                .replaceAll("\\{@link ([^}|]+)\\|([^}]+)}", "$1 ($2)") // this must come first
                .replaceAll("\\{@5etools ([^}|]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@area ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@action ([^}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@disease ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@hazard ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@reward ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@dc ([^}]+)}", "DC $1")
                .replaceAll("\\{@d20 ([^}]+?)}", "$1")
                .replaceAll("\\{@recharge ([^}]+?)}", "(Recharge $1-6)")
                .replaceAll("\\{@recharge}", "(Recharge 6)")
                .replaceAll("\\{@filter ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@classFeature ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@optfeature ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@cult ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                .replaceAll("\\{@cult ([^|}]+)\\|[^}]*}", "$1")
                .replaceAll("\\{@deity ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@language ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@quickref ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@table ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@variantrule ([^|}]+)\\|?[^}]*}", "$1")
                .replaceAll("\\{@book ([^}|]+)\\|?[^}]*}", "\"$1\"")
                .replaceAll("\\{@hit ([^}]+)}", "+$1")
                .replaceAll("\\{@h}", "Hit: ")
                .replaceAll("\\{@atk m}", "*Melee Attack:*")
                .replaceAll("\\{@atk mw}", "*Melee Weapon Attack:*")
                .replaceAll("\\{@atk rw}", "*Ranged Weapon Attack:*")
                .replaceAll("\\{@atk mw,rw}", "*Melee or Ranged Weapon Attack:*")
                .replaceAll("\\{@atk ms}", "*Melee Spell Attack:*")
                .replaceAll("\\{@atk rs}", "*Ranged Spell Attack:*")
                .replaceAll("\\{@atk ms,rs}", "*Melee or Ranged Spell Attack:*")
                .replaceAll("\\{@b ([^}]+?)}", "**$1**")
                .replaceAll("\\{@i ([^}]+?)}", "_$1_")
                .replaceAll("\\{@italic ([^}]+)}", "_$1_");

        // after other replacements
        return result.replaceAll("\\{@adventure ([^|}]+)\\|[^}]*}", "$1");
    }

    default String linkifyRules(String text, String rules) {
        return String.format("[%s](%s%s.md#%s)",
                text, index().rulesRoot(), rules,
                text.replace(" ", "%20")
                        .replace(".", ""));
    }

    default String linkify(MatchResult match) {
        switch (match.group(1)) {
            case "background":
                // "Backgrounds:
                // {@background Charlatan} assumes PHB by default,
                // {@background Anthropologist|toa} can have sources added with a pipe,
                // {@background Anthropologist|ToA|and optional link text added with another pipe}.",
                return linkifyType(IndexType.background, match.group(2), "backgrounds");
            case "creature":
                // "Creatures:
                // {@creature goblin} assumes MM by default,
                // {@creature cow|vgm} can have sources added with a pipe,
                // {@creature cow|vgm|and optional link text added with another pipe}.",
                return linkifyCreature(match.group(2));
            case "class":
                return linkifyClass(match.group(2));
            case "feat":
                // "Feats:
                // {@feat Alert} assumes PHB by default,
                // {@feat Elven Accuracy|xge} can have sources added with a pipe,
                // {@feat Elven Accuracy|xge|and optional link text added with another pipe}.",
                return linkifyType(IndexType.feat, match.group(2), "feats");
            case "item":
                // "Items:
                // {@item alchemy jug} assumes DMG by default,
                // {@item longsword|phb} can have sources added with a pipe,
                // {@item longsword|phb|and optional link text added with another pipe}.",
                return linkifyType(IndexType.item, match.group(2), "items", "dmg");
            case "race":
                // "Races:
                // {@race Human} assumes PHB by default,
                // {@race Aarakocra|eepc} can have sources added with a pipe,
                // {@race Aarakocra|eepc|and optional link text added with another pipe}.",
                return linkifyType(IndexType.race, match.group(2), "races");
            case "spell":
                // "Spells:
                // {@spell acid splash} assumes PHB by default,
                // {@spell tiny servant|xge} can have sources added with a pipe,
                // {@spell tiny servant|xge|and optional link text added with another pipe}.",
                return linkifyType(IndexType.spell, match.group(2), "spells");
        }
        throw new IllegalArgumentException("Unknown group to linkify: " + match.group(1));
    }

    default String linkOrText(String linkText, String key, String dirName, String resourceName) {
        return index().keyIsIncluded(key)
                ? String.format("[%s](%s%s/%s.md)",
                        linkText, index().compendiumRoot(), dirName, slugify(resourceName))
                : linkText;
    }

    default String linkifyType(IndexType type, String match, String dirName) {
        return linkifyType(type, match, dirName, "phb");
    }

    default String linkifyType(IndexType type, String match, String dirName, String defaultSource) {
        String[] parts = match.split("\\|");
        String linkText = parts[0];
        String source = defaultSource;
        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (parts.length > 1) {
            source = parts[1].isBlank() ? source : parts[1];
        }
        String key = index().createSimpleKey(type, parts[0], source);
        return linkOrText(linkText, key, dirName, parts[0]);
    }

    default String linkifyClass(String match) {
        // "Classes:
        // {@class fighter} assumes PHB by default,
        // {@class artificer|uaartificer} can have sources added with a pipe,
        // {@class fighter|phb|optional link text added with another pipe},
        // {@class fighter|phb|subclasses added|Eldritch Knight} with another pipe,
        // {@class fighter|phb|and class feature added|Eldritch Knight|phb|2-0} with another pipe
        //    (first number is level index (0-19), second number is feature index (0-n)).",
        String[] parts = match.split("\\|");
        String className = parts[0];
        String classSource = "phb";
        String linkText = className;
        String subclass = null;
        String subclassSource = null;
        if (parts.length > 4) {
            subclassSource = parts[4];
        }
        if (parts.length > 3) {
            subclass = parts[3];
        }
        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (parts.length > 1) {
            classSource = parts[1];
        }
        if (subclass != null && subclassSource == null) {
            subclassSource = classSource;
        }

        if (subclass != null) {
            String key = index().getSubclassKey(subclass, className, classSource);
            return linkOrText(linkText, key, "classes", className + "-" + subclass);
        } else {
            String key = index().getClassKey(className, classSource);
            return linkOrText(linkText, key, "classes", className);
        }
    }

    default String linkifyCreature(String match) {
        // "Creatures:
        // {@creature goblin} assumes MM by default,
        // {@creature cow|vgm} can have sources added with a pipe,
        // {@creature cow|vgm|and optional link text added with another pipe}.",
        String[] parts = match.trim().split("\\|");
        String linkText = parts[0];
        String source = "mm";
        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (parts.length > 1) {
            source = parts[1].isBlank() ? source : parts[1];
        }
        String key = index().createSimpleKey(IndexType.monster, parts[0], source);
        JsonNode jsonSource = index().getNode(key);
        String type = jsonSource == null ? null : getTextOrEmpty(jsonSource, "type");
        if (index().keyIsExcluded(key) || type == null) {
            return linkText;
        }
        return linkOrText(linkText, key, "bestiary/" + slugify(type), parts[0]);
    }
}
