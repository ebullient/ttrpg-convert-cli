package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.valueOrDefault;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;

interface KeyData {
    String name();

    String toKey();

    default String parentName() {
        return "";
    }

    default String parentSource() {
        return "";
    }

    default String level() {
        return "";
    }

    default String itemSource() {
        return "";
    }
}

// Unpack the plain type|name|source key shared by most types (spell, feat, race, monster, ...)
class SimpleKeyData implements KeyData {
    final Tools5eIndexType type;
    final String simpleName;
    final String source;

    public SimpleKeyData(Tools5eIndexType type, String key) {
        String[] parts = key.split("\\|");
        this.type = type;
        this.simpleName = parts[1];
        this.source = parts[2];
    }

    public SimpleKeyData(Tools5eIndexType type, String simpleName, String source) {
        this.type = type;
        this.simpleName = simpleName;
        this.source = source;
    }

    @Override
    public String name() {
        return simpleName;
    }

    public String toKey() {
        return type.createKey(simpleName, source);
    }

    public static SimpleKeyData fromKey(Tools5eIndexType type, String key) {
        return new SimpleKeyData(type, key);
    }

    // 0    name,
    // 1    source
    public static SimpleKeyData fromRefTag(Tools5eIndexType type, String tagBody) {
        String[] parts = tagBody.trim().split("\s?\\|\\s?");
        return new SimpleKeyData(type, parts[0],
                valueOrDefault(parts, 1, type.defaultSourceString()));
    }

    // {@feat name|source|linkText}
    public String toRefTag(JsonNode entry, String linkText) {
        return "%s|%s|%s".formatted(simpleName, source, linkText);
    }
}

// Unpack the type|abbreviation|source key shared by itemType/itemProperty (keyed by abbreviation, not name)
class AbbreviationKeyData implements KeyData {
    final Tools5eIndexType type;
    final String abbreviation;
    final String source;

    public AbbreviationKeyData(Tools5eIndexType type, String key) {
        String[] parts = key.split("\\|");
        this.type = type;
        this.abbreviation = parts[1];
        this.source = parts[2];
    }

    public AbbreviationKeyData(Tools5eIndexType type, String abbreviation, String source) {
        this.type = type;
        this.abbreviation = abbreviation;
        this.source = source;
    }

    @Override
    public String name() {
        return abbreviation;
    }

    public String toKey() {
        return type.createKey(abbreviation, source);
    }

    public static AbbreviationKeyData fromKey(Tools5eIndexType type, String key) {
        return new AbbreviationKeyData(type, key);
    }

    // {@itemType abv|source|linkText}
    public String toRefTag(JsonNode entry, String linkText) {
        return "%s|%s|%s".formatted(abbreviation, source, linkText);
    }
}

// Unpack the id-based type|typeId-id key shared by book/adventure/bookData/adventureData
class DocumentKeyData implements KeyData {
    final Tools5eIndexType type;
    final String typeId;
    final String id;

    public DocumentKeyData(Tools5eIndexType type, String key) {
        String[] parts = key.split("\\|");
        this.type = type;
        String[] idParts = parts[1].split("-", 2);
        this.typeId = idParts[0];
        this.id = idParts[1];
    }

    public DocumentKeyData(Tools5eIndexType type, String typeId, String id) {
        this.type = type;
        this.typeId = typeId;
        this.id = id;
    }

    @Override
    public String name() {
        return id;
    }

    public String toKey() {
        return String.format("%s|%s-%s", type.name(), typeId, id).toLowerCase();
    }

    public static DocumentKeyData fromKey(Tools5eIndexType type, String key) {
        return new DocumentKeyData(type, key);
    }
}

class ClassFeatureKeyData implements KeyData {
    final String cfName;
    final String className;
    final String classSource;
    final String level;
    final String cfSource;

    public ClassFeatureKeyData(String key) {
        String[] parts = key.split("\\|");
        this.cfName = parts[1];
        this.className = parts[2];
        this.classSource = parts[3];
        this.level = parts[4];
        this.cfSource = parts[5];
    }

