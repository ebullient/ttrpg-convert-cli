package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.Json2QuteItem.ItemField;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources.SourceAttributes;

public class MagicVariant implements JsonSource {

    static final List<String> IGNORE = List.of("entries", "rarity", "namePrefix", "nameSuffix");
    static final Pattern EXPRESSION = Pattern.compile("\\[\\[([^\\]]+)]]");
    static final Pattern TEMPLATE = Pattern.compile("\\{=([^}]+)}");

    // Process operations in this order always (see Renderer.applyProperties._OP_ORDER)
    static final String FIXED_OP_ORDER = "ltua";
    static final List<String> _LEADING_AN = List.of("a", "e", "i", "o", "u");

    static final MagicVariant INSTANCE = new MagicVariant();

    public static List<JsonNode> findSpecificVariants(Tools5eIndex index, Tools5eIndexType type,
            String key, JsonNode genericVariant, Tools5eJsonSourceCopier copier,
            List<JsonNode> baseItems) {
        return INSTANCE.findVariants(index, type, key, genericVariant, copier, baseItems);
    }

    public static void populateGenericVariant(final JsonNode variant) {
        INSTANCE.populateVariant(variant);
    }

    /**
     * Update generic variant item with inherited attributes
     * (minimally source, which is required to create the key)
     */
    private void populateVariant(final JsonNode variant) {
        JsonNode inherits = MagicItemField.inherits.getFrom(variant);

        // for (const prop in genericVariant.inherits) {
        //     if (Renderer.item._INHERITED_PROPS_BLOCKLIST.has(prop)) continue;
        //     const val = genericVariant.inherits[prop];
        //     if (val == null) delete genericVariant[prop];
        //     else if (genericVariant[prop]) {
        //         if (genericVariant[prop] instanceof Array && val instanceof Array)
        //             genericVariant[prop] = MiscUtil.copyFast(genericVariant[prop]).concat(val);
        //         else genericVariant[prop] = val;
        //     } else genericVariant[prop] = genericVariant.inherits[prop];
        // }
        List<String> fieldNames = streamOfFieldNames(inherits).toList();
        for (String fieldName : fieldNames) {
            if (IGNORE.contains(fieldName)) {
                continue;
            }
            JsonNode existing = variant.get(fieldName);
            JsonNode newValue = inherits.get(fieldName);
            if ((newValue == null || newValue.isNull()) && existing != null) {
                ((ObjectNode) variant).remove(fieldName);
            } else if (existing != null && existing.isArray() && newValue.isArray()) {
                ((ArrayNode) existing).addAll((ArrayNode) newValue);
            } else {
                ((ObjectNode) variant).set(fieldName, newValue);
            }
        }

        // if (!genericVariant.entries && genericVariant.inherits.entries) {
        //      genericVariant.entries = MiscUtil.copyFast(Renderer.applyAllProperties(genericVariant.inherits.entries, genericVariant.inherits));
        // }
        if (!SourceField.entries.existsIn(variant) && SourceField.entries.existsIn(inherits)) {
            SourceField.entries.setIn(variant,
                    resolveEntryAttributes(
                            SourceField.entries.getFrom(inherits),
                            tokenResolver(inherits)));
        }

        // if (genericVariant.inherits.rarity == null) delete genericVariant.rarity;
        // else if (genericVariant.inherits.rarity === "varies") {
        // /* No-op, i.e., use current rarity */
        // } else genericVariant.rarity = genericVariant.inherits.rarity;
        String value = MagicItemField.rarity.getTextOrNull(inherits);
        if (value == null) {
            MagicItemField.rarity.removeFrom(variant);
        } else if ("varies".equals(value)) {
            // no-op
        } else {
            MagicItemField.rarity.setIn(variant, value);
        }

        // if (genericVariant.requires.armor)
        //     genericVariant.armor = genericVariant.requires.armor;
        JsonNode armor = MagicItemField.armor.getFrom(MagicItemField.requires.getFrom(variant));
        if (armor != null) {
            MagicItemField.armor.setIn(variant, armor);
        }
    }

