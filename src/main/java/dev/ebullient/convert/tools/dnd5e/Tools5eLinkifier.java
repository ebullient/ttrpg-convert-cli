package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.toAnchorTag;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.dnd5e.Json2QuteClass.SubclassKeyData;
import dev.ebullient.convert.tools.dnd5e.Json2QuteDeity.DeityField;
import dev.ebullient.convert.tools.dnd5e.Json2QuteMonster.MonsterType;
import dev.ebullient.convert.tools.dnd5e.JsonSource.Tools5eFields;
import dev.ebullient.convert.tools.dnd5e.OptionalFeatureIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType.IndexFields;

public class Tools5eLinkifier {
    private static Tools5eLinkifier instance;

    public static Tools5eLinkifier instance() {
        if (instance == null) {
            instance = new Tools5eLinkifier();
        }
        return instance;
    }

    Tools5eIndex index;
    Tui tui;

    private Tools5eLinkifier() {
        reset();
    }

    public String monsterPath(boolean isNpc, String type) {
        return getRelativePath(Tools5eIndexType.monster) + "/" + (isNpc ? "npc" : MonsterType.toDirectory(type));
    }

    public String monsterPath(boolean isNpc, MonsterType type) {
        return getRelativePath(Tools5eIndexType.monster) + "/" + (isNpc ? "npc" : type.toDirectory());
    }

    public String vaultRoot(Tools5eSources sources) {
        return vaultRoot(sources.getType());
    }

    public String vaultRoot(Tools5eIndexType type) {
        return type.useCompendiumBase()
                ? index.compendiumVaultRoot()
                : index.rulesVaultRoot();
    }

    public String getRelativePath(Tools5eSources sources) {
        return getRelativePath(sources.getType());
    }

    public String getRelativePath(Tools5eIndexType type) {
        return switch (type) {
            case adventureData -> "adventures";
            case bookData -> "books";
            case card, deck -> "decks";
            case classtype, subclass -> "classes";
            case condition, status -> "conditions";
            case deity -> "deities";
            case facility -> "bastions";
            case item, itemGroup -> "items";
            case itemType -> "item-types";
            case itemMastery -> "item-mastery";
            case itemProperty -> "item-properties";
            case legendaryGroup -> "bestiary/legendary-group";
            case magicvariant -> "items";
            case monster -> "bestiary";
            case optfeature -> "optional-features";
            case optionalFeatureTypes, spellIndex -> "lists";
            case race, subrace -> "races";
            case table, tableGroup -> "tables";
            case trap, hazard -> "traps-hazards";
            case variantrule -> "variant-rules";
            default -> type.name() + 's';
        };
    }

    public String getTargetFileName(String name, Tools5eSources sources) {
        Tools5eIndexType type = sources.getType();
        JsonNode node = sources.findNode();
        return switch (type) {
            case background -> fixFileName(decoratedName(type, node), sources.primarySource(), type);
            case deity -> getDeityResourceName(sources);
            case subclass -> getSubclassResource(sources.getKey());
            default -> fixFileName(name, sources.primarySource(), type);
        };
    }

    public String getTargetFileName(String name, String source, Tools5eIndexType type) {
        return fixFileName(name, source, type);
    }

    private String fixFileName(String fileName, Tools5eSources sources) {
        return fixFileName(fileName, sources.primarySource(), sources.getType());
    }

    private String fixFileName(String fileName, String primarySource, Tools5eIndexType type) {
        if (type == Tools5eIndexType.adventureData
                || type == Tools5eIndexType.adventure
                || type == Tools5eIndexType.book
                || type == Tools5eIndexType.bookData
                || type == Tools5eIndexType.tableGroup) {
            return Tui.slugify(fileName); // file name is based on chapter, etc.
        }
        return Tui.slugify(fileName.replaceAll(" \\(\\*\\)", "-gv")
                + sourceIfNotDefault(primarySource, type));
    }

    private static String sourceIfNotDefault(String source, Tools5eIndexType type) {
        if (type == null) {
            return "";
        }
        String defaultSource = type.defaultOutputSource();
        // Special cases for items that are in the phb or xphb
        if (type == Tools5eIndexType.item
                || type == Tools5eIndexType.itemGroup
                || type == Tools5eIndexType.magicvariant) {
            if (source.equalsIgnoreCase("phb") && defaultSource.equalsIgnoreCase("dmg")) {
                return "";
            }
            if (source.equalsIgnoreCase("xphb") && defaultSource.equalsIgnoreCase("xdmg")) {
                return "";
            }
        }
        // Special cases for monsters that are in the phb or xphb
        if (type == Tools5eIndexType.monster
                || type == Tools5eIndexType.legendaryGroup) {
            if (source.equalsIgnoreCase("phb") && defaultSource.equalsIgnoreCase("mm")) {
                return "";
            }
            if (source.equalsIgnoreCase("xphb") && defaultSource.equalsIgnoreCase("xmm")) {
                return "";
            }
        }
        if (source.equalsIgnoreCase(defaultSource)) {
            return "";
        }

        return "-" + Tui.slugify(source);
    }

