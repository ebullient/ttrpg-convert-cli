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
        return Pf2eAbility.createAbility(rootNode, this, sources);
    }

    public enum Pf2eAbility implements JsonNodeReader {
        name,
        activity,
        components,
        cost,
        creature,
        frequency,
        note,
        range,
        requirements,
        trigger,
        special,
        entries;

        public static QuteAbility createEmbeddedAbility(JsonNode node, JsonSource convert) {
            return createAbility(node, convert, null);
        }

        private static QuteAbility createAbility(JsonNode node, JsonSource convert, Pf2eSources sources) {
            Tags tags = new Tags();
            return new QuteAbility(
                    sources,
                    name.getTextOrDefault(node, "Activate"),
                    entries.transformTextFrom(node, "\n", convert),
                    tags,
                    convert.collectTraitsFrom(node, tags),
                    Pf2eTypeReader.getQuteActivity(node, Pf2eFeat.activity, convert),
                    components.transformTextFrom(node, ", ", convert),
                    requirements.replaceTextFrom(node, convert),
                    cost.replaceTextFrom(node, convert),
                    trigger.replaceTextFrom(node, convert),
                    Pf2eFrequency.getFrequency(frequency.getFrom(node), convert),
                    special.replaceTextFrom(node, convert),
                    note.replaceTextFrom(node, convert),
                    sources == null, convert);
        }
    }
}
