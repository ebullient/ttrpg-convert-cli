package dev.ebullient.convert.tools.pf2e;

import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.pf2e.JsonSource.Field;
import dev.ebullient.convert.tools.pf2e.Pf2eSources.DefaultSource;

public enum Pf2eIndexType implements IndexType, NodeReader {
    ability, // B1
    action,
    ancestry,
    archetype,
    background,
    classFeature,
    classtype("class"),
    companion,
    companionAbility,
    condition,
    creature, // B1
    creatureTemplate, // B1
    curse, // GMG
    deity,
    disease, // GMG
    domain,
    eidolon, // SoM
    event, // LOTG
    familiar, // APG
    familiarAbility,
    feat,
    group,
    hazard,
    item,
    language,
    nation, // GMG
    optfeature, // APG
    organization, // LOCG
    place, // GMG
    plane, // GMG
    relicGift, // GMG
    ritual,
    settlement, // GMG
    skill,
    spell,
    subclassFeature,
    table,
    trait,
    trap,
    variantrule, // GMG
    vehicle, // GMG
    versatileHeritage, // APG
    syntheticGroup // for this tool only
    ;

    String templateName;

    Pf2eIndexType() {
        this.templateName = this.name();
    }

    Pf2eIndexType(String templateName) {
        this.templateName = templateName;
    }

    public static Pattern matchPattern = Pattern.compile("\\{@("
            + Stream.of(Pf2eIndexType.values())
                    .map(x -> x.templateName)
                    .collect(Collectors.joining("|"))
            + ") ([^}]+)}");

    public String templateName() {
        return templateName;
    }

    public void withArrayFrom(JsonNode node, BiConsumer<Pf2eIndexType, JsonNode> callback) {
        node.withArray(this.nodeName()).forEach(x -> callback.accept(this, x));
    }

    public static Pf2eIndexType fromTemplateName(String name) {
        return Stream.of(Pf2eIndexType.values())
                .filter(x -> x.templateName.equals(name))
                .findFirst().orElse(null);
    }

    public static Pf2eIndexType getTypeFromKey(String key) {
        String typeKey = key.substring(0, key.indexOf("|"));
        return valueOf(typeKey);
    }

    public String createKey(JsonNode node) {
        // TODO: special keys?
        String name = Field.name.getTextOrEmpty(node);
        String source = Field.source.getTextOrDefault(node, this.defaultSource().name());
        return String.format("%s|%s|%s", this.name(), name, source).toLowerCase();
    }

    public String createKey(String name, String source) {
        return String.format("%s|%s|%s", this.name(), name, source).toLowerCase();
    }

    public String compendiumPath() {
        switch (this) {
            case action:
                return "actions";
            default:
                return null;
        }
    }

    public DefaultSource defaultSource() {
        switch (this) {
            case versatileHeritage:
            case familiar:
            case optfeature:
                return DefaultSource.apg;
            case ability:
            case creature:
            case creatureTemplate:
                return DefaultSource.b1;
            case spell:
            case item:
            case classtype:
            case condition:
            case background:
            case ancestry:
            case archetype:
            case feat:
            case trap:
            case hazard:
            case deity:
            case action:
            case classFeature:
            case subclassFeature:
            case table:
            case language:
            case ritual:
            case trait:
            case group:
            case domain:
            case skill:
            case familiarAbility:
            case companion:
            case companionAbility:
                return DefaultSource.crb;
            case disease:
            case curse:
            case variantrule:
            case vehicle:
            case place:
            case plane:
            case relicGift:
            case settlement:
            case nation:
                return DefaultSource.gmg;
            case organization:
                return DefaultSource.locg;
            case event:
                return DefaultSource.lotg;
            case eidolon:
                return DefaultSource.som;
        }
        throw new IllegalStateException("How did we get here? Switch is missing " + this);
    }
}
