package dev.ebullient.convert.tools.pf2e;

import java.util.stream.Stream;

public interface Pf2eTypeReader extends JsonSource {

    enum Pf2eAlignmentValue implements Pf2eJsonNodeReader.FieldValue {
        ce("Chaotic Evil"),
        cg("Chaotic Good"),
        cn("Chaotic Neutral"),
        le("Lawful Evil"),
        lg("Lawful Good"),
        ln("Lawful Neutral"),
        n("Neutral"),
        ne("Neutral Evil"),
        ng("Neutral Good");

        final String longName;

        Pf2eAlignmentValue(String s) {
            longName = s;
        }

        @Override
        public String value() {
            return this.name();
        }

        @Override
        public boolean matches(String value) {
            return this.value().equalsIgnoreCase(value) || this.longName.equalsIgnoreCase(value);
        }

        public static Pf2eAlignmentValue fromString(String name) {
            if (name == null) {
                return null;
            }
            return Stream.of(Pf2eAlignmentValue.values())
                    .filter(t -> t.matches(name))
                    .findFirst().orElse(null);
        }
    }

}
