package dev.ebullient.convert.tools.dnd5e;

public interface SpellSchool {

    String name();

    String code();

    record CustomSpellSchool(String code, String name) implements SpellSchool {
        @Override
        public String code() {
            return code;
        }

        @Override
        public String name() {
            return name;
        }
    }

    enum SchoolEnum implements SpellSchool {
        Abjuration("A"),
        Conjuration("C"),
        Divination("D"),
        Enchantment("E"),
        Evocation("V"),
        Illusion("I"),
        Necromancy("N"),
        Transmutation("T"),
        Psychic("P"),
        None("_");

        private final String code;

        SchoolEnum(String abbreviation) {
            this.code = abbreviation;
        }

        public String code() {
            return code;
        }
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
            default -> {
                String tolower = v.toLowerCase();
                for (SpellSchool s : SchoolEnum.values()) {
                    if (s.name().toLowerCase().equals(tolower)) {
                        yield s;
                    }
                }
                yield null;
            }
        };
    }
}
