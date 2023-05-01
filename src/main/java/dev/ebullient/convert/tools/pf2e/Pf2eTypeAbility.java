package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.JsonSource.Field;
import dev.ebullient.convert.tools.pf2e.qute.QuteAbility;

public enum Pf2eTypeAbility implements NodeReader {
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

    public static QuteAbility createAbility(JsonNode node, JsonSource convert, boolean embedded) {
        boolean pushed = convert.parseState.push(node);
        try {
            String name = Field.name.getTextOrDefault(node, "Activate");

            List<String> abilityText = new ArrayList<>();
            convert.appendEntryToText(abilityText, Field.entries.getFrom(node), null);

            note.debugIfExists(node, convert.tui());
            range.debugIfExists(node, convert.tui());

            final String abilitySrc = convert.parseState.getSource(Pf2eIndexType.ability);

            List<String> tags = new ArrayList<>();
            return new QuteAbility(
                    name, abilityText, tags, convert.collectTraitsFrom(node, tags),
                    Pf2eTypeReader.getQuteActivity(node, Pf2eTypeReader.Pf2eFeat.activity, convert),
                    components.replaceTextFrom(node, convert),
                    requirements.replaceTextFrom(node, convert),
                    cost.replaceTextFrom(node, convert),
                    trigger.replaceTextFrom(node, convert),
                    convert.index().getFrequency(frequency.getFrom(node)),
                    special.replaceTextFrom(node, convert),
                    embedded) {
                @Override
                public String targetFile() {
                    if (!type.defaultSource().sameSource(abilitySrc)) {
                        return getName() + "-" + abilitySrc;
                    }
                    return getName();
                }
            };
        } finally {
            JsonTextReplacement.parseState.pop(pushed);
        }
    }
}