    /** Update / replace item with variants (where appropriate) */
    private List<JsonNode> findVariants(Tools5eIndex index, Tools5eIndexType type,
            String key, JsonNode genericVariant, Tools5eJsonSourceCopier copier,
            List<JsonNode> baseItems) {
        List<JsonNode> variants = new ArrayList<>();
        // baseItems.forEach((curBaseItem) => {
        //     ....
        //     genericVariants.forEach((curGenericVariant) => {
        //         if (!Renderer.item._createSpecificVariants_isEditionMatch({curBaseItem, curGenericVariant})) return;
        //         if (!Renderer.item._createSpecificVariants_hasRequiredProperty(curBaseItem, curGenericVariant)) return;
        //         if (Renderer.item._createSpecificVariants_hasExcludedProperty(curBaseItem, curGenericVariant)) return;
        //         genericAndSpecificVariants.push(Renderer.item._createSpecificVariants_createSpecificVariant(curBaseItem, curGenericVariant, opts));
        //     });
        // });
        // ..
        // We're looping the other way (variant is the outer loop / is passed in)
        boolean spawnNewItems = key.contains(" (*)");

        ArrayNode specificVariantListNode = null;
        String gvKey = Tools5eIndexType.item.createKey(genericVariant);
        if (!spawnNewItems) {
            // Add generic variant to the list of variants as a regular item
            // Variations will be added to this item.
            TtrpgValue.indexInputType.setIn(genericVariant, Tools5eIndexType.item.name());
            TtrpgValue.indexKey.setIn(genericVariant, gvKey);
            variants.add(genericVariant);
            index.addAlias(key, gvKey);
            specificVariantListNode = ItemField._variants.ensureArrayIn(genericVariant);
            ItemField._variants.setIn(genericVariant, specificVariantListNode);
        }

        String fluffKey = ItemField.hasFluff.booleanOrDefault(genericVariant, false)
                || ItemField.hasFluffImages.booleanOrDefault(genericVariant, false)
                        ? Tools5eIndexType.itemFluff.createKey(genericVariant)
                        : null;

        for (JsonNode baseItem : baseItems) {
            if (ItemField.packContents.existsIn(baseItem)
                    || !editionMatch(baseItem, genericVariant)
                    || !hasRequiredProperty(baseItem, genericVariant)
                    || hasExcludedProperty(baseItem, genericVariant)) {
                continue;
            }
            JsonNode specficVariant = createSpecificVariant(baseItem, genericVariant);
            if (specficVariant != null) {
                String newKey = Tools5eIndexType.item.createKey(specficVariant);
                TtrpgValue.indexInputType.setIn(specficVariant, Tools5eIndexType.item.name());
                TtrpgValue.indexKey.setIn(specficVariant, newKey);
                if (fluffKey != null) {
                    TtrpgValue.indexFluffKey.setIn(specficVariant, fluffKey);
                }
                Tools5eSources.constructSources(newKey, specficVariant);
                if (spawnNewItems) {
                    variants.add(specficVariant);
                    if (key.replace(" (*)", "").replace("magicvariant", "item").equals(newKey)) {
                        index.addAlias(key, newKey);
                    }
                    if (gvKey.replace(" (*)", "").equals(newKey)) {
                        index.addAlias(gvKey, newKey);
                    }
                } else {
                    // add variant to list of variants for this generic variant
                    // magic variant remains in index as a magic variant
                    specificVariantListNode.add(specficVariant);
                    index.addAlias(newKey, gvKey);
                }
            }
        }
        return variants;
    }

