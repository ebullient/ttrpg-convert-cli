package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.JsonSource.Field;
import dev.ebullient.convert.tools.pf2e.Pf2eSources.DefaultSource;

public enum Pf2eIndexType implements IndexType, NodeReader {
    ability, // B1
    action,
    adventure,
    ancestry,
    archetype,
    background,
    book,
    classFeature,
    classtype("class"),
    companion,
    companionAbility,
    condition,
    creature, // B1
    creatureTemplate, // B1
    curse, // GMG
    data, // data from any source
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
        if (this == book || this == adventure) {
            String id = Field.id.getTextOrEmpty(node);
            return String.format("%s|%s-%s", this.name(), this.name(), id).toLowerCase();
        }
        // TODO: special keys?
        String name = Field.name.getTextOrEmpty(node);
        String source = Field.source.getTextOrDefault(node, this.defaultSource().name());
        return String.format("%s|%s|%s", this.name(), name, source).toLowerCase();
    }

    public String createKey(String name, String source) {
        if (source == null) {
            return String.format("%s|%s", this.name(), name).toLowerCase();
        }
        return String.format("%s|%s|%s", this.name(), name, source).toLowerCase();
    }

    public String getRepoRoot(Pf2eIndex index) {
        return useCompendiumPath() ? index.compendiumRoot() : index.rulesRoot();
    }

    public Path getBasePath(Pf2eIndex index) {
        return useCompendiumPath() ? index.compendiumPath() : index.rulesPath();
    }

    public boolean checkCopiesAndReprints() {
        switch (this) {
            case adventure:
            case book:
            case data:
            case syntheticGroup:
                return false; // use rules
            default:
                return true; // use compendium
        }
    }

    public boolean useCompendiumPath() {
        switch (this) {
            case action:
            case condition:
            case trait:
            case table:
            case variantrule:
                return false; // use rules
            default:
                return true; // use compendium
        }
    }

    public String relativePath() {
        switch (this) {
            case action:
                return this.name() + 's';
            default:
                return null;
        }
    }

    public DefaultSource defaultSource() {
        switch (this) {
            case familiar:
            case optfeature:
            case versatileHeritage:
                return DefaultSource.apg;
            case ability:
            case creature:
            case creatureTemplate:
                return DefaultSource.b1;
            case action:
            case adventure:
            case ancestry:
            case archetype:
            case background:
            case book:
            case classFeature:
            case classtype:
            case companion:
            case companionAbility:
            case condition:
            case deity:
            case domain:
            case familiarAbility:
            case feat:
            case group:
            case hazard:
            case item:
            case language:
            case ritual:
            case skill:
            case spell:
            case subclassFeature:
            case table:
            case trait:
            case trap:
                return DefaultSource.crb;
            case curse:
            case disease:
            case nation:
            case place:
            case plane:
            case relicGift:
            case settlement:
            case variantrule:
            case vehicle:
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
