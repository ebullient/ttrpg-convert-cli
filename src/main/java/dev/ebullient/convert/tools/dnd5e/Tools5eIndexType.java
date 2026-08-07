package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.valueOrDefault;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;

public enum Tools5eIndexType implements IndexType, JsonNodeReader {
    action,
    adventure,
    adventureData,
    background,
    backgroundFluff,
    book,
    bookData,
    boon,
    card,
    charoption,
    charoptionFluff,
    citation,
    classtype("class"),
    classFluff,
    classfeature,
    condition,
    conditionFluff,
    cult,
    disease,
    deity,
    deck,
    facility("bastion"),
    facilityFluff,
    feat,
    featFluff,
    hazard,
    hazardFluff,
    item,
    itemEntry,
    itemFluff,
    itemGroup,
    itemMastery,
    itemProperty,
    itemType,
    itemTypeAdditionalEntries,
    language,
    languageFluff,
    legendaryGroup,
    magicvariant,
    monster,
    monsterFluff,
    monsterfeatures,
    monsterTemplate,
    object,
    objectFluff,
    optfeature,
    optionalFeatureTypes, // homebrew
    optionalfeatureFluff,
    psionic,
    psionicTypes, // homebrew
    race,
    raceFeature,
    raceFluff,
    reward,
    rewardFluff,
    sense,
    skill,
    spell,
    spellFluff,
    spellSchool, // homebrew
    status,
    subclass,
    subclassFeature,
    subclassFluff,
    subrace("race"),
    table,
    tableGroup,
    trap,
    trapFluff,
    variantrule,
    vehicle,
    vehicleFluff,
    vehicleUpgrade,

    note, // qute data type
    reference, // made up
    syntheticGroup, // qute data type
    spellIndex, // made up
    ;

    final String templateName;

    Tools5eIndexType() {
        this.templateName = this.name();
    }

    Tools5eIndexType(String templateName) {
        this.templateName = templateName;
    }

    public String templateName() {
        return templateName;
    }

