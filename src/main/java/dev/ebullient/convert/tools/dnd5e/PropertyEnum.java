package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.qute.TemplateData;

@TemplateData
public enum PropertyEnum {
    AMMUNITION("Ammunition", "A", "property/ammunition"),
    FINESSE("Finesse", "F", "property/finesse"),
    HEAVY("Heavy", "H", "property/heavy"),
    LIGHT("Light", "L", "property/light"),
    LOADING("Loading", "LD", "property/loading"),
    REACH("Reach", "R", "property/reach"),
    SPECIAL("Special", "S", "property/special"),
    THROWN("Thrown", "T", "property/thrown"),
    TWO_HANDED("Two-handed", "2H", "property/two-handed"),
    VERSATILE("Versatile", "V", "property/versatile"),
    MARTIAL("Martial", "M", "property/martial"),
    SILVERED("Silvered", "-", "property/silvered"),
    POISON("Poison", "=", "property/poison"),
    CURSED("Cursed Item", "*", "property/cursed"),

    // Additional properties
    AMMUNITION_FIREARM("Ammunition (Firearm)", "AF", "property/ammunition/firearm"),
    BURST_FIRE("Burst Fire", "BF", "property/burst-fire"),
    RELOAD("Reload", "RLD", "property/reload"),

    // Magic/Wondrous item attributes: tier
    MAJOR("Major", "!", "tier/major"),
    MINOR("Minor", "@", "tier/minor"),

    // Magic/Wondrous item attributes: rarity
    COMMON("Common", "1", "rarity/common"),
    UNCOMMON("Uncommon", "2", "rarity/uncommon"),
    RARE("Rare", "3", "rarity/rare"),
    VERY_RARE("Very Rare", "4", "rarity/very-rare"),
    LEGENDARY("Legendary", "5", "rarity/legendary"),
    ARTIFACT("Artifact", "6", "rarity/artifact"),
    VARIES("varies", "7", "rarity/varies"),
    RARITY_UNKNOWN("unknown", "8", "rarity/unknown"),
    RARITY_UNK_MAGIC("unknown (magic)", "9", "rarity/unknown/magic"),

    // Magic/Wondrous item attributes: attunement
    REQ_ATTUNEMENT("Requires Attunement", "#", "attunement/required"),
    OPT_ATTUNEMENT("Optional Attunement", "$", "attunement/optional");

    public final String longName;
    private final String encodedValue;
    private final String tagValue;

    PropertyEnum(String longName, String ev, String tagValue) {
        this.longName = longName;
        this.encodedValue = ev;
        this.tagValue = tagValue;
    }

    public static final List<PropertyEnum> tierProperties = List.of(MAJOR, MINOR);

    public static final List<PropertyEnum> rarityProperties = Stream.of(PropertyEnum.values())
            .filter(x -> x.ordinal() >= COMMON.ordinal() && x.ordinal() <= RARITY_UNK_MAGIC.ordinal())
            .collect(Collectors.toList());

    public String value() {
        return longName.toLowerCase();
    }

    public String tagValue() {
        return tagValue;
    }

    public boolean mundaneProperty() {
        return !tierProperties.contains(this) && !rarityProperties.contains(this);
    }

    public String getMarkdownLink(Tools5eIndex index) {
        if (rarityProperties.contains(this)) {
            return longName;
        }
        return String.format("[%s](%s)", longName,
                index.rulesVaultRoot() + "item-properties.md#" + longName.replaceAll(" ", "%20"));
    }

    public static PropertyEnum fromValue(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String key = v.toLowerCase();
        for (PropertyEnum p : PropertyEnum.values()) {
            if (p.longName.toLowerCase().equals(key)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Invalid/Unknown property " + v);
    }

    public static PropertyEnum fromEncodedType(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        for (PropertyEnum p : PropertyEnum.values()) {
            if (p.encodedValue.equals(v)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Invalid/Unknown property " + v);
    }

    public static List<PropertyEnum> fromEncodedValue(String v) {
        if (v == null || v.isBlank()) {
            return Collections.emptyList();
        }
        List<PropertyEnum> result = new ArrayList<>();
        for (String s : v.split("\\s*,\\s*")) {
            result.add(fromEncodedType(s));
        }
        result.removeIf(Objects::isNull);
        return result;
    }

    public static void findAdditionalProperties(String name, ItemEnum type, Collection<PropertyEnum> properties,
            Predicate<String> matches) {
        if (type.isWeapon() && name.toLowerCase(Locale.ROOT).contains("silvered")) {
            properties.add(SILVERED);
        }
        if (matches.test("^Curse: .*")) {
            properties.add(PropertyEnum.CURSED);
        }
        if (matches.test(
                "^(This poison is|This poison was|You can use the poison in|This poison must be harvested|A creature subjected to this poison|A creature that ingests this poison) .*")) {
            properties.add(PropertyEnum.POISON);
        }
        if (matches.test(".*it is actually poison.*")) {
            properties.add(PropertyEnum.POISON);
        }
    }
}