    // @formatter:off
    /**
     * render.js -- _createSpecificVariants_isEditionMatch
     *
     * When creating specific variants, the application of "classic" and "one" editions
     * goes by the following logic:
     *
     * |  B. Item | Gen. Var | Apply | Example
     * |----------|----------|-------|----------------------------------------
     * |     null |     null |     X | "Fool's Blade|BMT" -> "Pitchfork|ToB3-Lairs"
     * |  classic |     null |       | "Fool's Blade|BMT" -> "Longsword|PHB"
     * |      one |     null |     X | "Fool's Blade|BMT" -> "Longsword|XPHB"
     * |     null |  classic |     X | "+1 Weapon|DMG" -> "Pitchfork|ToB3-Lairs" -- TODO(Future): consider cutting this, with a homebrew tag migration
     * |  classic |  classic |     X | "+1 Weapon|DMG" -> "Longsword|PHB"
     * |      one |  classic |       | "+1 Weapon|DMG" -> "Longsword|XPHB"
     * |     null |      one |     X | "+1 Weapon|XDMG" -> "Pitchfork|ToB3-Lairs"
     * |  classic |      one |       | "+1 Weapon|XDMG" -> "Longsword|PHB"
     * |      one |      one |     X | "+1 Weapon|XDMG" -> "Longsword|XPHB"
     *
     * This aims to minimize spamming near-duplicates, while preserving as many '14 items as possible.
     */
    // @formatter:on
    boolean editionMatch(JsonNode baseItem, JsonNode genericVariant) {
        String baseItemEdition = Tools5eFields.edition.getTextOrNull(baseItem);
        String variantEdition = Tools5eFields.edition.getTextOrNull(genericVariant);
        if (baseItemEdition == null && variantEdition == null) {
            return true; // ok: null -> null
        }
        if (baseItemEdition != null) { // variantEdition may be null
            if (baseItemEdition.equalsIgnoreCase(variantEdition)) {
                return true; // ok: classic -> classic, one -> one
            }
            if ("classic".equalsIgnoreCase(baseItemEdition)) {
                return false; // nope: classic -> one or classic -> null
            }
            if ("one".equalsIgnoreCase(baseItemEdition)) {
                // ok: one -> null; nope: one -> classic
                return !"classic".equalsIgnoreCase(variantEdition);
            }
        }
        // ok: null -> classic, null -> one
        return true;
    }

    /**
     * render.js -- _createSpecificVariants_hasRequiredProperty
     */
    boolean hasRequiredProperty(JsonNode baseItem, JsonNode genericVariant) {
        JsonNode variantRequires = MagicItemField.requires.getFrom(genericVariant);
        if (variantRequires == null) {
            return true; // all is well if there are no required properties defined
        }
        if (!variantRequires.isArray()) {
            tui().errorf("Incorrect magic variant requirements: %s", genericVariant);
            return false;
        }
        // "requires": [
        //   { "weapon": true },
        //   { "type": "S" },
        //   { "net": true }
        // ],
        // return genericVariant.requires.some(req =>
        //      Renderer.item._createSpecificVariants_isRequiresExcludesMatch(baseItem, req, "every"));
        return streamOf(variantRequires).anyMatch(req -> {
            if (req != null && !req.isObject()) {
                tui().errorf("Incorrectly specified magic variant requirement in %s: %s",
                        TtrpgValue.indexKey.getFrom(genericVariant), req);
            }
            return matchesRequiresExcludes(baseItem, req, true);
        });
    }

    boolean hasExcludedProperty(JsonNode baseItem, JsonNode genericVariant) {
        JsonNode excludes = MagicItemField.excludes.getFrom(genericVariant);
        if (excludes != null && !excludes.isObject()) {
            tui().errorf("Incorrectly specified magic variant excludes in %s: %s",
                    TtrpgValue.indexKey.getFrom(genericVariant), excludes);
            return true;
        }
        // "excludes": {
        //   "net": true
        // },
        // return Renderer.item._createSpecificVariants_isRequiresExcludesMatch(baseItem, genericVariant.excludes, "some");
        return matchesRequiresExcludes(baseItem, excludes, false);
    }