    public static Tools5eIndexType fromText(String name) {
        if ("creature".equalsIgnoreCase(name)) {
            return monster;
        }
        if ("creatureFluff".equalsIgnoreCase(name)) {
            return monsterFluff;
        }
        if ("optionalfeature".equalsIgnoreCase(name)) {
            return optfeature;
        }
        if ("legroup".equalsIgnoreCase(name)) {
            return legendaryGroup;
        }
        return Stream.of(values())
                .filter(x -> x.templateName.equalsIgnoreCase(name) || x.name().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public static Tools5eIndexType getTypeFromKey(String key) {
        if (!isPresent(key)) {
            return null;
        }
        String typeKey = key.substring(0, key.indexOf("|"));
        return fromText(typeKey);
    }

    public static Tools5eIndexType getTypeFromNode(JsonNode node) {
        String typeKey = TtrpgValue.indexInputType.getTextOrEmpty(node);
        return fromText(typeKey);
    }

    @Override
    public String createKey(JsonNode x) {
        if (this == book || this == adventure || this == bookData || this == adventureData) {
            String id = SourceField.id.getTextOrEmpty(x);
            return new DocumentKeyData(this, this.name().replace("Data", ""), id).toKey();
        } else if (this == itemTypeAdditionalEntries) {
            return createKey(
                    IndexFields.appliesTo.getTextOrEmpty(x),
                    SourceField.source.getTextOrEmpty(x));
        }

        String name = SourceField.name.getTextOrEmpty(x).trim();
        String source = SourceField.source.getTextOrEmpty(x).trim();

        // With introduction of XPHB, etc., we are going to be explicit about sources
        // links will be adjusted to add assumed sources
        return switch (this) {
            case classfeature -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "phb");
                yield new ClassFeatureKeyData(name,
                        IndexFields.className.getTextOrEmpty(x), classSource,
                        IndexFields.level.getTextOrEmpty(x), source)
                        .toKey();
            }
            case card -> {
                String set = IndexFields.set.getTextOrThrow(x).trim();
                yield new CardKeyData(name, set, source).toKey();
            }
            case deity -> {
                yield new DeityKeyData(name, IndexFields.pantheon.getTextOrEmpty(x).trim(), source).toKey();
            }
            case itemType, itemProperty -> {
                source = SourceField.source.getTextOrDefault(x, "phb");
                String abbreviation = IndexFields.abbreviation.getTextOrDefault(x, name).trim();
                yield new AbbreviationKeyData(this, abbreviation, source).toKey();
            }
            case reference -> {
                if (!isPresent(source)) {
                    source = Tools5eSources.has2024Content()
                            ? "XPHB"
                            : "PHB";
                }
                yield createKey(name, source);
            }
            case subclass -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "phb");
                String scSource = SourceField.source.getTextOrDefault(x, classSource);
                yield new SubclassKeyData(name,
                        IndexFields.className.getTextOrEmpty(x).trim(),
                        classSource, scSource)
                        .toKey();
            }
            case subclassFeature -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "phb");
                String scSource = IndexFields.subclassSource.getTextOrDefault(x, "phb");
                yield new SubclassFeatureKeyData(name,
                        IndexFields.className.getTextOrEmpty(x).trim(), classSource,
                        IndexFields.subclassShortName.getTextOrEmpty(x).trim(), scSource,
                        IndexFields.level.getTextOrEmpty(x), source)
                        .toKey();
            }
            case subrace -> {
                String raceSource = IndexFields.raceSource.getTextOrDefault(x, "phb");
                yield new SubraceKeyData(name,
                        IndexFields.raceName.getTextOrEmpty(x).trim(),
                        raceSource, source)
                        .toKey();
            }
            default -> createKey(name, source);
        };
    }

    public String createKey(String name, String source) {
        if (source == null) {
            return String.format("%s|%s", this.name(), name).toLowerCase();
        }
        return switch (this) {
            case adventure,
                    adventureData,
                    book,
                    bookData ->
                String.format("%s|%s-%s", this.name(), name, source).toLowerCase();
            default ->
                String.format("%s|%s|%s", this.name(), name, source).toLowerCase();
        };
    }

    public String fromTagReference(String crossRef) {
        if (crossRef == null || crossRef.isEmpty()) {
            return null;
        }
        String[] parts = crossRef.trim().split("\s?\\|\\s?");
        return switch (this) {
            case card -> {
                yield CardKeyData.fromRefTag(crossRef).toKey();
            }
            case classfeature -> {
                ClassFeatureKeyData keyData = ClassFeatureKeyData.fromRefTag(crossRef);
                yield keyData == null ? null : keyData.toKey();
            }
            case classtype -> {
                // A {@class} tag can reference either a class or a subclass.
                // {@class fighter|phb|optional link text added with another pipe}
                // {@class Fighter|phb|Samurai|Samurai|xge}
                // {@subclass} tags have a different structure
                if (parts.length < 5) {
                    yield "%s|%s|%s".formatted(
                            this.name(),
                            parts[0].trim(),
                            valueOrDefault(parts, 1, defaultSourceString()))
                            .toLowerCase();
                }
                yield new SubclassKeyData(
                        valueOrDefault(parts, 3, null),
                        parts[0],
                        valueOrDefault(parts, 1, defaultSourceString()),
                        valueOrDefault(parts, 4, defaultSourceString()))
                        .toKey();
            }
            case deity -> {
                yield DeityKeyData.fromRefTag(crossRef).toKey();
            }
            case itemProperty -> {
                yield ItemProperty.refTagToKey(crossRef).toKey();
            }
            case itemType -> {
                yield ItemType.refTagToKey(crossRef).toKey();
            }
            case subclass -> {
                SubclassKeyData keyData = SubclassKeyData.fromRefTag(crossRef);
                yield keyData == null ? null : keyData.toKey();
            }
            case subclassFeature -> {
                SubclassFeatureKeyData keyData = SubclassFeatureKeyData.fromRefTag(crossRef);
                yield keyData == null ? null : keyData.toKey();
            }
            default -> {
                yield SimpleKeyData.fromRefTag(this, crossRef).toKey();
            }
        };
    }

    public String toTagReference(JsonNode entry) {
        String linkText = Tools5eLinkifier.instance().decoratedName(this, entry);
        String name = SourceField.name.getTextOrEmpty(entry);
        String source = SourceField.source.getTextOrEmpty(entry);

        return switch (this) {
            // {@card Donjon|Deck of Several Things|LLK}
            case card -> new CardKeyData(name, IndexFields.set.getTextOrEmpty(entry), source)
                    .toRefTag(entry, linkText);
            // {@subclass Artillerist|Artificer|TCE|TCE}
            case subclass -> new SubclassKeyData(
                    name,
                    IndexFields.className.getTextOrEmpty(entry),
                    IndexFields.classSource.getTextOrEmpty(entry),
                    source)
                    .toRefTag(entry, linkText);
            // {@subclassFeature Blessed Strikes|Cleric|PHB|Twilight|TCE|8|TCE}
            case subclassFeature -> new SubclassFeatureKeyData(
                    name,
                    IndexFields.className.getTextOrEmpty(entry), IndexFields.classSource.getTextOrEmpty(entry),
                    IndexFields.subclassShortName.getTextOrEmpty(entry), IndexFields.subclassSource.getTextOrEmpty(entry),
                    IndexFields.level.getTextOrEmpty(entry), source)
                    .toRefTag(entry, linkText);
            // {@itemType abv|source|linkText}
            case itemProperty, itemType -> new AbbreviationKeyData(this,
                    IndexFields.abbreviation.getTextOrEmpty(entry), source)
                    .toRefTag(entry, linkText);
            // {@feat name|source|linkText}
            default -> new SimpleKeyData(this, name, source).toRefTag(entry, linkText);
        };
    }

    public String linkify(JsonSource convert, JsonNode entry) {
        String reference = toTagReference(entry);
        return convert.linkify(this, reference);
    }

    public boolean multiNode() {
        return switch (this) {
            case action,
                    condition,
                    disease,
                    itemType,
                    itemProperty,
                    itemMastery,
                    sense,
                    skill,
                    spellIndex,
                    status,
                    syntheticGroup ->
                true;
            default -> false;
        };
    }

    public boolean writeFile() {
        return switch (this) {
            case background,
                    classtype,
                    deck,
                    deity,
                    facility,
                    feat,
                    hazard,
                    item,
                    itemGroup,
                    monster,
                    object,
                    optfeature,
                    psionic,
                    race,
                    subrace,
                    reward,
                    spell,
                    trap,
                    vehicle ->
                true;
            default -> false;
        };
    }

    public boolean useQuteNote() {
        return switch (this) {
            case action,
                    adventureData,
                    bookData,
                    condition,
                    disease,
                    itemType,
                    itemProperty,
                    itemMastery,
                    legendaryGroup,
                    optionalFeatureTypes,
                    sense,
                    skill,
                    spellIndex,
                    status,
                    table,
                    tableGroup,
                    variantrule ->
                true; // QuteNote-based
            default -> false; // QuteBase
        };
    }

    public boolean useCompendiumBase() {
        return switch (this) {
            case action,
                    condition,
                    disease,
                    itemProperty,
                    itemType,
                    itemMastery,
                    sense,
                    skill,
                    status,
                    variantrule ->
                false; // use rules
            default -> true; // use compendium
        };
    }

    // render.js -- Tag*
    public String defaultSourceString() {
        return switch (this) {
            case card,
                    deck,
                    disease,
                    hazard,
                    item,
                    itemGroup,
                    magicvariant,
                    object,
                    reward,
                    table,
                    tableGroup,
                    trap,
                    variantrule ->
                "DMG";
            case legendaryGroup,
                    monster,
                    monsterfeatures ->
                "MM";
            case boon, cult -> "MTF";
            case charoption -> "MOT";
            case facility -> "XDMG";
            case itemMastery -> "XPHB";
            case itemTypeAdditionalEntries -> "XGE";
            case psionic -> "UATheMysticClass";
            case vehicle, vehicleUpgrade -> "GoS";
            // ---
            case syntheticGroup -> null;
            case reference ->
                Tools5eSources.has2024Content()
                        ? "XPHB"
                        : "PHB";
            default -> "PHB";
        };
    }

    public String defaultOutputSource() {
        return switch (this) {
            case classtype, classfeature, subclass, subclassFeature ->
                TtrpgConfig.getDefaultOutputSource(classtype);
            case card, deck ->
                TtrpgConfig.getDefaultOutputSource(deck);
            case legendaryGroup, monster, monsterfeatures ->
                TtrpgConfig.getDefaultOutputSource(monster);
            case item, itemGroup, magicvariant ->
                TtrpgConfig.getDefaultOutputSource(item);
            case object ->
                TtrpgConfig.getDefaultOutputSource(object);
            case race, subrace ->
                TtrpgConfig.getDefaultOutputSource(race);
            case table, tableGroup ->
                TtrpgConfig.getDefaultOutputSource(table);
            case trap, hazard ->
                TtrpgConfig.getDefaultOutputSource(trap);
            case vehicle, vehicleUpgrade ->
                TtrpgConfig.getDefaultOutputSource(vehicle);
            default -> TtrpgConfig.getDefaultOutputSource(this);
        };
    }

    boolean hasVariants() {
        return switch (this) {
            case magicvariant, monster -> true;
            default -> false;
        };
    }

    boolean isFluffType() {
        return switch (this) {
            case backgroundFluff,
                    classFluff,
                    conditionFluff,
                    facilityFluff,
                    featFluff,
                    hazardFluff,
                    itemFluff,
                    languageFluff,
                    monsterFluff,
                    objectFluff,
                    optionalfeatureFluff,
                    raceFluff,
                    rewardFluff,
                    subclassFluff,
                    trapFluff,
                    vehicleFluff ->
                true;
            default -> false;
        };
    }

    boolean isDependentType() {
        // These types are not directly filtered.
        // Special rules are applied after the parent item is filtered
        return switch (this) {
            case card,
                    classfeature,
                    optionalFeatureTypes,
                    subclass,
                    subclassFeature ->
                true;
            default -> false;
        };
    }

    boolean isOutputType() {
        return useQuteNote() || writeFile();
    }

    enum IndexFields implements JsonNodeReader {
        abbreviation,
        alias,
        appliesTo,
        className,
        classSource,
        deck,
        featureType,
        level,
        pantheon,
        raceName,
        raceSource,
        set,
        subclassShortName,
        subclassSource,
    }

    public void withArrayFrom(JsonNode node, BiConsumer<Tools5eIndexType, JsonNode> callback) {
        if (node.has(this.nodeName())) {
            node.withArray(this.nodeName()).forEach(x -> callback.accept(this, x));
        }
    }

    public void withArrayFrom(JsonNode node, String field, BiConsumer<Tools5eIndexType, JsonNode> callback) {
        if (node.has(field)) {
            node.withArray(field).forEach(x -> callback.accept(this, x));
        }
    }

    boolean isKey(String crossRef) {
        return crossRef != null && crossRef.startsWith(name());
    }
}
