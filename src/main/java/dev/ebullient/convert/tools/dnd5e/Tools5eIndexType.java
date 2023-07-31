package dev.ebullient.convert.tools.dnd5e;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.JsonSource.Tools5eFields;

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
    classtype("class"),
    classfeature,
    condition,
    conditionFluff,
    cult,
    disease,
    deity,
    deck,
    feat,
    featFluff,
    hazard,
    item,
    itemEntry,
    itemFluff,
    itemType,
    itemTypeAdditionalEntries,
    itemProperty,
    legendaryGroup,
    magicvariant,
    monster,
    monsterFluff,
    monsterfeatures,
    monsterTemplate,
    object,
    objectFluff,
    optionalfeature,
    optionalFeatureTypes, // homebrew
    psionic,
    psionicTypes, // homebrew
    race,
    raceFeature,
    raceFluff,
    reward,
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
    variantrule,
    vehicle,
    vehicleFluff,
    vehicleUpgrade,

    note, // qute data type
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
        if ("optfeature".equalsIgnoreCase(name)) {
            return optionalfeature;
        }
        return Stream.of(values())
                .filter(x -> x.templateName.equalsIgnoreCase(name) || x.name().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public static Tools5eIndexType getTypeFromKey(String key) {
        String typeKey = key.substring(0, key.indexOf("|"));
        return fromText(typeKey);
    }

    public static Tools5eIndexType getTypeFromNode(JsonNode node) {
        String typeKey = TtrpgValue.indexInputType.getFromNode(node);
        return fromText(typeKey);
    }

    public String createKey(JsonNode x) {
        if (this == book || this == adventure || this == bookData || this == adventureData) {
            String id = SourceField.id.getTextOrEmpty(x);
            return String.format("%s|%s-%s",
                    this.name(),
                    this.name().replace("Data", ""),
                    id).toLowerCase();
        } else if (this == itemTypeAdditionalEntries) {
            return createKey(
                    Tools5eFields.appliesTo.getTextOrEmpty(x),
                    SourceField.source.getTextOrEmpty(x));
        }

        String name = IndexElement.name.getTextOrEmpty(x);
        String source = IndexElement.source.getTextOrEmpty(x);

        switch (this) {
            case classfeature -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "phb");
                return String.format("%s|%s|%s|%s|%s%s",
                        this.name(),
                        name,
                        IndexFields.className.getTextOrEmpty(x),
                        "phb".equalsIgnoreCase(classSource) ? "" : classSource,
                        IndexFields.level.getTextOrEmpty(x),
                        source.equalsIgnoreCase(classSource) ? "" : "|" + source)
                        .toLowerCase();
            }
            case deity -> {
                return String.format("%s|%s|%s|%s",
                        this.name(),
                        name,
                        IndexFields.pantheon.getTextOrEmpty(x),
                        source)
                        .toLowerCase();
            }
            case itemType, itemProperty -> {
                String abbreviation = IndexFields.abbreviation.getTextOrDefault(x, name);
                return String.format("%s|%s|%s",
                        this.name(),
                        abbreviation,
                        source)
                        .toLowerCase();
            }
            case itemEntry -> {
                return String.format("%s|%s%s",
                        this.name(),
                        name,
                        "dmg".equalsIgnoreCase(source) ? "" : "|" + source)
                        .toLowerCase();
            }
            case optionalfeature -> {
                return String.format("%s|%s%s",
                        this.name(),
                        name,
                        "phb".equalsIgnoreCase(source) ? "" : "|" + source)
                        .toLowerCase();
            }
            case subclass -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "PHB");
                String scSource = IndexElement.source.getTextOrDefault(x, classSource);
                // subclass|subclassName|className|classSource|subclassSource
                return String.format("%s|%s|%s|%s|%s",
                        this.name(),
                        name,
                        IndexFields.className.getTextOrEmpty(x),
                        classSource,
                        scSource.equalsIgnoreCase(classSource) ? "" : scSource)
                        .toLowerCase();
            }
            case subclassFeature -> {
                String classSource = IndexFields.classSource.getTextOrDefault(x, "PHB");
                String scSource = IndexFields.subclassSource.getTextOrDefault(x, "PHB");
                return String.format("%s|%s|%s|%s|%s|%s|%s%s",
                        this.name(),
                        name,
                        IndexFields.className.getTextOrEmpty(x),
                        "phb".equalsIgnoreCase(classSource) ? "" : classSource,
                        IndexFields.subclassShortName.getTextOrEmpty(x),
                        "phb".equalsIgnoreCase(scSource) ? "" : scSource,
                        IndexFields.level.getTextOrEmpty(x),
                        source.equalsIgnoreCase(scSource) ? "" : "|" + source)
                        .toLowerCase();
            }
            case subrace -> {
                return String.format("%s|%s|%s|%s",
                        this.name(),
                        name,
                        IndexFields.raceName.getTextOrEmpty(x),
                        IndexFields.raceSource.getTextOrEmpty(x))
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
        if (this == optionalfeature) {
            // "optionalfeature|agonizing blast",
            // "optionalfeature|alchemical acid|uaartificer",
            return String.format("%s|%s%s",
                    Tools5eIndexType.optionalfeature,
                    name,
                    "phb".equalsIgnoreCase(source) ? "" : "|" + source)
                    .toLowerCase();
        }

        return String.format("%s|%s|%s", this.name(), name, source).toLowerCase();
    }

    public String fromRawKey(String crossRef) {
        if (this.equals(subclassFeature)) {
            String[] parts = crossRef.split("\\|");
            // 0    name,
            // 1    IndexFields.className.getTextOrEmpty(x),
            // 2    "phb".equalsIgnoreCase(classSource) ? "" : classSource,
            // 3    IndexFields.subclassShortName.getTextOrEmpty(x),
            // 4    "phb".equalsIgnoreCase(scSource) ? "" : scSource,
            // 5    IndexFields.level.getTextOrEmpty(x),
            // 6    source.equalsIgnoreCase(scSource) ? "" : "|" + source)
            String featureSource = parts.length > 6 ? parts[6] : parts[4];
            return getSubclassFeatureKey(parts[0], featureSource, parts[1], parts[2], parts[3], parts[4],
                    parts[5]);
        }
        if (this.equals(classfeature)) {
            String[] parts = crossRef.split("\\|");
            // 0    name,
            // 1    IndexFields.className.getTextOrEmpty(x),
            // 2    "phb".equalsIgnoreCase(classSource) ? "" : classSource,
            // 3    IndexFields.level.getTextOrEmpty(x),
            // 4    source.equalsIgnoreCase(classSource) ? "" : "|" + source)
            String featureSource = parts.length > 4 ? parts[4] : parts[2];
            return getClassFeatureKey(parts[0], featureSource, parts[1], parts[2], parts[3]);
        }

        return String.format("%s|%s", this.name(), crossRef).toLowerCase();
    }

    public static String getSubclassKey(String className, String classSource, String subclassName, String subclassSource) {
        if (classSource == null || classSource.isEmpty()) {
            // phb stays in the subclass text reference (match allowed sources)
            classSource = "phb";
        }
        return String.format("%s|%s|%s|%s|%s",
                Tools5eIndexType.subclass,
                subclassName,
                className,
                classSource,
                classSource.equalsIgnoreCase(subclassSource) ? "" : subclassSource)
                .toLowerCase();
    }

    public static String getSubclassTextReference(String className, String classSource, String subclassName,
            String subclassSource, String text) {
        if (classSource == null || classSource.isEmpty()) {
            // phb stays in the subclass text reference (match allowed sources)
            classSource = "phb";
        }
        // {@class Fighter|phb|Samurai|Samurai|xge}
        return String.format("%s|%s|%s|%s|%s",
                className,
                classSource,
                text == null ? subclassName : text,
                subclassName,
                classSource.equalsIgnoreCase(subclassSource) ? "" : subclassSource);
    }

    public static String getClassFeatureKey(String name, String featureSource, String className, String classSource,
            String level) {
        return String.format("%s|%s|%s|%s|%s%s",
                Tools5eIndexType.classfeature,
                name,
                className,
                "phb".equalsIgnoreCase(classSource) ? "" : classSource,
                level,
                featureSource.equalsIgnoreCase(classSource) ? "" : "|" + featureSource)
                .toLowerCase();
    }

    public static String getSubclassFeatureKey(String name, String featureSource, String className, String classSource,
            String scShortName, String scSource, String level) {
        return String.format("%s|%s|%s|%s|%s|%s|%s%s",
                Tools5eIndexType.subclassFeature,
                name,
                className,
                "phb".equalsIgnoreCase(classSource) ? "" : classSource,
                scShortName,
                "phb".equalsIgnoreCase(scSource) ? "" : scSource,
                level,
                featureSource.equalsIgnoreCase(scSource) ? "" : "|" + featureSource)
                .toLowerCase();
    }

    public boolean multiNode() {
        return switch (this) {
            case action,
                    condition,
                    disease,
                    itemType,
                    itemProperty,
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
                    deity,
                    feat,
                    hazard,
                    item,
                    monster,
                    optionalfeature,
                    optionalFeatureTypes,
                    race,
                    reward,
                    spell,
                    trap ->
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
                    sense,
                    skill,
                    status,
                    variantrule ->
                false; // use rules
            default -> true; // use compendium
        };
    }

    public String defaultSourceString() {
        return switch (this) {
            case card,
                    deck,
                    disease,
                    hazard,
                    item,
                    magicvariant,
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
            case vehicle, vehicleUpgrade -> "GoS";
            case boon, cult -> "MTF";
            case psionic -> "UATheMysticClass";
            case charoption -> "MOT";
            case syntheticGroup -> null;
            case itemTypeAdditionalEntries -> "XGE";
            default -> "PHB";
        };
    }

    enum IndexFields implements JsonNodeReader {
        abbreviation,
        className,
        classSource,
        featureType,
        level,
        pantheon,
        raceName,
        raceSource,
        subclassShortName,
        subclassSource
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
