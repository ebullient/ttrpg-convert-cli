package dev.ebullient.convert.tools.dnd5e;

public enum IndexType {
    background,
    backgroundfluff,
    classtype,
    classfeature,
    deity,
    feat,
    item,
    itementry,
    itemfluff,
    itemvariant,
    legendarygroup,
    monster,
    monsterfluff,
    race,
    racefluff,
    spell,
    spellfluff,
    subclass,
    subclassfeature,
    subrace,
    optionalfeature,
    table,
    trait,
    sourceless,
    note,
    reference;

    public static IndexType getTypeFromKey(String key) {
        String typeKey = key.substring(0, key.indexOf("|"));
        return valueOf(typeKey);
    }
}
