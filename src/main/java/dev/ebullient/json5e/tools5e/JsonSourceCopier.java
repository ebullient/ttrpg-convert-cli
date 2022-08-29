package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.json5e.io.Json5eTui;

public class JsonSourceCopier implements JsonSource {

    final JsonIndex index;

    JsonSourceCopier(JsonIndex index) {
        this.index = index;
    }

    @Override
    public JsonIndex index() {
        return index;
    }

    @Override
    public CompendiumSources getSources() {
        throw new IllegalStateException("Should not call getSources while copying source");
    }

    JsonNode handleCopy(IndexType type, JsonNode jsonSource) {
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
            JsonNode baseNode = index().getOrigin(type, _copy);
            if (baseNode != null) {
                // is the copy a copy?
                baseNode = handleCopy(type, baseNode);
                try {
                    String originKey = index().getKey(type, jsonSource);
                    jsonSource = mergeNodes(originKey, baseNode, jsonSource);
                } catch (IllegalStateException | StackOverflowError e) {
                    throw new IllegalStateException("Unable to resolve copy " + _copy.toPrettyString());
                }
            }
        }
        return jsonSource;
    }

    JsonNode copyAndMergeRace(JsonNode jsonNode) {
        if (jsonNode.has("raceName") || jsonNode.has("_copy")) {
            CompendiumSources sources = index().constructSources(IndexType.race, jsonNode);
            jsonNode = cloneOrCopy(sources.getKey(),
                    jsonNode, IndexType.race,
                    getTextOrDefault(jsonNode, "raceName", null),
                    getTextOrDefault(jsonNode, "raceSource", null));
        }
        return jsonNode;
    }

    JsonNode copyAndMergeClass(JsonNode jsonSource) {
        if (jsonSource.has("className") || jsonSource.has("_copy")) {
            CompendiumSources sources = index().constructSources(IndexType.classtype, jsonSource);
            jsonSource = cloneOrCopy(sources.getKey(),
                    jsonSource, IndexType.classtype,
                    getTextOrDefault(jsonSource, "className", null),
                    getTextOrDefault(jsonSource, "classSource", null));
        }
        return jsonSource;
    }

    JsonNode cloneOrCopy(String originKey, JsonNode value, IndexType parentType, String parentName,
            String parentSource) {
        JsonNode parentNode = parentName == null ? null : index().getOrigin(parentType, parentName, parentSource);
        JsonNode copyNode = index().getOrigin(parentType, value.get("_copy"));
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
        return value;
    }

    JsonNode mergeNodes(String originKey, JsonNode baseNode, JsonNode overlayNode) {
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
                            mergeFields(overlayField.get(i), (ObjectNode) cpyAbility.get(i));
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
                                mergeFields(overlayField.get(i), (ObjectNode) cpyArray.get(i));
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

    ArrayNode sortArrayNode(ArrayNode array) {
        if (array == null || array.size() <= 1) {
            return array;
        }
        Set<JsonNode> elements = new TreeSet<>(Comparator.comparing(a -> a.asText().toLowerCase()));
        array.forEach(elements::add);
        ArrayNode sorted = Json5eTui.MAPPER.createArrayNode();
        elements.forEach(sorted::add);
        return sorted;
    }

    void mergeFields(JsonNode sourceNode, ObjectNode targetNode) {
        sourceNode.fields().forEachRemaining(f -> targetNode.set(f.getKey(), copyNode(f.getValue())));
    }

    void handleModifications(String originKey, String prop, JsonNode modInfo, JsonNode target) {
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

    void doMod(String originKey, JsonNode modItem, String modFieldName, JsonNode target) {
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
                doReplaceText(modItem, modFieldName, target);
                break;
            case "addSkills":
                doAddSkills(originKey, modItem, target);
                break;
            case "addSpells":
                doAddSpells(originKey, modItem, target);
                break;
            case "removeSpells":
                doRemoveSpells(originKey, modItem, target);
                break;
            case "replaceSpells":
                doReplaceSpells(originKey, modItem, target);
                break;
            default:
                tui().errorf("Unknown modification mode: %s (from %s)", modItem.toPrettyString(), originKey);
                break;
        }
    }

    void doRemoveSpells(String originKey, JsonNode modItem, JsonNode target) {
        if (!target.has("spellcasting")) {
            throw new IllegalStateException("Can't remove spells from a monster without spellcasting: " + originKey);
        }

        JsonNode arrayNode = target.get("spellcasting");
        arrayNode.forEach(targetSpellcasting -> List.of("rest", "daily", "weekly", "yearly").forEach(prop -> {
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
        }));
    }

    void doReplaceSpells(String originKey, JsonNode modItem, JsonNode target) {
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

    void doAddSpells(String originKey, JsonNode modItem, JsonNode target) {
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

    void doAddSkills(String originKey, JsonNode modItem, JsonNode target) {
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

    void doReplaceText(JsonNode modItem, String modFieldName, JsonNode target) {
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

    void doModArray(String originKey, JsonNode modItem, String fieldName, JsonNode target) {
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

    void appendToArray(ArrayNode tgtArray, JsonNode items) {
        if (items == null) {
            return;
        }
        if (items.isArray()) {
            tgtArray.addAll((ArrayNode) items);
        } else {
            tgtArray.add(items);
        }
    }

    void insertIntoArray(ArrayNode tgtArray, int index, JsonNode items) {
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

    void removeFromArray(String originKey, JsonNode modItem, ArrayNode tgtArray, JsonNode items) {
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

    boolean replaceArray(String originKey, JsonNode modItem, ArrayNode tgtArray, JsonNode replace, JsonNode items) {
        if (items == null) {
            return false;
        }
        int index;
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

    int findIndexByName(String originKey, ArrayNode haystack, String needle) {
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

    int findIndex(ArrayNode haystack, JsonNode needle) {
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).equals(needle)) {
                return i;
            }
        }
        return -1;
    }

    int getAbilityModNumber(int abilityScore) {
        return (int) Math.floor((abilityScore - 10) / 2);
    }

    String getAbilityForSkill(String skill) {
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
}
