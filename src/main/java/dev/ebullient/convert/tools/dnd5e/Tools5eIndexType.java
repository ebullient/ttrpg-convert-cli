package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.valueOrDefault;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
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
    item,
    itemEntry,
    itemFluff,
    itemGroup,
    itemMastery,
    itemProperty,
    itemType,
    itemTypeAdditionalEntries,
    language,
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
        if (key == null || key.isEmpty()) {
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
            return String.format("%s|%s-%s",
                    this.name(),
                    this.name().replace("Data", ""),
                    id).toLowerCase();
        } else if (this == itemTypeAdditionalEntries) {
            return createKey(
                    IndexFields.appliesTo.getTextOrEmpty(x),
                    SourceField.source.getTextOrEmpty(x));
        }

        String name = SourceField.name.getTextOrEmpty(x).trim();
        String source = SourceField.source.getTextOrEmpty(x).trim();

        // With introduction of XPHB, etc., we are going to be explicit about sources
        // links will be adjusted to add assumed sources
        switch (this) {
            case classfeature -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "phb");
                return "%s|%s|%s|%s|%s|%s".formatted(
                        this.name(),
                        name,
                        IndexFields.className.getTextOrEmpty(x),
                        classSource,
                        IndexFields.level.getTextOrEmpty(x),
                        source)
                        .toLowerCase();
            }
            case card -> {
                String set = IndexFields.set.getTextOrThrow(x).trim();
                return "%s|%s|%s|%s".formatted(
                        this.name(),
                        name,
                        set,
                        source)
                        .toLowerCase();
            }
            case deity -> {
                return "%s|%s|%s|%s".formatted(
                        this.name(),
                        name,
                        IndexFields.pantheon.getTextOrEmpty(x).trim(),
                        source)
                        .toLowerCase();
            }
            case itemType, itemProperty -> {
                source = SourceField.source.getTextOrDefault(x, "phb");
                String abbreviation = IndexFields.abbreviation.getTextOrDefault(x, name).trim();
                return "%s|%s|%s".formatted(
                        this.name(),
                        abbreviation,
                        source)
                        .toLowerCase();
            }
            case itemEntry -> {
                return "%s|%s|%s".formatted(
                        this.name(),
                        name,
                        source)
                        .toLowerCase();
            }
            case optfeature -> {
                return "%s|%s|%s".formatted(
                        this.name(),
                        name,
                        source)
                        .toLowerCase();
            }
            case subclass -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "phb");
                String scSource = SourceField.source.getTextOrDefault(x, classSource);
                // subclass|subclassName|className|classSource|subclassSource
                return "%s|%s|%s|%s|%s".formatted(
                        this.name(),
                        name,
                        IndexFields.className.getTextOrEmpty(x).trim(),
                        classSource,
                        scSource)
                        .toLowerCase();
            }
            case subclassFeature -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "phb");
                String scSource = IndexFields.subclassSource.getTextOrDefault(x, "phb");
                // scFeature|className|classSource|subclassShortName|subclassSource|level|source
                return "%s|%s|%s|%s|%s|%s|%s|%s".formatted(
                        this.name(),
                        name,
                        IndexFields.className.getTextOrEmpty(x).trim(),
                        classSource,
                        IndexFields.subclassShortName.getTextOrEmpty(x).trim(),
                        scSource,
                        IndexFields.level.getTextOrEmpty(x),
                        source)
                        .toLowerCase();
            }
            case subrace -> {
                String raceSource = IndexFields.raceSource.getTextOrDefault(x, "phb");
                return "%s|%s|%s|%s|%s".formatted(
                        this.name(),
                        name,
                        IndexFields.raceName.getTextOrEmpty(x).trim(),
                        raceSource,
                        source)
                        .toLowerCase();
            }
            default -> {
                return createKey(name, source);
            }
        }
    }

    public String createKey(String name, String source) {
        if (source == null) {
            return String.format("%s|%s", this.name(), name).toLowerCase();
        }
        if (this == book || this == adventure || this == bookData || this == adventureData) {
            return String.format("%s|%s-%s", this.name(), name, source).toLowerCase();
        }

        return String.format("%s|%s|%s", this.name(), name, source).toLowerCase();
    }

    public String fromTagReference(String crossRef) {
        if (crossRef == null || crossRef.isEmpty()) {
            return null;
        }
        String[] parts = crossRef.trim().split("\s?\\|\\s?");
        return switch (this) {
            case card -> {
                // 0    name,
                // 1    set,
                // 2    source
                yield String.format("%s|%s|%s|%s",
                        this.name(),
                        parts[0].trim(),
                        parts[1].trim(),
                        parts.length > 2 ? parts[2] : defaultSourceString())
                        .toLowerCase();
            }
            case classfeature -> {
                // 0    name,
                // 1    IndexFields.className.getTextOrEmpty(x),
                // 2    classSource || "phb",
                // 3    IndexFields.level.getTextOrEmpty(x),
                // 4    source || classSource
                if (parts.length < 4) {
                    Tui.instance().errorf("Badly formed Class Feature key (not enough segments): %s", crossRef);
                    yield null;
                }
                String classSource = valueOrDefault(parts[2], "phb");
                String featureSource = parts.length > 4 ? parts[4] : classSource;
                yield getClassFeatureKey(
                        parts[0], featureSource,
                        parts[1], classSource,
                        parts[3]);
            }
            case itemMastery, itemProperty, itemType -> {
                // utils.js: itemType.unpackUid, itemProperty.unpackUid
                String source = parts.length > 1 ? parts[1] : defaultSourceString();
                yield "%s|%s|%s".formatted(this.name(), parts[0], source).toLowerCase();
            }
            case subclassFeature -> {
                // 0    name,
                // 1    IndexFields.className.getTextOrEmpty(x),
                // 2    classSource || "phb",
                // 3    IndexFields.subclassShortName.getTextOrEmpty(x),
                // 4    subClassSource || "phb",
                // 5    IndexFields.level.getTextOrEmpty(x),
                // 6    source || subClassSource
                if (parts.length < 6) {
                    Tui.instance().errorf("Badly formed Subclass Feature key (not enough segments): %s", crossRef);
                    yield null;
                }
                String classSource = valueOrDefault(parts[2], "phb");
                String subClassSource = valueOrDefault(parts[4], "phb");
                String featureSource = parts.length > 6 ? parts[6] : subClassSource;
                yield getSubclassFeatureKey(
                        parts[0], featureSource,
                        parts[1], classSource,
                        parts[3], subClassSource,
                        parts[5]);
            }
            default -> "%s|%s".formatted(this.name(), crossRef).toLowerCase();
        };
    }

    public String toTagReference(JsonNode entry) {
        String linkText = this.decoratedName(entry);
        String name = SourceField.name.getTextOrEmpty(entry);
        String source = SourceField.source.getTextOrEmpty(entry);

        return switch (this) {
            // {@card Donjon|Deck of Several Things|LLK}
            case card -> "%s|%s|%s".formatted(
                    name,
                    IndexFields.deck.getTextOrEmpty(entry),
                    source);
            // {@class Fighter|phb|Samurai|Samurai|xge}
            case subclass -> Tools5eIndexType.getSubclassTextReference(
                    IndexFields.className.getTextOrEmpty(entry),
                    IndexFields.classSource.getTextOrEmpty(entry),
                    name, source, linkText);
            // {@subclassFeature Blessed Strikes|Cleric|PHB|Twilight|TCE|8|TCE}
            case subclassFeature -> "%s|%s|%s|%s|%s|%s|%s|%s".formatted(
                    name,
                    IndexFields.className.getTextOrEmpty(entry),
                    IndexFields.classSource.getTextOrEmpty(entry),
                    IndexFields.subclassShortName.getTextOrEmpty(entry),
                    IndexFields.subclassSource.getTextOrEmpty(entry),
                    IndexFields.level.getTextOrEmpty(entry),
                    source,
                    linkText);
            // {@itemType abv|source|linkText}
            case itemProperty, itemType -> "%s|%s|%s".formatted(
                    IndexFields.abbreviation.getTextOrEmpty(entry),
                    source, linkText);
            // {@feat name|source|linkText}
            default -> "%s|%s|%s".formatted(name, source, linkText);
        };
    }

    public String linkify(JsonSource convert, JsonNode entry) {
        String reference = toTagReference(entry);
        return convert.linkify(this, reference);
    }

    public String decoratedName(JsonNode entry) {
        String name = SourceField.name.getTextOrEmpty(entry);
        switch (this) {
            case background -> {
                if (name.startsWith("Variant")) {
                    name = name.replace("Variant ", "") + " (Variant)";
                }
            }
            case race, subrace -> {
                name = name.replace("Variant; ", "");
            }
            default -> {
            }
        }
        return decoratedName(name, entry);
    }

    public String decoratedName(String name, JsonNode entry) {
        Tools5eSources sources = Tools5eSources.findOrTemporary(entry);
        if (sources.isPrimarySource("DMG")
                && !sources.type.defaultSourceString().equalsIgnoreCase("DMG")
                && !name.contains("(DMG)")) {
            return name + " (DMG)";
        }
        return name;
    }

    public static String getSubclassKey(String className, String classSource, String subclassName, String subclassSource) {
        if (classSource == null || classSource.isEmpty()) {
            // phb remains in the subclass text reference (match allowed sources)
            classSource = "phb";
        }
        return "%s|%s|%s|%s|%s".formatted(
                Tools5eIndexType.subclass,
                subclassName,
                className,
                classSource,
                subclassSource)
                .toLowerCase();
    }

    public static String getSubclassTextReference(String className, String classSource, String subclassName,
            String subclassSource, String text) {
        if (classSource == null || classSource.isEmpty()) {
            // phb remains in the subclass text reference (match allowed sources)
            classSource = "phb";
        }
        // {@class Fighter|phb|Samurai|Samurai|xge}
        return "%s|%s|%s|%s|%s".formatted(
                className,
                classSource,
                valueOrDefault(text, subclassName),
                subclassName,
                subclassSource);
    }

    public static String getClassFeatureKey(String name, String featureSource, String className, String classSource,
            String level) {
        return "%s|%s|%s|%s|%s|%s".formatted(
                Tools5eIndexType.classfeature,
                name,
                className,
                classSource,
                level,
                featureSource)
                .toLowerCase();
    }

    public static String getSubclassFeatureKey(String name, String featureSource, String className, String classSource,
            String scShortName, String scSource, String level) {
        return "%s|%s|%s|%s|%s|%s|%s|%s".formatted(
                Tools5eIndexType.subclassFeature,
                name,
                className,
                classSource,
                scShortName,
                scSource,
                level,
                featureSource)
                .toLowerCase();
    }

    public String fromChildKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return switch (this) {
            case deck, classtype, race -> {
                String[] parts = key.trim().split("\s?\\|\\s?");
                // card|cardName|deckName|source
                // classfeature|cfName|className|classSource|level|cfSource
                // subclass|scName|className|classSource|scSource
                // subclassfeature|scfName|className|classSource|subclassShortName|scSource|level|scfSource
                // subrace|subraceName|raceName|raceSource|subraceSource
                yield parts.length < 4 ? null : "%s|%s|%s".formatted(this, parts[2], parts[3]);
            }
            case subclass -> {
                String[] parts = key.trim().split("\s?\\|\\s?");
                // subclassfeature|scfName|className|classSource|subclassShortName|scSource|level|scfSource
                yield parts.length < 6 ? null : "%s|%s|%s|%s|%s".formatted(this, parts[4], parts[2], parts[3], parts[5]);
            }
            default -> null;
        };
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

    public String getRelativePath() {
        return switch (this) {
            case adventureData -> "adventures";
            case bookData -> "books";
            case card, deck -> "decks";
            case deity -> "deities";
            case facility -> "bastions";
            case item, itemGroup -> "items";
            case itemType -> "item-types";
            case itemMastery -> "item-mastery";
            case itemProperty -> "item-properties";
            case legendaryGroup -> "bestiary/legendary-group";
            case magicvariant -> "items";
            case monster -> "bestiary";
            case optfeature, optionalFeatureTypes -> "optional-features";
            case race, subrace -> "races";
            case subclass, classtype -> "classes";
            case table, tableGroup -> "tables";
            case trap, hazard -> "traps-hazards";
            case variantrule -> "variant-rules";
            default -> this.name() + 's';
        };
    }

    public String vaultRoot(Tools5eIndex index) {
        return useCompendiumBase()
                ? index.compendiumVaultRoot()
                : index.rulesVaultRoot();
    }

    // render.js -- Tag*
    public String defaultSourceString() {
        return switch (this) {
            case card,
                    deck,
                    disease,
                    hazard,
                    item,
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
            case syntheticGroup -> null;
            case vehicle, vehicleUpgrade -> "GoS";
            default -> "PHB";
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
                    facilityFluff,
                    classFluff,
                    conditionFluff,
                    featFluff,
                    itemFluff,
                    monsterFluff,
                    objectFluff,
                    optionalfeatureFluff,
                    raceFluff,
                    rewardFluff,
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
                    optfeature,
                    optionalFeatureTypes,
                    subclass,
                    subclassFeature,
                    subrace ->
                true;
            default -> false;
        };
    }

    boolean isOutputType() {
        return useQuteNote() || writeFile();
    }

    enum IndexFields implements JsonNodeReader {
        abbreviation,
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
}
