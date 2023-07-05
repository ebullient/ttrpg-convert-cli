package dev.ebullient.convert.tools.dnd5e;

import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.JsonSource.Fields;

public enum Tools5eIndexType implements IndexType, NodeReader {
    action,
    adventure,
    adventureData,
    artObjects,
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
    gems,
    hazard,
    item,
    itemEntry,
    itemFluff,
    itemType,
    itemTypeAdditionalEntries,
    itemProperty,
    legendaryGroup,
    magicItems,
    magicvariant,
    monster,
    monsterFluff,
    monsterfeatures,
    nametable,
    object,
    objectFluff,
    optionalfeature,
    psionic,
    race,
    raceFeature,
    raceFluff,
    reward,
    sense,
    skill,
    spell,
    spellFluff,
    status,
    subclass,
    subclassFeature,
    subrace("race"),
    table,
    trait,
    trap,
    variantrule,
    vehicle,
    vehicleFluff,
    vehicleUpgrade,

    note, // qute data type
    syntheticGroup, // qute data type
    ;

    String templateName;

    Tools5eIndexType() {
        this.templateName = this.name();
    }

    Tools5eIndexType(String templateName) {
        this.templateName = templateName;
    }

    public static final Pattern matchPattern = Pattern.compile("\\{@("
            + Stream.of(values())
                    .flatMap(x -> Stream.of(x.templateName, x.name()))
                    .distinct()
                    .collect(Collectors.joining("|"))
            + ") ([^{}]+?)}");

    public String templateName() {
        return templateName;
    }

    public static Tools5eIndexType fromText(String name) {
        return Stream.of(values())
                .filter(x -> x.templateName.equals(name) || x.name().equalsIgnoreCase(name))
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
            String id = Fields.id.getTextOrEmpty(x);
            return String.format("%s|%s-%s",
                    this.name(),
                    this.name().replace("Data", ""),
                    id).toLowerCase();
        } else if (this == itemTypeAdditionalEntries) {
            return createKey(
                    Fields.appliesTo.getTextOrEmpty(x),
                    Fields.source.getTextOrEmpty(x));
        }

        String name = IndexElement.name.getTextOrEmpty(x);
        String source = IndexElement.source.getTextOrEmpty(x);

        switch (this) {
            case classfeature: {
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
            case deity: {
                return String.format("%s|%s|%s|%s",
                        this.name(),
                        name,
                        IndexFields.pantheon.getTextOrEmpty(x),
                        source)
                        .toLowerCase();
            }
            case itemType:
            case itemProperty: {
                String abbreviation = IndexFields.abbreviation.getTextOrDefault(x, name);
                return String.format("%s|%s|%s",
                        this.name(),
                        abbreviation,
                        source)
                        .toLowerCase();
            }
            case itemEntry: {
                return String.format("%s|%s%s",
                        this.name(),
                        name,
                        "dmg".equalsIgnoreCase(source) ? "" : "|" + source)
                        .toLowerCase();
            }
            case optionalfeature: {
                return String.format("%s|%s%s",
                        this.name(),
                        name,
                        "phb".equalsIgnoreCase(source) ? "" : "|" + source)
                        .toLowerCase();
            }
            case subclass: {
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
            case subclassFeature: {
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
            case subrace: {
                return String.format("%s|%s|%s|%s",
                        this.name(),
                        name,
                        IndexFields.raceName.getTextOrEmpty(x),
                        IndexFields.raceSource.getTextOrEmpty(x))
                        .toLowerCase();
            }
            default:
                return createKey(name, source);
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
        return String.format("%s|%s|%s|%s|%s",
                Tools5eIndexType.subclass, subclassName, className, classSource,
                classSource.equals(subclassSource) ? "" : subclassSource).toLowerCase();
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
        switch (this) {
            case action:
            case artObjects:
            case condition:
            case disease:
            case gems:
            case itemType:
            case itemProperty:
            case magicItems:
            case sense:
            case skill:
            case status:
            case syntheticGroup:
                return true;
            default:
                return false;
        }
    }

    public boolean writeFile() {
        switch (this) {
            case background:
            case classtype:
            case deity:
            case feat:
            case item:
            case monster:
            case race:
            case spell:
                return true;
            default:
                return false;
        }
    }

    public boolean useQuteNote() {
        switch (this) {
            case action:
            case adventureData:
            case artObjects:
            case bookData:
            case condition:
            case disease:
            case gems:
            case itemType:
            case itemProperty:
            case magicItems:
            case nametable:
            case sense:
            case skill:
            case status:
            case table:
            case variantrule:
                return true; // QuteNote-based
            default:
                return false;
        }
    }

    public boolean useCompendiumBase() {
        switch (this) {
            case action:
            case condition:
            case disease:
            case itemProperty:
            case itemType:
            case sense:
            case skill:
            case status:
            case variantrule:
                return false; // use rules
            default:
                return true; // use compendium
        }
    }

    public String defaultSourceString() {
        switch (this) {
            case card:
            case deck:
            case disease:
            case hazard:
            case item:
            case magicvariant:
            case reward:
            case table:
            case trap:
            case variantrule:
                return "DMG";
            case legendaryGroup:
            case monster:
            case monsterfeatures:
                return "MM";
            case vehicle:
            case vehicleUpgrade:
                return "GoS";
            case boon:
            case cult:
                return "MTF";
            case psionic:
                return "UATheMysticClass";
            case charoption:
                return "MOT";
            case syntheticGroup:
                return null;
            case itemTypeAdditionalEntries:
                return "XGE";
            default:
                return "PHB";
        }
    }

    enum IndexFields implements NodeReader {
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
