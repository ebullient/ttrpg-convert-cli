package dev.ebullient.convert.tools.dnd5e;

import dev.ebullient.convert.tools.IndexType;

public enum Tools5eIndexType implements IndexType {
    background,
    backgroundfluff,
    classtype("class"),
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
    subrace("race"),
    optionalfeature,
    table,
    trait,
    sourceless,
    note,
    reference;

    String templateName;

    Tools5eIndexType() {
        this.templateName = this.name();
    }

    Tools5eIndexType(String templateName) {
        this.templateName = templateName;
    }

    public String templateName() {
        return templateName;
    }

    public static Tools5eIndexType getTypeFromKey(String key) {
        String typeKey = key.substring(0, key.indexOf("|"));
        return valueOf(typeKey);
    }
}
