package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonSourceCopier implements JsonSource {
    static final Pattern spell_dc_subst = Pattern.compile("<\\$spell_dc__([^$]+)\\$>");
    static final Pattern to_hit_subst = Pattern.compile("\\+?<\\$to_hit__([^$]+)\\$>");
    static final Pattern dmg_mod_subst = Pattern.compile("<\\$damage_mod__([^$]+)\\$>");
    static final Pattern dmg_avg_subst = Pattern.compile("<\\$damage_avg__([\\d.,]+)([+*-])([^$]+)\\$>");

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
        JsonNode _trait = _copy == null ? null : _copy.get("_trait");
        JsonNode overwrite = _copy == null ? null : _copy.get("overwrite");

        if (_preserve == null || !_preserve.has("reprintedAs")) {
            target.remove("reprintedAs");
        }
        if (_preserve == null || !_preserve.has("otherSources")) {
            target.remove("otherSources");
        }
        if (_preserve == null || !_preserve.has("additionalSources")) {
            target.remove("additionalSources");
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
                            tui().errorf(
                                    "Copy/Merge: Ability array lengths did not match (from %s):%nBASE:%n%s%nCOPY:%n%s",
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

        if (_trait != null) {
            String key = index.getKey(IndexType.trait, _trait);
            JsonNode trait = index.getOrigin(key);
            if (trait == null) {
                tui().warn("Unable to find trait for " + key);
            } else {
                JsonNode apply = trait.get("apply");
                if (apply != null) {
                    if (apply.has("_root")) {
                        apply.get("_root").fields()
                                .forEachRemaining(field -> target.replace(field.getKey(), copyNode(field.getValue())));
                    }
                    handleMod(originKey, target, apply.get("_mod"));
                }
            }
        }

        handleMod(originKey, target, _mod);

        String targetString = target.toString()
                .replace("<$name$>", baseNode.get("name").asText())
                .replace("<$title_short_name$>", getShortName(target, true))
                .replace("<$short_name$>", getShortName(target, false));

        targetString = spell_dc_subst.matcher(targetString)
                .replaceAll((match) -> getSpellDc(target, match.group(1)));

        targetString = to_hit_subst.matcher(targetString)
                .replaceAll((match) -> getToHitString(target, match.group(1)));

        targetString = dmg_mod_subst.matcher(targetString)
                .replaceAll((match) -> getDamageMod(target, match.group(1)));

        targetString = dmg_avg_subst.matcher(targetString)
                .replaceAll((match) -> getDamageAvg(target, match.group(1), match.group(2), match.group(3)));

        try {
            return mapper().readTree(targetString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return target;
        }
    }

    private String getSpellDc(JsonNode target, String ability) {
        int mod = getAbilityModNumber(target.get(ability).asInt());
        int pb = crToPb(target.get("cr"));
        return (8 + mod + pb) + "";
    }

    private String getDamageMod(JsonNode target, String ability) {
        int mod = getAbilityModNumber(target.get(ability).asInt());
        return mod == 0
                ? ""
                : (mod >= 0 ? "+" : "") + mod;
    }

    private String getDamageAvg(JsonNode target, String amount, String op, String ability) {
        int mod = getAbilityModNumber(target.get(ability).asInt());
        if ("+".equals(op)) {
            double total = Double.parseDouble(amount) + mod;
            return (total >= 0 ? "+" : "") + Math.floor(total);
        }
        throw new IllegalArgumentException("Unknown damage average operation: " + op + " for " + target.get("name").asText());
    }

    private String getToHitString(JsonNode target, String ability) {
        int pb = crToPb(target.get("cr"));
        int mod = getAbilityModNumber(target.get(ability).asInt());
        int total = pb + mod;
        return (total >= 0 ? "+" : "") + total;
    }

    private String getShortName(JsonNode target, boolean isTitleCase) {
        String name = target.get("name").asText();
        JsonNode shortName = target.get("shortName");
        boolean isNamedCreature = target.has("isNamedCreature");
        String prefix = isNamedCreature
                ? ""
                : isTitleCase ? "The " : "the ";

        if (shortName != null) {
            return shortName.isBoolean()
                    ? prefix + name
                    : prefix + (isNamedCreature
                            ? shortName.asText()
                            : isTitleCase
                                    ? toTitleCase(shortName.asText())
                                    : shortName.asText().toLowerCase());
        }

        return prefix + getShortNameFromName(name, isNamedCreature);
    }

    private String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return Arrays
                .stream(text.split(" "))
                .map(word -> word.isEmpty()
                        ? word
                        : Character.toTitleCase(word.charAt(0)) + word
                                .substring(1)
                                .toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private String getShortNameFromName(String name, boolean isNamedCreature) {
        String result = name.split(",")[0]
                .replaceAll("(?i)(?:adult|ancient|young) \\w+ (dragon|dracolich)", "$1");

        return isNamedCreature
                ? result.split(" ")[0]
                : result.toLowerCase();
    }

    void handleMod(String originKey, JsonNode target, JsonNode _mod) {
        if (_mod != null) {
            _mod.fields().forEachRemaining(field -> {
                if (field.getKey().equals("*")) {
                    List.of("action", "bonus", "reaction", "trait", "legendary",
                            "mythic", "variant", "spellcasting",
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

            if (_mod.has("calculateProp")) {
                doCalculateProp(originKey, _mod.get("calculateProp"), target);
            }
        }
    }

    ArrayNode sortArrayNode(ArrayNode array) {
        if (array == null || array.size() <= 1) {
            return array;
        }
        Set<JsonNode> elements = new TreeSet<>(Comparator.comparing(a -> a.asText().toLowerCase()));
        array.forEach(elements::add);
        ArrayNode sorted = mapper().createArrayNode();
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
            case "addSenses":
                doAddSenses(originKey, modItem, target);
                break;
            case "addSaves":
                doAddSaves(originKey, modItem, target, false);
                break;
            case "addSkills":
                doAddSkills(originKey, modItem, target, false);
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
            case "maxSize":
                doMaxSize(originKey, modItem, target);
                break;
            case "scalarAddProp":
                doScalarAddProp(originKey, modItem, target, modFieldName);
                break;
            case "scalarMultProp":
                doScalarMultProp(originKey, modItem, target, modFieldName);
                break;
            case "scalarAddHit":
                doScalarAddHit(originKey, modItem, target, modFieldName);
                break;
            case "scalarAddDc":
                doScalarAddDc(originKey, modItem, target, modFieldName);
                break;
            case "scalarMultXp":
                doScalarMultXp(originKey, modItem, target);
                break;
            case "addAllSaves":
            case "addAllSkills":
            case "calculateProp":
                // ignore
                break;
            default:
                tui().errorf("Unknown modification mode: %s (from %s)", modItem.toPrettyString(), originKey);
                break;
        }
    }

    static final Pattern dcPattern = Pattern.compile("\\{@dc (\\d+)(?:\\|[^}]+)?}");

    private void doScalarAddDc(String originKey, JsonNode modItem, JsonNode target, String modFieldName) {
        if (!target.has(modFieldName)) {
            return;
        }
        int scalar = modItem.get("scalar").asInt();
        String fullNode = dcPattern.matcher(target.get(modFieldName).toString())
                .replaceAll((match) -> "{@dc " + (Integer.parseInt(match.group(1)) + scalar) + "}");

        try {
            ((ObjectNode) target).replace(modFieldName, mapper().readTree(fullNode));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to apply scalar to dc " + target.get(modFieldName).toString());
        }
    }

    static final Pattern hitPattern = Pattern.compile("\\{@hit ([-+]?\\d+)}");

    private void doScalarAddHit(String originKey, JsonNode modItem, JsonNode target, String modFieldName) {
        if (!target.has(modFieldName)) {
            return;
        }
        int scalar = modItem.get("scalar").asInt();
        String fullNode = hitPattern.matcher(target.get(modFieldName).toString())
                .replaceAll((match) -> "{@hit " + (Integer.parseInt(match.group(1)) + scalar) + "}");

        try {
            ((ObjectNode) target).replace(modFieldName, mapper().readTree(fullNode));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to apply scalar to hit " + target.get(modFieldName).toString());
        }
    }

    private void doScalarMultXp(String originKey, JsonNode modItem, JsonNode target) {
        double scalar = modItem.get("scalar").asDouble();
        boolean floor = booleanOrDefault(modItem, "floor", false);

        JsonNode crNode = target.get("cr");
        int xp = crToXp(crNode);
        double result = xp * scalar;

        String newCr = JsonSource.XP_CHART_ALT.entrySet().stream()
                .filter(e -> e.getValue() <= result)
                .max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();

        ObjectNode o = mapper().createObjectNode();
        o.set("cr", new TextNode(newCr));
        o.set("xp", doubleToJsonNode(null, result, floor));

        ((ObjectNode) target).replace("cr", o);
    }

    private void doScalarMultProp(String originKey, JsonNode modItem, JsonNode target, String modFieldName) {
        if (!target.has(modFieldName)) {
            return;
        }
        double scalar = modItem.get("scalar").asDouble();
        String prop = modItem.get("prop").asText();
        boolean floor = booleanOrDefault(modItem, "floor", false);
        if (prop.equals("*")) {
            Iterator<String> i = target.get(modFieldName).fieldNames();
            while (i.hasNext()) {
                String name = i.next();
                applyMultiply((ObjectNode) target.get(modFieldName), name, scalar, floor);
            }
        } else {
            applyMultiply((ObjectNode) target.get(modFieldName), prop, scalar, floor);
        }
    }

    private void applyMultiply(ObjectNode target, String prop, double scalar, boolean floor) {
        double value = target.get(prop).asDouble();
        double newValue = value * scalar;
        target.replace(prop, doubleToJsonNode(target.get(prop), newValue, floor));
    }

    private void doScalarAddProp(String originKey, JsonNode modItem, JsonNode target, String modFieldName) {
        if (!target.has(modFieldName)) {
            return;
        }
        double scalar = modItem.get("scalar").asDouble();
        String prop = modItem.get("prop").asText();
        if (prop.equals("*")) {
            Iterator<String> i = target.get(modFieldName).fieldNames();
            while (i.hasNext()) {
                String name = i.next();
                applyAdd((ObjectNode) target.get(modFieldName), name, scalar);
            }
        } else {
            applyAdd((ObjectNode) target.get(modFieldName), prop, scalar);
        }
    }

    private void applyAdd(ObjectNode target, String prop, double scalar) {
        double value = target.get(prop).asDouble();
        double newValue = value + scalar;
        target.replace(prop, doubleToJsonNode(target.get(prop), newValue, true));
    }

    private void doCalculateProp(String originKey, JsonNode modItem, JsonNode target) {
        String prop = modItem.get("prop").asText();
        String formula = modItem.get("formula").asText();
        double prBonus = crToPb(target.get("cr"));
        int dexMod = getAbilityModNumber(target.get("dex").asInt());

        throw new IllegalStateException("doCalculateProp is implemented yet");
    }

    JsonNode doubleToJsonNode(JsonNode original, double newValue, boolean floor) {
        if (original != null && original.isTextual()) {
            return new TextNode((newValue >= 0 ? "+" : "") + (int) Math.floor(newValue));
        }
        if (floor) {
            return new IntNode((int) Math.floor(newValue));
        }
        return new DoubleNode(newValue);
    }

    private void doMaxSize(String originKey, JsonNode modItem, JsonNode target) {
        final String sizes = "VFDTSMLHGC";
        String maxValue = getTextOrEmpty(modItem, "max");
        int max = sizes.indexOf(maxValue);
        boolean set = false;
        ArrayNode size = target.withArray("size");
        for (int i = size.size() - 1; i >= 0; i--) {
            String s = size.get(i).asText();
            if (sizes.indexOf(s) > max) {
                size.remove(i);
                set = true;
            }
        }
        if (set) {
            size.add(maxValue);
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
            ObjectNode targetGroup = targetSpellcasting.withObject("/" + prop);
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
                ObjectNode targetSpells = targetSpellcasting.withObject("/spells");
                spells.fields().forEachRemaining(ss -> {
                    if (targetSpells.has(ss.getKey())) {
                        JsonNode levelMetas = ss.getValue();
                        ObjectNode targetLevel = targetSpells.withObject("/" + ss.getKey());
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
                ObjectNode targetGroup = targetSpellcasting.withObject("/" + prop);
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
            ObjectNode targetSpells = targetSpellcasting.withObject("/spells");
            spells.fields().forEachRemaining(s -> {
                if (!targetSpells.has(s.getKey())) {
                    targetSpells.set(s.getKey(), sortArrayNode((ArrayNode) s.getValue()));
                } else {
                    JsonNode spellsNew = spells.get(s.getKey());
                    ObjectNode spellsTgt = targetSpells.withObject("/" + s.getKey());
                    spellsNew.fields().forEachRemaining(ss -> {
                        if (!spellsTgt.has(ss.getKey())) {
                            spellsTgt.set(ss.getKey(), sortArrayNode((ArrayNode) ss.getValue()));
                        } else if (spellsTgt.get(ss.getKey()).isArray()) {
                            ArrayNode spellsArray = spellsTgt.withArray(ss.getKey());
                            appendToArray(spellsArray, copyNode(ss.getValue()));
                            targetSpells.set(s.getKey(), sortArrayNode(spellsArray));
                        } else if (spellsTgt.get(ss.getKey()).isObject()) {
                            throw new IllegalArgumentException(
                                    String.format("Object %s is not an array (referenced from %s)", ss.getKey(),
                                            originKey));
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

            ObjectNode targetGroup = targetSpellcasting.withObject("/" + prop);
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

    void doAddSenses(String originKey, JsonNode modItem, JsonNode target) {
        tui().debugf("Add senses %s", originKey);
        if (!modItem.has("senses") || modItem.get("senses").size() == 0) {
            return;
        }
        JsonNode senses = modItem.get("senses");
        if (senses.isObject()) {
            senses = mapper().createArrayNode();
            ((ArrayNode) senses).add(modItem.get("senses"));
        }

        ArrayNode targetSenses = target.withArray("senses");

        senses.forEach(sense -> {
            String type = sense.get("type").asText();
            int range = sense.get("range").asInt();
            TextNode newValue = new TextNode(String.format("%s %s ft.", type, range));

            boolean found = false;
            for (int i = 0; i < targetSenses.size(); i++) {
                Matcher m = Pattern.compile(type + " (\\d+)", i).matcher(sense.asText());
                if (m.matches()) {
                    found = true;
                    if (Integer.parseInt(m.group(1)) < range) {
                        targetSenses.set(i, newValue);
                    }
                }
            }
            if (!found) {
                targetSenses.add(newValue);
            }
        });

    }

    void doAddSaves(String originKey, JsonNode modItem, JsonNode target, boolean allSaves) {
        tui().debugf("Add saves %s", originKey);
        // if (allSaves) {
        //     int value = modItem.get("saves").asInt();
        //     if (value > 0) {
        //         ObjectNode recurse = mapper().createObjectNode();
        //         ObjectNode newSaves = mapper().createObjectNode();
        //         SkillOrAbility.allSaves.forEach(x -> newSaves.put(x, value));
        //         recurse.set("saves", newSaves);
        //         doAddSaves(originKey, recurse, target, false);
        //     }
        //     return;
        // }
        ObjectNode targetSaves = target.withObject("/save");
        modItem.get("saves").fields().forEachRemaining(e -> {
            int mode = e.getValue().asInt();
            String ability = e.getKey();
            String cr = monsterCr(target);
            double total = mode * crToPb(cr) + getAbilityModNumber(target.get(ability).asInt());
            String totalAsText = (total >= 0 ? "+" : "") + ((int) total);

            if (targetSaves.has(ability)) {
                if (targetSaves.get(ability).asDouble() < total) {
                    targetSaves.set(ability, new TextNode(totalAsText));
                }
            } else {
                targetSaves.set(ability, new TextNode(totalAsText));
            }
        });
    }

    void doAddSkills(String originKey, JsonNode modItem, JsonNode target, boolean allSkills) {
        tui().debugf("Add skills %s", originKey);
        // if (allSkills) {
        //     int value = modItem.get("skills").asInt();
        //     if (value > 0) {
        //         ObjectNode recurse = mapper().createObjectNode();
        //         ObjectNode newSkills = mapper().createObjectNode();
        //         SkillOrAbility.allSkills.forEach(x -> newSkills.put(x, value));
        //         recurse.set("skills", newSkills);
        //         doAddSkills(originKey, recurse, target, false);
        //     }
        //     return;
        // }
        ObjectNode targetSkills = target.withObject("/skills");
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
            case "sleight of hand":
            case "stealth":
                return "dex";
            case "arcana":
            case "history":
            case "investigation":
            case "nature":
            case "religion":
                return "int";
            case "animal handling":
            case "insight":
            case "medicine":
            case "perception":
            case "survival":
                return "wis";
            case "deception":
            case "intimidation":
            case "performance":
            case "persuasion":
                return "cha";
        }
        throw new IllegalArgumentException("Unknown skill: " + skill);
    }
}
