package dev.ebullient.json5e.tools5e;

public enum IndexType {
    background,
    backgroundfluff,
    classtype,
    classfeature,
    feat,
    item,
    itementry,
    itemfluff,
    itemvariant,
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
    namelist,
    reference;

    public static IndexType getTypeFromKey(String key) {
        String typeKey = key.substring(0, key.indexOf("|"));
        return valueOf(typeKey);
    }
}
