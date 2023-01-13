package dev.ebullient.convert.tools5e;

public enum SchoolEnum {
    Abjuration,
    Conjuration,
    Divination,
    Enchantment,
    Evocation,
    Illusion,
    Necromancy,
    Transmutation,
    None;

    public static SchoolEnum fromShortcode(String v) {
        if (v == null || v.isBlank()) {
            return None;
        }
        switch (v) {
            case "A":
                return Abjuration;
            case "C":
                return Conjuration;
            case "D":
                return Divination;
            case "E":
            case "EN":
                return Enchantment;
            case "V":
            case "EV":
                return Evocation;
            case "I":
                return Illusion;
            case "N":
                return Necromancy;
            case "T":
                return Transmutation;
            default:
                throw new IllegalArgumentException("Invalid/Unknown school " + v);
        }
    }
}
