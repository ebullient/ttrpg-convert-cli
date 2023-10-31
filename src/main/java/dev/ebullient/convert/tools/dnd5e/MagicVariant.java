package dev.ebullient.convert.tools.dnd5e;

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
import dev.ebullient.convert.tools.dnd5e.Json2QuteItem.ItemFields;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.Tuple;

public class MagicVariant implements JsonSource {

    static final List<String> IGNORE = List.of("entries", "rarity", "namePrefix", "nameSuffix");
    static final Pattern EXPRESSION = Pattern.compile("\\[\\[([^\\]]+)]]");
    static final Pattern TEMPLATE = Pattern.compile("\\{=([^}]+)}");

    // Process operations in this order always (see Renderer.applyProperties._OP_ORDER)
    static final String FIXED_OP_ORDER = "ltua";
    static final List<String> _LEADING_AN = List.of("a", "e", "i", "o", "u");

    static final MagicVariant INSTANCE = new MagicVariant();

    /** Update generic variant item with inherited attributes (minimally source, required for key) */
    public void populateGenericVariant(final JsonNode variant) {
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
    public List<Tuple> findSpecificVariants(Tools5eIndex index, Tools5eIndexType type,
            String key, JsonNode genericVariant, JsonSourceCopier copier) {
        // const specificVariants = Renderer.item._createSpecificVariants(baseItems, genericVariants);
        // const outSpecificVariants = Renderer.item._enhanceItems(specificVariants);
        List<Tuple> variants = new ArrayList<>();
        genericVariant = copyNode(genericVariant);
        String gvKey = Tools5eIndexType.item.createKey(genericVariant);
        // Generic variants with (*) in the name have a single specific variant.
        // Those will be replaced (See below)
        if (!key.contains(" (*)")) {
            // Add generic variant to the list of variants as a regular item
            // (will have variants field, see)
            TtrpgValue.indexInputType.setIn(genericVariant, Tools5eIndexType.item.name());
            variants.add(new Tuple(gvKey, genericVariant));
            index.addAlias(key, gvKey);
        }

        // Create specific variants
        List<JsonNode> baseItems = index.originNodesMatching(x -> TtrpgValue.indexBaseItem.booleanOrDefault(x, false));
        for (JsonNode baseItem : baseItems) {
            if (MagicItemField.packContents.existsIn(baseItem)
                    || !hasRequiredProperty(genericVariant, baseItem)
                    || hasExcludedProperty(genericVariant, baseItem)) {
                continue;
            }
            JsonNode specficVariant = createSpecificVariant(genericVariant, baseItem);
            if (specficVariant != null) {
                String newKey = Tools5eIndexType.item.createKey(specficVariant);
                TtrpgValue.indexInputType.setIn(specficVariant, Tools5eIndexType.item.name());
                TtrpgValue.indexKey.setIn(specficVariant, newKey);
                Tools5eSources.constructSources(specficVariant);
                if (key.contains(" (*)")) {
                    // specific variant will replace single generic variant (Shield) as a regular item
                    variants.add(new Tuple(newKey, specficVariant));
                    if (key.replace(" (*)", "").replace("magicvariant", "item").equals(newKey)) {
                        index.addAlias(key, newKey);
                    }
                    if (gvKey.replace(" (*)", "").equals(newKey)) {
                        index.addAlias(gvKey, newKey);
                    }
                } else {
                    // add variant to list of variants for this generic variant
                    // magic variant remains in index as a magic variant
                    ItemFields._variants.arrayFrom(genericVariant).add(specficVariant);
                    index.addAlias(newKey, gvKey);
                }
            }
        }

        return variants;
    }

    boolean hasRequiredProperty(JsonNode genericVariant, JsonNode baseItem) {
        JsonNode variantRequires = MagicItemField.requires.getFrom(genericVariant);
        if (variantRequires == null) {
            return true; // all is well if there are no required properties defined
        }
        if (!variantRequires.isArray()) {
            tui().errorf("Incorrectly specified magic variant requirements", genericVariant);
            return false;
        }
        // "requires": [
        //   { "weapon": true },
        //   { "type": "S" },
        //   { "net": true }
        // ],
        // return genericVariant.requires.some(req => Renderer.item._createSpecificVariants_isRequiresExcludesMatch(baseItem, req, "every"));
        return streamOf(variantRequires).anyMatch((r) -> {
            return streamOfFieldNames(r).allMatch((name) -> testProperty(baseItem, name, r.get(name)));
        });
    }

    boolean hasExcludedProperty(JsonNode genericVariant, JsonNode baseItem) {
        JsonNode excludes = MagicItemField.excludes.getFrom(genericVariant);
        if (excludes == null) {
            // no excluded properties
            return false;
        }
        if (!excludes.isObject()) {
            tui().errorf("Incorrectly specified magic variant requirements", genericVariant);
            return true;
        }
        // "excludes": {
        //   "net": true
        // },
        // bail the first time you find an excluded property
        return streamOfFieldNames(excludes).anyMatch((name) -> testProperty(baseItem, name, excludes.get(name)));
    }

    boolean testProperty(JsonNode baseItem, String reqKey, JsonNode reqValue) {
        JsonNode customProperties = MagicItemField.customProperties.getFrom(baseItem);
        JsonNode candidate = getProperty(baseItem, customProperties, reqKey);
        if (candidate == null || candidate.isNull()) {
            return false;
        }
        if (reqValue.isArray()) {
            return candidate.isArray()
                    ? streamOf(candidate).anyMatch((x) -> arrayContains(reqValue, x))
                    : arrayContains(reqValue, candidate);
        }
        if (reqValue.isObject()) {
            tui().errorf(
                    "Unsupported comparison for required property \"%s\"; Raise an issue containing this message. We need to look for %s in %s",
                    reqKey, reqValue, baseItem);
        }
        return candidate.isArray()
                ? arrayContains(candidate, reqValue)
                : reqValue.equals(candidate);
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
            return match.group(1);
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

    private JsonNode getProperty(JsonNode baseItem, JsonNode customProperties, String fieldName) {
        JsonNode value = baseItem.get(fieldName);
        if (value == null || value.isNull()) {
            if (customProperties != null) {
                value = customProperties.get(fieldName);
            }
        }
        return value;
    }

    private JsonNode createSpecificVariant(JsonNode genericVariant, JsonNode baseItem) {
        JsonNode specificVariant = copyNode(baseItem);
        TtrpgValue.indexBaseItem.removeFrom(specificVariant);

        // Magic variants apply their own SRD info; page info
        Tools5eFields.basicRules.removeFrom(specificVariant);
        Tools5eFields.srd.removeFrom(specificVariant);
        SourceField.page.removeFrom(specificVariant);

        // Magic items do not inherit the value of the non-magical item
        ItemFields.value.removeFrom(specificVariant);

        // Remove fluff specifiers
        ItemFields.hasFluff.removeFrom(specificVariant);
        ItemFields.hasFluffImages.removeFrom(specificVariant);

        JsonNode inherits = MagicItemField.inherits.getFrom(genericVariant);
        for (Entry<String, JsonNode> property : iterableFields(inherits)) {
            switch (property.getKey()) {
                case "barding" -> {
                    MagicItemField.bardingType.setIn(specificVariant, ItemFields.type.getFrom(baseItem));
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
                    ArrayNode itemProperty = ItemFields.property.arrayFrom(specificVariant);
                    index().copier.appendIfNotExistsArr(itemProperty, property.getValue());
                }
                case "propertyRemove" -> {
                    ArrayNode itemProperty = ItemFields.property.arrayFrom(specificVariant);
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
                                ItemFields.value.setIn(specificVariant, value);
                            } else {
                                DoubleNode value = DoubleNode.valueOf(result.getNumberValue().doubleValue());
                                ItemFields.weight.setIn(specificVariant, value);
                            }
                        } catch (EvaluationException | ParseException e) {
                            tui().errorf(e, "Unable to parse %s: %s", property.getKey(), property.getValue());
                        }
                    }
                }
                case "conditionImmune" -> {
                    ArrayNode condImmune = MagicItemField.conditionImmune.arrayFrom(specificVariant);
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
        return specificVariant;
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