    // _createSpecificVariants_isRequiresExcludesMatch
    private boolean matchesRequiresExcludes(JsonNode candidate, JsonNode reqsOrExcludes, boolean matchAll) {
        if (candidate == null || reqsOrExcludes == null) {
            return false;
        }

        var entries = reqsOrExcludes.properties().stream();

        return matchAll
                ? entries.allMatch(e -> testProperty(candidate, e.getKey(), e.getValue(), matchAll))
                : entries.anyMatch(e -> testProperty(candidate, e.getKey(), e.getValue(), matchAll));
    }

    private boolean testProperty(JsonNode candidate, String reqKey, JsonNode reqValue, boolean matchAll) {
        JsonNode candidateValue = candidate.get(reqKey);
        if (candidateValue == null || candidateValue.isNull()) {
            return reqValue == null || reqValue.isNull();
        }
        if (reqValue.isArray()) {
            return candidateValue.isArray()
                    ? streamOf(candidateValue).anyMatch(it -> arrayContains(reqValue, it))
                    : arrayContains(reqValue, candidateValue);
        }
        if (reqValue.isObject()) {
            // recursion: chase required custom properties (e.g.)
            return matchesRequiresExcludes(candidate.get(reqKey), reqValue, matchAll);
        }
        return candidateValue.isArray()
                ? arrayContains(candidateValue, reqValue)
                : reqValue.equals(candidateValue);
    }

    boolean arrayContains(JsonNode array, JsonNode value) {
        return streamOf(array).anyMatch((x) -> x.equals(value));
    }

    private JsonNode resolveEntryAttributes(
            JsonNode entriesNode,
            BiFunction<String, Boolean, String> applyProperties) {
        try {
            String entriesTemplate = mapper().writeValueAsString(entriesNode);
            entriesTemplate = replaceTokens(entriesTemplate, applyProperties);
            return mapper().readTree(entriesTemplate);
        } catch (JsonProcessingException e) {
            tui().errorf(e, "Unable to process entries from %s", entriesNode);
        }
        return entriesNode;
    }

