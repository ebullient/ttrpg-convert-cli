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
    other,
    trait,
    namelist;

    public boolean isOptional() {
        return this == subclass || this == subclassfeature || this == optionalfeature;
    }

    public static IndexType fromKey(String key) {
        for (IndexType t : values()) {
            if (key.startsWith(t.name() + "|")) {
                return t;
            }
        }
        return null;
    }

    /**
     * When used as a filter, does the given type "match"?
     * (specifically, a subrace counts as a race)
     *
     * @param type IndexType to compare
     * @return true if specified type "matches" this type
     */
    public boolean matches(IndexType type) {
        if (this == race && type == subrace) {
            return true;
        }
        return type == this;
    }
}