    public ClassFeatureKeyData(String cfName, String className, String classSource, String level, String cfSource) {
        this.cfName = cfName;
        this.className = className;
        this.classSource = classSource;
        this.level = level;
        this.cfSource = cfSource;
    }

    @Override
    public String name() {
        return cfName;
    }

    @Override
    public String parentName() {
        return className;
    }

    @Override
    public String parentSource() {
        return classSource;
    }

    @Override
    public String level() {
        return level;
    }

    @Override
    public String itemSource() {
        return cfSource;
    }

    public String toKey() {
        return String.join("|",
                Tools5eIndexType.classfeature.name(),
                cfName,
                className, classSource,
                level, cfSource)
                .toLowerCase();
    }

    public SimpleKeyData toParentKey() {
        return new SimpleKeyData(Tools5eIndexType.classtype, className, classSource);
    }

    public static ClassFeatureKeyData fromKey(String key) {
        return new ClassFeatureKeyData(key);
    }

    // 0    name,
    // 1    className,
    // 2    classSource || "phb",
    // 3    level,
    // 4    source || classSource
    public static ClassFeatureKeyData fromRefTag(String tagBody) {
        String[] parts = tagBody.trim().split("\s?\\|\\s?");
        if (parts.length < 4) {
            Tui.instance().errorf("Badly formed Class Feature key (not enough segments): %s", tagBody);
            return null;
        }
        String classSource = valueOrDefault(parts, 2, Tools5eIndexType.classtype.defaultSourceString());
        String featureSource = valueOrDefault(parts, 4, classSource);
        return new ClassFeatureKeyData(parts[0].trim(),
                parts[1].trim(), classSource,
                parts[3].trim(), featureSource);
    }

    // {@classFeature Rage|Barbarian||1||optional display text}
    public String toRefTag(JsonNode entry, String linkText) {
        return "%s|%s|%s|%s|%s|%s".formatted(cfName, className, classSource, level, cfSource, linkText);
    }
}

// Unpack a card key
class CardKeyData implements KeyData {
    final String cardName;
    final String deckName;
    final String source;

    public CardKeyData(String key) {
        String[] parts = key.split("\\|");
        this.cardName = parts[1];
        this.deckName = parts[2];
        this.source = parts[3];
    }

    public CardKeyData(String cardName, String deckName, String source) {
        this.cardName = cardName;
        this.deckName = deckName;
        this.source = source;
    }

    @Override
    public String name() {
        return cardName;
    }

    public String toKey() {
        return String.join("|",
                Tools5eIndexType.card.name(),
                cardName, deckName, source)
                .toLowerCase();
    }

    public SimpleKeyData toParentKey() {
        return new SimpleKeyData(Tools5eIndexType.deck, deckName, source);
    }

    public static CardKeyData fromKey(String key) {
        return new CardKeyData(key);
    }

    // 0    name,
    // 1    set,
    // 2    source
    public static CardKeyData fromRefTag(String tagBody) {
        String[] parts = tagBody.trim().split("\s?\\|\\s?");
        return new CardKeyData(
                parts[0].trim(),
                parts[1].trim(),
                valueOrDefault(parts, 2, Tools5eIndexType.card.defaultSourceString()));
    }

    // {@card Donjon|Deck of Several Things|LLK}
    public String toRefTag(JsonNode entry, String linkText) {
        return "%s|%s|%s".formatted(cardName, deckName, source);
    }
}

// Unpack a deity key
class DeityKeyData implements KeyData {
    final String deityName;
    final String pantheon;
    final String source;

    public DeityKeyData(String key) {
        String[] parts = key.split("\\|");
        this.deityName = parts[1];
        this.pantheon = parts[2];
        this.source = parts[3];
    }

    public DeityKeyData(String deityName, String pantheon, String source) {
        this.deityName = deityName;
        this.pantheon = pantheon;
        this.source = source;
    }