    // --- create links ---

    public String link(String linkText, String key) {
        if (key == null || index.isExcluded(key)) {
            return linkText;
        }
        Tools5eSources linkSource = Tools5eSources.findSources(key);
        return createLink(linkText, key, linkSource);
    }

    public String link(Tools5eSources linkSource) {
        JsonNode node = linkSource.findNode();
        String linkText = decoratedName(node);
        String key = linkSource.getKey();
        if (index.isExcluded(key)) {
            return linkText;
        }
        return createLink(linkText, key, linkSource);
    }

    public String link(String linkText, Tools5eSources linkSource) {
        String key = linkSource.getKey();
        if (index.isExcluded(key)) {
            return linkText;
        }
        return createLink(linkText, key, linkSource);
    }

    private String createLink(String linkText, String key, Tools5eSources linkSource) {
        Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
        return switch (type) {
            case action,
                    condition,
                    disease,
                    sense,
                    skill,
                    status ->
                linkRule(linkText, key);
            case card -> linkCard(linkText, key);
            case classtype -> linkClass(linkText, key);
            case classfeature -> linkClassFeature(linkText, key);
            case monster -> linkCreature(linkText, key);
            case deity -> linkDeity(linkText, key);
            case subclass -> linkSubclass(linkText, key);
            case variantrule -> linkVariantRules(linkText, key);
            default -> {
                JsonNode node = index.getNode(key);
                yield linkOrText(linkText, key,
                        getRelativePath(type),
                        fixFileName(decoratedName(type, node), linkSource.primarySource(), type));
            }
        };
    }

    private String linkOrText(String linkText, String key, String dirName, String resourceName) {
        return index.isIncluded(key)
                ? "[%s](%s%s/%s.md)".formatted(linkText,
                        index.compendiumVaultRoot(),
                        dirName,
                        slugify(resourceName))
                : linkText;
    }

    private String linkCard(String linkText, String cardKey) {
        Tools5eSources cardSources = Tools5eSources.findSources(cardKey);
        JsonNode node = cardSources.findNode();
        String cardName = cardSources.getName();
        String deckName = IndexFields.set.getTextOrThrow(node).trim();

        return "[%s](%s%s/%s.md#%s)".formatted(
                linkText,
                index.compendiumVaultRoot(),
                getRelativePath(Tools5eIndexType.deck),
                fixFileName(deckName, cardSources.primarySource(), Tools5eIndexType.card),
                cardName.replace(" ", "%20"));
    }

    private String linkClass(String linkText, String classKey) {
        Tools5eSources classSources = Tools5eSources.findSources(classKey);
        return linkOrText(linkText, classKey,
                getRelativePath(Tools5eIndexType.classtype),
                getClassResource(classSources.getName(), classSources.primarySource()));
    }

    private String linkClassFeature(String linkText, String featureKey) {
        JsonNode featureNode = index.getNode(featureKey);
        Tools5eSources featureSources = Tools5eSources.findSources(featureKey);
        int level = IndexFields.level.intOrThrow(featureNode); // required

        String headerName = decoratedFeatureTypeName(featureSources, featureNode) + " (Level " + level + ")";
        String resource = slugify(getClassResource(
                IndexFields.className.getTextOrEmpty(featureNode),
                IndexFields.classSource.getTextOrEmpty(featureNode)));

        return "[%s](%s%s/%s.md#%s)".formatted(
                linkText,
                index.compendiumVaultRoot(),
                getRelativePath(Tools5eIndexType.classtype),
                resource,
                toAnchorTag(headerName));
    }

    private String linkCreature(String linkText, String creatureKey) {
        JsonNode node = index.getNode(creatureKey);
        Tools5eSources sources = Tools5eSources.findSources(creatureKey);

        MonsterType creatureType = MonsterType.fromNode(node, index); // may be missing for partial index
        String resourceName = decoratedName(Tools5eIndexType.monster, node);
        boolean isNpc = Json2QuteMonster.isNpc(node);

        return linkOrText(linkText, creatureKey,
                monsterPath(isNpc, creatureType),
                fixFileName(resourceName, sources.primarySource(), Tools5eIndexType.monster));
    }

