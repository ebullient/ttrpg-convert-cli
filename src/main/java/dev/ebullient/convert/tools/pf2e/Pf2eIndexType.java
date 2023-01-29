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
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;

public enum Pf2eIndexType implements IndexType, NodeReader {
    ability, // B1
    action,
    adventure,
    affliction,
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
    curse("affliction"), // GMG
    data, // data from any source
    deity,
    disease("affliction"), // GMG
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
    syntheticGroup, // for this tool only
    bookReference // this tool only
    ;

    final String templateName;

    Pf2eIndexType() {
        this.templateName = this.name();
    }

    Pf2eIndexType(String templateName) {
        this.templateName = templateName;
    }

    public static final Pattern matchPattern = Pattern.compile("\\{@("
            + Stream.of(Pf2eIndexType.values())
                    .flatMap(x -> Stream.of(x.templateName, x.name()))
                    .distinct()
                    .collect(Collectors.joining("|"))
            + ") ([^{}]+?)}");

    public String templateName() {
        return templateName;
    }

    public boolean isDefaultSource(String source) {
        return defaultSource().sameSource(source);
    }

    public void withArrayFrom(JsonNode node, BiConsumer<Pf2eIndexType, JsonNode> callback) {
        node.withArray(this.nodeName()).forEach(x -> callback.accept(this, x));
    }

    public static Pf2eIndexType fromText(String name) {
        return Stream.of(Pf2eIndexType.values())
                .filter(x -> x.templateName.equals(name) || x.name().equalsIgnoreCase(name))
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

        String name = Field.name.getTextOrEmpty(node);
        String source = Field.source.getTextOrDefault(node, this.defaultSource().name());
        return String.format("%s|%s|%s", this.name(), name, source).toLowerCase();
    }

    public String createKey(String name, String source) {
        if (source == null || this == data) {
            return String.format("%s|%s", this.name(), name).toLowerCase();
        }
        return String.format("%s|%s|%s", this.name(), name, source).toLowerCase();
    }

    public String getVaultRoot(Pf2eIndex index) {
        return useCompendiumBase() ? index.compendiumVaultRoot() : index.rulesVaultRoot();
    }

    public Path getFilePath(Pf2eIndex index) {
        return useCompendiumBase() ? index.compendiumFilePath() : index.rulesFilePath();
    }

    public String relativeRepositoryRoot(Pf2eIndex index) {
        String root = getVaultRoot(index);
        String relativePath = relativePath();

        if (relativePath == null || relativePath.isEmpty() || ".".equals(relativePath)) {
            return root.replaceAll("/$", "");
        }
        return root + relativePath;
    }

    public Pf2eQuteBase convertJson2QuteBase(Pf2eIndex index, JsonNode node) {
        Pf2eIndexType type = this;
        switch (this) {
            // Group: Affliction/Curse/Disease
            case affliction:
                type = Pf2eIndexType.fromText(Field.type.getTextOrDefault(node, "Disease"));
            case curse:
            case disease:
                return new Json2QuteAffliction(index, type, node).build();
            // Other type
            case action:
                return new Json2QuteAction(index, this, node).build();
            case archetype:
                return new Json2QuteArchetype(index, this, node).build();
            case background:
                return new Json2QuteBackground(index, this, node).build();
            case deity:
                return new Json2QuteDeity(index, this, node).build();
            case feat:
                return new Json2QuteFeat(index, this, node).build();
            case hazard:
                return new Json2QuteHazard(index, this, node).build();
            case ritual:
                return new Json2QuteRitual(index, this, node).build();
            case spell:
                return new Json2QuteSpell(index, this, node).build();
            case trait:
                return new Json2QuteTrait(index, this, node).build();
            default:
                return null;
        }
    }

    public boolean alwaysInclude() {
        switch (this) {
            case bookReference:
            case data:
            case syntheticGroup:
                return true;
            default:
                return false;
        }
    }

    public boolean checkCopiesAndReprints() {
        switch (this) {
            case adventure:
            case book:
            case data:
            case syntheticGroup:
                return false; // don't check copy/reprint fields
            default:
                return true;
        }
    }

    public boolean useQuteNote() {
        switch (this) {
            case condition:
            case domain:
            case skill:
            case table:
                return true; // QuteNote-based
            default:
                return false;
        }
    }

    public boolean useCompendiumBase() {
        switch (this) {
            case ability:
            case action:
            case book:
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
            // Simple suffix subdir (rules or compendium)
            case action:
            case feat:
            case spell:
            case table:
            case trait:
            case variantrule:
                return this.name() + 's';
            case ritual:
                return "spells/rituals";
            // Character
            case ancestry:
                return "character/ancestries";
            case classtype:
                return "character/classes";
            case archetype:
            case background:
            case companion:
                return "character/" + this.name() + 's';
            // Equipment
            case item:
            case vehicle:
                return "equipment/" + this.name() + 's';
            // GM
            case curse:
            case disease:
                return "gm/afflictions";
            case creature:
            case hazard:
                return "gm/" + this.name() + 's';
            case relicGift:
                return "gm/relics-gifts";
            // Setting
            case domain:
                return "setting";
            case adventure:
            case language:
            case organization:
            case place:
            case plane:
            case event:
                return "setting/" + this.name() + 's';
            case deity:
                return "setting/deities";
            case ability:
                return "abilities";
            default:
                return ".";
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
            case bookReference:
                return DefaultSource.crb;
            case affliction:
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
            default:
                throw new IllegalStateException("How did we get here? Switch is missing " + this);
        }
    }
}