    @Override
    public String name() {
        return deityName;
    }

    public String toKey() {
        return String.join("|",
                Tools5eIndexType.deity.name(),
                deityName, pantheon, source)
                .toLowerCase();
    }

    public static DeityKeyData fromKey(String key) {
        return new DeityKeyData(key);
    }

    // 0    name,
    // 1    pantheon,
    // 2    source
    public static DeityKeyData fromRefTag(String tagBody) {
        String[] parts = tagBody.trim().split("\s?\\|\\s?");
        return new DeityKeyData(
                parts[0],
                valueOrDefault(parts, 1, "Forgotten Realms"),
                valueOrDefault(parts, 2, Tools5eIndexType.deity.defaultSourceString()));
    }

    // {@deity Ioun|dawn war|dmg|and optional link text added with another pipe}
    public String toRefTag(JsonNode entry, String linkText) {
        return "%s|%s|%s|%s".formatted(deityName, pantheon, source, linkText);
    }
}

// Unpack a subrace key
class SubraceKeyData implements KeyData {
    final String subraceName;
    final String raceName;
    final String raceSource;
    final String source;

    public SubraceKeyData(String key) {
        String[] parts = key.split("\\|");
        this.subraceName = parts[1];
        this.raceName = parts[2];
        this.raceSource = parts[3];
        this.source = parts[4];
    }

    public SubraceKeyData(String subraceName, String raceName, String raceSource, String source) {
        this.subraceName = subraceName;
        this.raceName = raceName;
        this.raceSource = raceSource;
        this.source = source;
    }

    @Override
    public String name() {
        return subraceName;
    }

    @Override
    public String parentName() {
        return raceName;
    }

    @Override
    public String parentSource() {
        return raceSource;
    }

    @Override
    public String itemSource() {
        return source;
    }

    public String toKey() {
        return String.join("|",
                Tools5eIndexType.subrace.name(),
                subraceName, raceName, raceSource, source)
                .toLowerCase();
    }

    public static SubraceKeyData fromKey(String key) {
        return new SubraceKeyData(key);
    }

    // 0    name,
    // 1    raceName,
    // 2    raceSource,
    // 3    source
    public static SubraceKeyData fromRefTag(String tagBody) {
        String[] parts = tagBody.trim().split("\s?\\|\\s?");
        String raceSource = valueOrDefault(parts, 2, "phb");
        return new SubraceKeyData(
                parts[0],
                parts[1],
                raceSource,
                valueOrDefault(parts, 3, raceSource));
    }

    public String toRefTag(JsonNode entry, String linkText) {
        return "%s|%s|%s|%s|%s".formatted(subraceName, raceName, raceSource, source, linkText);
    }
}

// Unpack a subclass key
class SubclassKeyData implements KeyData {
    String scName;
    String className;
    String classSource;
    String scSource;

    public SubclassKeyData(String key) {
        String[] parts = key.split("\\|");
        this.scName = parts[1];
        this.className = parts[2];
        this.classSource = parts[3];
        this.scSource = parts[4];
    }

    public SubclassKeyData(String scName, String className, String classSource, String scSource) {
        this.scName = scName;
        this.className = className;
        this.classSource = classSource;
        this.scSource = scSource;
    }

    @Override
    public String name() {
        return scName;
    }

    @Override
    public String parentName() {
        return className;
    }

    @Override
    public String parentSource() {
        return classSource;
    }

    @Override
    public String level() {
        return "";
    }

    @Override
    public String itemSource() {
        return scSource;
    }

    public String toKey() {
        return String.join("|",
                Tools5eIndexType.subclass.name(),
                scName,
                className, classSource,
                scSource)
                .toLowerCase();
    }

    public SimpleKeyData toParentKey() {
        return new SimpleKeyData(Tools5eIndexType.classtype, className, classSource);
    }

    public static SubclassKeyData fromKey(String key) {
        return new SubclassKeyData(key);
    }

