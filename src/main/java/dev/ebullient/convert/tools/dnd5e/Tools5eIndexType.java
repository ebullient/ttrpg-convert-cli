package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.valueOrDefault;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
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
        return switch (this) {
            case classfeature -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "phb");
                yield "%s|%s|%s|%s|%s|%s".formatted(
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
                yield "%s|%s|%s|%s".formatted(
                        this.name(),
                        name,
                        set,
                        source)
                        .toLowerCase();
            }
            case deity -> {
                yield "%s|%s|%s|%s".formatted(
                        this.name(),
                        name,
                        IndexFields.pantheon.getTextOrEmpty(x).trim(),
                        source)
                        .toLowerCase();
            }
            case itemType, itemProperty -> {
                source = SourceField.source.getTextOrDefault(x, "phb");
                String abbreviation = IndexFields.abbreviation.getTextOrDefault(x, name).trim();
                yield "%s|%s|%s".formatted(
                        this.name(),
                        abbreviation,
                        source)
                        .toLowerCase();
            }
            case itemEntry -> {
                yield "%s|%s|%s".formatted(
                        this.name(),
                        name,
                        source)
                        .toLowerCase();
            }
            case optfeature -> {
                yield "%s|%s|%s".formatted(
                        this.name(),
                        name,
                        source)
                        .toLowerCase();
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
                // subclass|subclassName|className|classSource|subclassSource
                yield "%s|%s|%s|%s|%s".formatted(
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
                yield "%s|%s|%s|%s|%s|%s|%s|%s".formatted(
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
                yield "%s|%s|%s|%s|%s".formatted(
                        this.name(),
                        name,
                        IndexFields.raceName.getTextOrEmpty(x).trim(),
                        raceSource,
                        source)
                        .toLowerCase();
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
                // 0    name,
                // 1    set,
                // 2    source
                yield String.format("%s|%s|%s|%s",
                        this.name(),
                        parts[0].trim(),
                        parts[1].trim(),
                        valueOrDefault(parts, 2, defaultSourceString()))
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
                String classSource = valueOrDefault(parts, 2, Tools5eIndexType.classtype.defaultSourceString());
                String featureSource = valueOrDefault(parts, 4, classSource);
                yield "%s|%s|%s|%s|%s|%s".formatted(this.name(),
                        parts[0].trim(),
                        parts[1].trim(),
                        classSource,
                        parts[3].trim(),
                        featureSource)
                        .toLowerCase();
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
                yield getSubclassKey(
                        parts[0],
                        valueOrDefault(parts, 1, defaultSourceString()),
                        valueOrDefault(parts, 3, null),
                        valueOrDefault(parts, 4, defaultSourceString()));
            }
            case deity -> {
                yield "%s|%s|%s|%s".formatted(
                        this.name(),
                        parts[0],
                        valueOrDefault(parts, 1, "Forotten Realms"),
                        valueOrDefault(parts, 2, defaultSourceString()))
                        .toLowerCase();
            }
            case itemProperty -> {
                yield ItemProperty.refTagToKey(crossRef);
            }
            case itemType -> {
                yield ItemType.refTagToKey(crossRef);
            }
            case subclass -> {
                // Homebrew and reprint tags
                // {@subclass Artillerist|Artificer|TCE|TCE}
                // 0    subclassShortName,
                // 1    IndexFields.className.getTextOrEmpty(x),
                // 2    classSource || "phb",
                // 3    subClassSource || "phb"
                if (parts.length < 2) {
                    Tui.instance().errorf("Badly formed Subclass key (not enough segments): %s", crossRef);
                    yield null;
                }
                String scName = parts[0];
                String className = parts[1];
                String classSource = valueOrDefault(parts, 2, "phb");
                String subClassSource = valueOrDefault(parts, 3, "phb");

                yield getSubclassKey(
                        className, classSource,
                        scName, subClassSource);
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
                String classSource = valueOrDefault(parts, 2, "phb");
                String scSource = valueOrDefault(parts, 4, "phb");
                String featureSource = valueOrDefault(parts, 6, scSource);

                yield "%s|%s|%s|%s|%s|%s|%s|%s".formatted(
                        Tools5eIndexType.subclassFeature,
                        parts[0],
                        parts[1].trim(),
                        classSource,
                        parts[3].trim(),
                        scSource,
                        parts[5].trim(),
                        featureSource)
                        .toLowerCase();
            }
            default -> {
                // 0    name,
                // 1    source
                yield createKey(parts[0],
                        valueOrDefault(parts, 1, defaultSourceString()));
            }
        };
    }

    public String toTagReference(JsonNode entry) {
        String linkText = Tools5eLinkifier.instance().decoratedName(this, entry);
        String name = SourceField.name.getTextOrEmpty(entry);
        String source = SourceField.source.getTextOrEmpty(entry);

        return switch (this) {
            // {@card Donjon|Deck of Several Things|LLK}
            case card -> "%s|%s|%s".formatted(
                    name,
                    IndexFields.deck.getTextOrEmpty(entry),
                    source);
            // {@subclass Artillerist|Artificer|TCE|TCE}
            case subclass -> "%s|%s|%s|%s|%s".formatted(
                    name,
                    IndexFields.className.getTextOrEmpty(entry),
                    IndexFields.classSource.getTextOrEmpty(entry),
                    source,
                    linkText);
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

    public static String getSubclassKey(String className, String classSource, String subclassName, String subclassSource) {
        classSource = valueOrDefault(classSource, Tools5eIndexType.classtype.defaultSourceString());
        subclassSource = valueOrDefault(subclassSource, Tools5eIndexType.subclass.defaultSourceString());
        return "%s|%s|%s|%s|%s".formatted(
                Tools5eIndexType.subclass,
                subclassName,
                className,
                classSource,
                subclassSource)
                .toLowerCase();
    }

    public String fromChildKey(String key) {
        if (!isPresent(key)) {
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
