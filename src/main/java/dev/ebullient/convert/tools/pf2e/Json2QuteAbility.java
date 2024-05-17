package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteAbility;

import java.util.ArrayList;
import java.util.List;

public class Json2QuteAbility extends Json2QuteBase {

    public Json2QuteAbility(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected QuteAbility buildQuteNote() {
        return Pf2eAbility.createAbility(rootNode, this, false);
    }

    public enum Pf2eAbility implements JsonNodeReader {
        activity,
        components,
        cost,
        creature,
        frequency,
        note,
        range,
        requirements,
        trigger,
        special;

        public static QuteAbility createEmbeddedAbility(JsonNode node, JsonSource convert) {
            return createAbility(node, convert, true);
        }

        private static QuteAbility createAbility(JsonNode node, JsonSource convert, boolean embedded) {
            boolean pushed = convert.parseState().push(node);
            try {
                String name = SourceField.name.getTextOrDefault(node, "Activate");

                List<String> abilityText = new ArrayList<>();
                convert.appendToText(abilityText, SourceField.entries.getFrom(node), null);

                range.debugIfExists(node, convert.tui());

                final String abilitySrc = convert.parseState().getSource(Pf2eIndexType.ability);

                // handle abilities in entries
                String freq = convert.index().getFrequency(frequency.getFrom(node));
                if (freq == null) {
                    freq = convert.index().getFrequency(node);
                }

                Tags tags = new Tags();
                return new QuteAbility(
                        name, abilityText, tags, convert.collectTraitsFrom(node, tags),
                        Pf2eTypeReader.getQuteActivity(node, Pf2eFeat.activity, convert),
                        components.transformTextFrom(node, ", ", convert),
                        requirements.replaceTextFrom(node, convert),
                        cost.replaceTextFrom(node, convert),
                        trigger.replaceTextFrom(node, convert),
                        freq,
                        special.replaceTextFrom(node, convert),
                        note.replaceTextFrom(node, convert),
                        embedded) {
                    @Override
                    public String targetFile() {
                        if (!indexType().defaultSource().sameSource(abilitySrc)) {
                            return getName() + "-" + abilitySrc;
                        }
                        return getName();
                    }
                };
            } finally {
                convert.parseState().pop(pushed);
            }
        }
    }
}
