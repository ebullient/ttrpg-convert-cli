package dev.ebullient.convert.tools.dnd5e;

import io.quarkus.qute.TemplateData;

@TemplateData
public enum ItemTypeGroup {
    ammunition,
    armor,
    gear,
    shield,
    treasure,
    vehicle,
    weapon,
    wondrous;

    public boolean hasGroup(ItemType type, ItemType typeAlt) {
        return (type != null && this == type.group())
                || (typeAlt != null && this == typeAlt.group());
    }
}