    private String linkDeity(String linkText, String deityKey) {
        Tools5eSources deitySources = Tools5eSources.findSources(deityKey);
        return linkOrText(linkText, deityKey,
                getRelativePath(Tools5eIndexType.deity),
                getDeityResourceName(deitySources));
    }

    public String linkOptionalFeature(String linkText, String featureType) {
        OptionalFeatureType oft = index.getOptionalFeatureType(featureType);
        if (oft == null) {
            return linkText;
        }
        if (linkText.equals(featureType)) {
            linkText = oft.getTitle();
        }
        return linkOrText(linkText, oft.getKey(),
                getRelativePath(Tools5eIndexType.optionalFeatureTypes),
                oft.getFilename());
    }

    private String linkRule(String linkText, String ruleKey) {
        Tools5eSources sources = Tools5eSources.findSources(ruleKey);
        String sectionName = sources == null ? linkText : sources.getName();

        return "[%s](%s%s.md#%s)".formatted(
                linkText,
                index.rulesVaultRoot(),
                getRelativePath(Tools5eIndexType.getTypeFromKey(ruleKey)),
                toAnchorTag(sectionName));
    }

    public String linkSpellEntry(Tools5eSources sources) {
        JsonNode spellNode = sources.findNode();
        String name = decoratedName(Tools5eIndexType.spell, spellNode);
        return "[%s](%s%s/%s.md \"%s\")".formatted(name,
                Tools5eIndex.instance().compendiumVaultRoot(),
                getRelativePath(Tools5eIndexType.spell),
                fixFileName(name, sources),
                sources.primarySource());
    }

    public String linkSubclass(String linkText, String subclassKey) {
        return linkOrText(linkText, subclassKey,
                getRelativePath(Tools5eIndexType.classtype),
                getSubclassResource(subclassKey));
    }

    public String linkSubclassFeature(String linkText,
            String featureKey, JsonNode featureJson,
            String subclassKey, JsonNode subclassNode) {

        if (index.isExcluded(featureKey)) {
            return linkText;
        }

        Tools5eSources featureSources = Tools5eSources.findSources(featureKey);

        String level = Tools5eFields.level.getTextOrEmpty(featureJson);
        String headerName = decoratedFeatureTypeName(featureSources, featureJson) + " (Level " + level + ")";
        String resource = slugify(getSubclassResource(
                SourceField.name.getTextOrEmpty(subclassNode),
                IndexFields.className.getTextOrEmpty(subclassNode),
                IndexFields.classSource.getTextOrEmpty(subclassNode),
                SourceField.source.getTextOrEmpty(subclassNode)));
        return "[%s](%s%s/%s.md#%s)".formatted(
                linkText,
                index.compendiumVaultRoot(),
                getRelativePath(Tools5eIndexType.classtype),
                resource,
                toAnchorTag(headerName));
    }

    private String linkVariantRules(String linkText, String rulesKey) {
        Tools5eSources rulesSources = Tools5eSources.findSources(rulesKey);
        String name = rulesSources.getName();
        return "[%s](%s%s/%s.md)".formatted(
                linkText,
                index.rulesVaultRoot(),
                getRelativePath(Tools5eIndexType.variantrule),
                fixFileName(name, rulesSources));
    }

    // --- construct resource names ---

    public String getDeityResourceName(Tools5eSources deitySources) {
        String name = deitySources.getName();
        String source = deitySources.primarySource();
        JsonNode node = deitySources.findNode();
        String pantheon = DeityField.pantheon.getTextOrEmpty(node);
        String suffix = "";
        switch (pantheon.toLowerCase()) {
            case "exandria" -> {
                suffix = TtrpgConfig.getConfig().sourceIncluded("egw") && source.equalsIgnoreCase("egw")
                        ? ""
                        : ("-" + Tui.slugify(source));
            }
            case "dragonlance" -> {
                suffix = TtrpgConfig.getConfig().sourceIncluded("dsotdq") && source.equalsIgnoreCase("dsotdq")
                        ? ""
                        : ("-" + Tui.slugify(source));
            }
            default -> {
                suffix = sourceIfNotDefault(source, Tools5eIndexType.deity);
            }
        }
        return Tui.slugify(pantheon + "-" + name) + suffix;
    }

    public String getClassResource(String className, String classSource) {
        return fixFileName(className, classSource, Tools5eIndexType.classtype);
    }

