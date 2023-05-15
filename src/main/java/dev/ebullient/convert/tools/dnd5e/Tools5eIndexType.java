package dev.ebullient.convert.tools.dnd5e;

import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.NodeReader;

public enum Tools5eIndexType implements IndexType, NodeReader {
    background,
    backgroundfluff,
    classtype("class"),
    classfeature,
    deity,
    feat,
    item,
    itementry,
    itemfluff,
    itemproperty,
    legendarygroup,
    magicvariant,
    monster,
    monsterfluff,
    race,
    racefluff,
    spell,
    spellfluff,
    subclass,
    subclassfeature,
    subrace("race"),
    optionalfeature,
    table,
    trait,
    syntheticGroup,
    note,
    reference,
    vehicle,
    vehicleupgrade;

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
        return valueOf(typeKey);
    }

    public String createKey(JsonNode x) {
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
            case itementry: {
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
            case subclassfeature: {
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
        if (this == reference) {
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
        return String.format("%s|%s", this.name(), crossRef).toLowerCase()
                // NOTE: correct reference inconsistencies in the original data
                .replaceAll("\\|phb\\|", "||")
                .replaceAll("\\|tce\\|8\\|tce", "|tce|8");
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

    public String defaultSourceString() {
        switch (this) {
            case item:
            case itemfluff:
            case itemproperty:
            case itementry:
            case magicvariant:
                return "DMG";
            case legendarygroup:
            case monster:
            case monsterfluff:
                return "MM";
            case vehicle:
            case vehicleupgrade:
                return "GoS";
            default:
                return "PHB";
        }
    }

    enum IndexFields implements NodeReader {
        level,
        pantheon,
        className,
        classSource,
        featureType,
        raceName,
        raceSource,
        subclassSource,
        subclassShortName
    }

    public void withArrayFrom(JsonNode node, BiConsumer<Tools5eIndexType, JsonNode> callback) {
        node.withArray(this.nodeName()).forEach(x -> callback.accept(this, x));
    }

    public void withArrayFrom(JsonNode node, String field, BiConsumer<Tools5eIndexType, JsonNode> callback) {
        node.withArray(field).forEach(x -> callback.accept(this, x));
    }
}
