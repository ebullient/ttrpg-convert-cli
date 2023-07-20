package dev.ebullient.convert.tools.dnd5e;

public interface SpellSchool {

    String name();

    record CustomSpellSchool(String name) implements SpellSchool {
    }

    enum SchoolEnum implements SpellSchool {
        Abjuration,
        Conjuration,
        Divination,
        Enchantment,
        Evocation,
        Illusion,
        Necromancy,
        Transmutation,
        None
    }

    static SpellSchool fromEncodedValue(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return switch (v) {
            case "A" -> SchoolEnum.Abjuration;
            case "C" -> SchoolEnum.Conjuration;
            case "D" -> SchoolEnum.Divination;
            case "E", "EN" -> SchoolEnum.Enchantment;
            case "V", "EV" -> SchoolEnum.Evocation;
            case "I" -> SchoolEnum.Illusion;
            case "N" -> SchoolEnum.Necromancy;
            case "T" -> SchoolEnum.Transmutation;
            default -> null;
        };
    }
}
