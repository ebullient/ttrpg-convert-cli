package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.pf2e.Pf2eSources.DefaultSource;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;

public enum Pf2eIndexType implements IndexType, JsonNodeReader {
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
    creatureTemplateFluff, // B1
    curse("affliction"), // GMG
    data, // data from any source
    deity,
    deityFluff,
    disease("affliction"), // GMG
    domain,
    eidolon, // SoM
    event, // LOTG
    familiar, // APG
    familiarAbility,
    feat,
    group,
    hazard,
    baseitem,
    item,
    language,
    nation, // GMG
    optfeature, // APG
    organization, // LOCG
    organizationFluff, // LOCG
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
            + Stream.of(values())
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
        return Stream.of(values())
                .filter(x -> x.templateName.equals(name) || x.name().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public static Pf2eIndexType getTypeFromKey(String key) {
        String typeKey = key.substring(0, key.indexOf("|"));
        return valueOf(typeKey);
    }

    @Override
    public String createKey(JsonNode node) {
        if (this == book || this == adventure) {
            String id = SourceField.id.getTextOrEmpty(node);
            return String.format("%s|%s-%s", this.name(), this.name(), id).toLowerCase();
        }

        String name = SourceField.name.getTextOrEmpty(node);
        String source = SourceField.source.getTextOrDefault(node, this.defaultSourceString());
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

        if (relativePath.isEmpty() || ".".equals(relativePath)) {
            return root.replaceAll("/$", "");
        }
        return root + relativePath;
    }

    public Pf2eQuteBase convertJson2QuteBase(Pf2eIndex index, JsonNode node) {
        // also update #isOutputType
        return switch (this) {
            case action -> new Json2QuteAction(index, node).build();
            case archetype -> new Json2QuteArchetype(index, node).build();
            case background -> new Json2QuteBackground(index, node).build();
            case deity -> new Json2QuteDeity(index, node).build();
            case feat -> new Json2QuteFeat(index, node).build();
            case hazard -> new Json2QuteHazard(index, node).build();
            case item -> new Json2QuteItem(index, node).build();
            case ritual -> new Json2QuteRitual(index, node).build();
            case spell -> new Json2QuteSpell(index, node).build();
            case trait -> new Json2QuteTrait(index, node).build();
            case creature -> new Json2QuteCreature(index, node).build();
            default -> null;
        };
    }

    public boolean alwaysInclude() {
        return switch (this) {
            case bookReference, data, syntheticGroup -> true;
            default -> false;
        };
    }

    public boolean checkCopiesAndReprints() {
        return switch (this) {
            case adventure, book, data, syntheticGroup -> false; // don't check copy/reprint fields
            default -> true;
        };
    }

    public boolean useQuteNote() {
        // also update #isOutputType
        return switch (this) {
            case ability,
                    affliction,
                    book,
                    condition,
                    curse,
                    disease,
                    domain,
                    skill,
                    table ->
                true; // QuteNote-based
            default -> false;
        };
    }

    public boolean useCompendiumBase() {
        return switch (this) {
            case ability,
                    action,
                    book,
                    condition,
                    trait,
                    table,
                    variantrule ->
                false; // use rules
            default -> true; // use compendium
        };
    }

    public boolean isOutputType() {
        return switch (this) {
            case ability,
                    action,
                    affliction,
                    archetype,
                    background,
                    book,
                    condition,
                    creature,
                    curse,
                    deity,
                    disease,
                    domain,
                    feat,
                    hazard,
                    item,
                    ritual,
                    skill,
                    spell,
                    table,
                    trait ->
                true;
            default -> false;
        };
    }

    public String relativePath() {
        // also update #isOutputType
        return switch (this) {
            // Simple suffix subdir (rules or compendium)
            case action, feat, spell, table, trait, variantrule -> this.name() + 's';
            case ritual -> "spells/rituals";
            // Character
            case ancestry -> "character/ancestries";
            case classtype -> "character/classes";
            case archetype, background, companion -> "character/" + this.name() + 's';
            // Equipment
            case item, vehicle -> "equipment/" + this.name() + 's';
            // GM
            case affliction, curse, disease -> "gm/afflictions";
            case creature, hazard -> "gm/" + this.name() + 's';
            case relicGift -> "gm/relics-gifts";
            // Setting
            case domain -> "setting";
            case adventure, language, organization, place, plane, event -> "setting/" + this.name() + 's';
            case deity -> "setting/deities";
            case ability -> "abilities";
            default -> ".";
        };
    }

    public String defaultSourceString() {
        return defaultSource().name();
    }

    public DefaultSource defaultSource() {
        return switch (this) {
            case familiar, optfeature, versatileHeritage -> DefaultSource.apg;
            case ability, creature, creatureTemplate -> DefaultSource.b1;
            case action, adventure, ancestry, archetype, background, book, classFeature, classtype, companion, companionAbility,
                    condition, deity, domain, familiarAbility, feat, group, hazard, item, language, ritual, skill, spell,
                    subclassFeature, table, trait, trap, bookReference, syntheticGroup ->
                DefaultSource.crb;
            case affliction, curse, disease, nation, place, plane, relicGift, settlement, variantrule, vehicle ->
                DefaultSource.gmg;
            case organization -> DefaultSource.locg;
            case event -> DefaultSource.lotg;
            case eidolon -> DefaultSource.som;
            default -> {
                Tui.instance().errorf("Can not find defaultSource for type %s; assuming crb", this);
                yield DefaultSource.crb;
            }
        };
    }
}