    // Homebrew and reprint tags
    // {@subclass Artillerist|Artificer|TCE|TCE}
    // 0    subclassShortName,
    // 1    IndexFields.className.getTextOrEmpty(x),
    // 2    classSource || "phb",
    // 3    subClassSource || "phb"
    public static SubclassKeyData fromRefTag(String tagBody) {
        String[] parts = tagBody.trim().split("\s?\\|\\s?");
        if (parts.length < 2) {
            Tui.instance().errorf("Badly formed Subclass key (not enough segments): %s", tagBody);
            return null;
        }
        return new SubclassKeyData(
                parts[0],
                parts[1],
                valueOrDefault(parts, 2, "phb"),
                valueOrDefault(parts, 3, "phb"));
    }

    // {@subclass Artillerist|Artificer|TCE|TCE}
    public String toRefTag(JsonNode entry, String linkText) {
        return "%s|%s|%s|%s|%s".formatted(
                scName,
                className,
                classSource,
                SourceField.source.getTextOrEmpty(entry),
                linkText);
    }
}

// Unpack a subclass feature key
class SubclassFeatureKeyData implements KeyData {
    String scfName;
    String className;
    String classSource;
    String scName;
    String scSource;
    String level;
    String scfSource;

    public SubclassFeatureKeyData(String key) {
        String[] parts = key.split("\\|");
        this.scfName = parts[1];
        this.className = parts[2];
        this.classSource = parts[3];
        this.scName = parts[4];
        this.scSource = parts[5];
        this.level = parts[6];
        this.scfSource = parts[7];
    }

    public SubclassFeatureKeyData(String scfName, String className, String classSource,
            String scName, String scSource, String level, String scfSource) {
        this.scfName = scfName;
        this.className = className;
        this.classSource = classSource;
        this.scName = scName;
        this.scSource = scSource;
        this.level = level;
        this.scfSource = scfSource;
    }

    @Override
    public String name() {
        return scfName;
    }

    @Override
    public String parentName() {
        return scName;
    }

    @Override
    public String parentSource() {
        return scSource;
    }

    @Override
    public String level() {
        return level;
    }

    @Override
    public String itemSource() {
        return scfSource;
    }

    public String toKey() {
        return String.join("|",
                Tools5eIndexType.subclassFeature.name(),
                scfName,
                className, classSource,
                scName, scSource,
                level, scfSource)
                .toLowerCase();
    }

    public SubclassKeyData toParentKey() {
        return new SubclassKeyData(scName, className, classSource, scSource);
    }

    public static SubclassFeatureKeyData fromKey(String key) {
        return new SubclassFeatureKeyData(key);
    }

    // 0    name,
    // 1    IndexFields.className.getTextOrEmpty(x),
    // 2    classSource || "phb",
    // 3    IndexFields.subclassShortName.getTextOrEmpty(x),
    // 4    subClassSource || "phb",
    // 5    IndexFields.level.getTextOrEmpty(x),
    // 6    source || subClassSource
    public static SubclassFeatureKeyData fromRefTag(String tagBody) {
        String[] parts = tagBody.trim().split("\s?\\|\\s?");
        if (parts.length < 6) {
            Tui.instance().errorf("Badly formed Subclass Feature key (not enough segments): %s", tagBody);
            return null;
        }
        String classSource = valueOrDefault(parts, 2, "phb");
        String scSource = valueOrDefault(parts, 4, "phb");
        String featureSource = valueOrDefault(parts, 6, scSource);

        return new SubclassFeatureKeyData(parts[0],
                parts[1].trim(), classSource,
                parts[3].trim(), scSource,
                parts[5].trim(), featureSource);
    }

    // {@subclassFeature Blessed Strikes|Cleric|PHB|Twilight|TCE|8|TCE}
    public String toRefTag(JsonNode entry, String linkText) {
        return "%s|%s|%s|%s|%s|%s|%s|%s".formatted(
                scfName,
                className, classSource,
                scName, scSource,
                level,
                SourceField.source.getTextOrEmpty(entry),
                linkText);
    }
}