    public String getSubclassResource(String subclassKey) {
        SubclassKeyData subclassData = new SubclassKeyData(subclassKey);
        return getSubclassResource(subclassData.name(),
                subclassData.parentName(), subclassData.parentSource(),
                subclassData.itemSource());
    }

    public String getSubclassResource(String subclass, String parentClass, String classSource, String subclassSource) {
        String parentFile = Tui.slugify(parentClass);
        String defaultSource = Tools5eIndexType.classtype.defaultOutputSource();
        if (!classSource.equalsIgnoreCase(defaultSource) &&
                (classSource.equalsIgnoreCase("phb") || classSource.equalsIgnoreCase("xphb"))) {
            // For the most part, all subclasses are derived from the basic classes.
            // There wasn't really a need to include the class source in the file name.
            // However, the XPHB has created duplicates of all of the base classes.
            // So if the parent class is not from the default source, we need to include
            // its source in the file name if it's from the PHB or XPHB.
            parentFile += "-" + classSource;
        }
        return fixFileName(
                parentFile + "-" + Tui.slugify(subclass),
                subclassSource,
                Tools5eIndexType.subclass);
    }

    public String getOptionalFeatureTypeResource(String name) {
        return slugify("list-optfeaturetype-" + name);
    }

    public String getClassSpellList(JsonNode classNode) {
        return getClassSpellList(SourceField.name.getTextOrEmpty(classNode));
    }

    public String getClassSpellList(String className) {
        return "list-spells-%s-%s".formatted(
                getRelativePath(Tools5eIndexType.classtype),
                className.toLowerCase());
    }

    public String getSpellList(String name, Tools5eSources sources) {
        Tools5eIndexType type = sources.getType();
        JsonNode node = sources.findNode();
        if (type == Tools5eIndexType.classtype) {
            return getClassSpellList(node);
        }
        final String fileResource = fixFileName(name, sources);
        return "list-spells-%s-%s".formatted(getRelativePath(type), fileResource);
    }

    public String decoratedName(JsonNode entry) {
        Tools5eIndexType type = Tools5eIndexType.getTypeFromNode(entry);
        return decoratedName(type, entry);
    }

    public String decoratedName(Tools5eIndexType type, JsonNode entry) {
        String name = SourceField.name.getTextOrEmpty(entry);
        switch (type) {
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
        if (Tools5eIndex.isSrdBasicOnly() && sources.isSrdOrBasicRules()) {
            String srcName = SourceField.name.getTextOrEmpty(entry);
            if (name.equalsIgnoreCase(srcName)) {
                // SRD name may be different / generic
                name = sources.getName();
            }
        }
        return Tools5eIndex.instance().replaceText(name);
    }

    public String decoratedFeatureTypeName(Tools5eSources valueSources, JsonNode value) {
        String name = valueSources.getName();
        String type = IndexFields.featureType.getTextOrEmpty(value);

        if (!type.isEmpty()) {
            switch (type) {
                case "D":
                    return "Dragon Mark: " + name;
                case "ED":
                    return "Elemental Discipline: " + name;
                case "EI":
                    return "Eldritch Invocation: " + name;
                case "MM":
                    return "Metamagic: " + name;
                case "MV":
                case "MV:B":
                case "MV:C2-UA":
                    return "Maneuver: " + name;
                case "FS:F":
                case "FS:B":
                case "FS:R":
                case "FS:P":
                    return "Fighting Style: " + name;
                case "AS":
                case "AS:V1-UA":
                case "AS:V2-UA":
                    return "Arcane Shot: " + name;
                case "PB":
                    return "Pact Boon: " + name;
                case "AI":
                    return "Artificer Infusion: " + name;
                case "SHP:H":
                case "SHP:M":
                case "SHP:W":
                case "SHP:F":
                case "SHP:O":
                    return "Ship Upgrade: " + name;
                case "IWM:W":
                    return "Infernal War Machine Variant: " + name;
                case "IWM:A":
                case "IWM:G":
                    return "Infernal War Machine Upgrade: " + name;
                case "OR":
                    return "Onomancy Resonant: " + name;
                case "RN":
                    return "Rune Knight Rune: " + name;
                case "AF":
                    return "Alchemical Formula: " + name;
                default:
                    tui.errorf("Unknown feature type %s for class feature %s", type, name);
            }
        }
        return name;
    }

    String slugify(String s) {
        return Tui.slugify(s);
    }

    void reset() {
        index = Tools5eIndex.instance();
        tui = Tui.instance();
    }
}