    private String processText(String text, Function<String, String> tokenResolver) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // Handle {=thing|stuff} tokens
        return TEMPLATE.matcher(text).replaceAll((match) -> {
            String[] parts = match.group(1).trim().split("/");
            String key = parts[0];
            String value = tokenResolver.apply(key);
            if (parts.length > 1) {
                List<String> modifiers = Stream.of(FIXED_OP_ORDER.split(""))
                        .filter(x -> parts[1].contains(x))
                        .toList();
                for (String m : modifiers) {
                    value = switch (m) {
                        case "a" -> {
                            yield _LEADING_AN.contains(value.substring(0, 1).toLowerCase())
                                    ? "an"
                                    : "a";
                        }
                        case "l" -> value.toLowerCase();
                        case "t" -> toTitleCase(value);
                        case "u" -> value.toUpperCase();
                        default -> {
                            tui().errorf(
                                    "Unhandled modifier %s while processing %s (open an issue and include this message)", m,
                                    text);
                            yield text;
                        }
                    };
                }
            }
            return value;
        });
    }

    private BiFunction<String, Boolean, String> tokenResolver(final JsonNode valueSource) {
        return (s, b) -> {
            return processText(s, (key) -> {
                if (valueSource.has(key)) {
                    return valueSource.get(key).asText();
                }
                tui().errorf("Replacement for %s not found in %s", key, valueSource);
                return key;
            });
        };
    }

    private BiFunction<String, Boolean, String> tokenResolver(final JsonNode baseItem, final JsonNode inherits) {
        // _getInjectableProps (baseItem, inherits) {
        //     return {
        //         baseName: baseItem.name,
        //         dmgType: baseItem.dmgType ? Parser.dmgTypeToFull(baseItem.dmgType) : null,
        //         bonusAc: inherits.bonusAc,
        //         bonusWeapon: inherits.bonusWeapon,
        //         bonusWeaponAttack: inherits.bonusWeaponAttack,
        //         bonusWeaponDamage: inherits.bonusWeaponDamage,
        //         bonusWeaponCritDamage: inherits.bonusWeaponCritDamage,
        //         bonusSpellAttack: inherits.bonusSpellAttack,
        //         bonusSpellSaveDc: inherits.bonusSpellSaveDc,
        //         bonusSavingThrow: inherits.bonusSavingThrow,
        //     };
        // },
        return (s, b) -> {
            return processText(s, (key) -> {
                return switch (key) {
                    case "baseName" -> SourceField.name.getTextOrEmpty(baseItem);
                    case "dmgType" -> {
                        JsonNode dmgType = MagicItemField.dmgType.getFrom(baseItem);
                        if (dmgType != null) {
                            yield damageTypeToFull(dmgType.asText());
                        }
                        yield "";
                    }
                    case "bonusAc" -> MagicItemField.bonusAc.getTextOrEmpty(inherits);
                    case "bonusWeapon" -> MagicItemField.bonusWeapon.getTextOrEmpty(inherits);
                    case "bonusWeaponAttack" -> MagicItemField.bonusWeaponAttack.getTextOrEmpty(inherits);
                    case "bonusWeaponDamage" -> MagicItemField.bonusWeaponDamage.getTextOrEmpty(inherits);
                    case "bonusWeaponCritDamage" -> MagicItemField.bonusWeaponCritDamage.getTextOrEmpty(inherits);
                    case "bonusSpellAttack" -> MagicItemField.bonusSpellAttack.getTextOrEmpty(inherits);
                    case "bonusSpellSaveDc" -> MagicItemField.bonusSpellSaveDc.getTextOrEmpty(inherits);
                    case "bonusSavingThrow" -> MagicItemField.bonusSavingThrow.getTextOrEmpty(inherits);
                    default -> key;
                };
            });
        };
    }

    private JsonNode createSpecificVariant(JsonNode baseItem, JsonNode genericVariant) {
        JsonNode inherits = MagicItemField.inherits.getFrom(genericVariant);
        JsonNode specificVariant = copyNode(baseItem);

        // Remove base item flag
        TtrpgValue.indexBaseItem.removeFrom(specificVariant);

        // Magic variants apply their own SRD info; page info
        SourceAttributes.basicRules.removeFrom(specificVariant);
        SourceAttributes.freeRules2024.removeFrom(specificVariant);
        SourceAttributes.srd.removeFrom(specificVariant);
        SourceAttributes.srd52.removeFrom(specificVariant);
        SourceField.page.removeFrom(specificVariant);

        // Magic items do not inherit the value of the non-magical item
        ItemField.value.removeFrom(specificVariant);

        // Reset or remove fluff specifiers based on generic variant
        resetOrRemove(ItemField.hasFluff, genericVariant, specificVariant);
        resetOrRemove(ItemField.hasFluffImages, genericVariant, specificVariant);

        for (Entry<String, JsonNode> property : inherits.properties()) {
            switch (property.getKey()) {
                case "barding" -> {
                    MagicItemField.bardingType.setIn(specificVariant, ItemField.type.getFrom(baseItem));
                }
                case "entries" -> {
                    JsonNode entries = resolveEntryAttributes(property.getValue(),
                            tokenResolver(baseItem, inherits));
                    SourceField.entries.setIn(specificVariant, entries);
                }
                case "namePrefix" -> {
                    String name = SourceField.name.getTextOrEmpty(specificVariant);
                    SourceField.name.setIn(specificVariant, property.getValue().asText() + name);
                }
                case "nameSuffix" -> {
                    String name = SourceField.name.getTextOrEmpty(specificVariant);
                    SourceField.name.setIn(specificVariant, name + property.getValue().asText());
                }
                case "nameRemove" -> {
                    Pattern p = Pattern.compile(property.getValue().asText());
                    String name = SourceField.name.getTextOrEmpty(specificVariant);
                    SourceField.name.setIn(specificVariant, p.matcher(name).replaceAll(""));
                }
                case "propertyAdd" -> {
                    ArrayNode itemProperty = ItemField.property.ensureArrayIn(specificVariant);
                    index().copier.appendIfNotExistsArr(itemProperty, property.getValue());
                }
                case "propertyRemove" -> {
                    ArrayNode itemProperty = ItemField.property.ensureArrayIn(specificVariant);
                    index().copier.removeFromArr(itemProperty, property.getValue());
                }
                case "valueExpression", "weightExpression" -> {
                    String expr = EXPRESSION.matcher(property.getValue().asText()).replaceAll((match) -> {
                        JsonNode value = null;
                        String[] path = match.group(1).split("\\.");
                        if (path[0].equalsIgnoreCase("baseitem")) {
                            value = baseItem.get(match.group(1).substring(9));
                        } else if (path[0].equalsIgnoreCase("item")) {
                            value = specificVariant.get(match.group(1).substring(5));
                        } else {
                            value = specificVariant.get(match.group(1));
                        }
                        if (value == null) {
                            return "";
                        }
                        return value.asText();
                    });
                    if (!expr.isBlank()) {
                        try {
                            Expression expression = new Expression(expr);
                            EvaluationValue result = expression.evaluate();
                            if (property.getKey() == "valueExpression") {
                                IntNode value = IntNode.valueOf(result.getNumberValue().intValue());
                                ItemField.value.setIn(specificVariant, value);
                            } else {
                                DoubleNode value = DoubleNode.valueOf(result.getNumberValue().doubleValue());
                                ItemField.weight.setIn(specificVariant, value);
                            }
                        } catch (EvaluationException | ParseException e) {
                            tui().errorf(e, "Unable to parse %s: %s", property.getKey(), property.getValue());
                        }
                    }
                }
                case "conditionImmune" -> {
                    ArrayNode condImmune = MagicItemField.conditionImmune.ensureArrayIn(specificVariant);
                    index().copier.appendIfNotExistsArr(condImmune, property.getValue());
                }
                case "vulnerable", "resist", "immune" -> {
                    // TODO
                    /* no-op */ }
                default -> {
                    ((ObjectNode) specificVariant).set(property.getKey(), copyNode(property.getValue()));
                }
            }
        }
        // TODO:
        // Renderer.item._createSpecificVariants_mergeVulnerableResistImmune({specificVariant, inherits});

        return specificVariant;
    }

    private void resetOrRemove(JsonNodeReader field, JsonNode source, JsonNode target) {
        JsonNode value = field.getFrom(source);
        if (value == null || value.isNull()) {
            field.removeFrom(target);
        } else {
            field.setIn(target, value);
        }
    }

    enum MagicItemField implements JsonNodeReader {
        armor,
        barding,
        bardingType,
        conditionImmune,
        customProperties,
        entries,
        excludes,
        immune,
        inherits,
        namePrefix,
        nameRemove,
        nameSuffix,
        packContents, // not specific variant
        propertyAdd,
        propertyRemove,
        rarity,
        requires,
        resist,
        valueExpression,
        vulnerable,
        weightExpression,
        bonusAc,
        dmgType,
        damageType,
        bonusWeapon,
        bonusWeaponAttack,
        bonusWeaponDamage,
        bonusWeaponCritDamage,
        bonusSpellAttack,
        bonusSpellSaveDc,
        bonusSavingThrow,
    }

    @Override
    public Tools5eIndex index() {
        return Tools5eIndex.getInstance();
    }

    @Override
    public Tools5eSources getSources() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSources'");
    }
}
