package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;
import dev.ebullient.convert.tools.pf2e.qute.QuteTrait;
import dev.ebullient.convert.tools.pf2e.qute.QuteTraitIndex;

public class Json2QuteTrait extends Json2QuteBase {

    public Json2QuteTrait(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.trait, rootNode);
    }

    @Override
    protected QuteTrait buildQuteResource() {
        Tags tags = new Tags(sources);
        List<String> text = new ArrayList<>();
        List<String> categories = new ArrayList<>();

        Field.categories.getListOfStrings(rootNode, tui()).forEach(c -> {
            tags.add("trait", "category", c);

            JsonNode implied = TraitField.implies.getFrom(rootNode);
            if (implied != null) {
                implied.fieldNames().forEachRemaining(n -> {
                    if ("spell".equals(n.toLowerCase())) {
                        String school = implied.get(n).get("_fSchool").asText();
                        tags.add("trait", "category", "spell", school);
                        categories.add(String.format("%s (%s)", c, school));
                    } else {
                        tags.add("trait", "category", n);
                    }
                });
            } else {
                categories.add(c);
            }
        });

        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        return new QuteTrait(sources, text, tags, List.of(), categories);
    }

    static Pf2eQuteNote buildIndex(Pf2eIndex index) {
        Pf2eSources sources = Pf2eSources.constructSyntheticSource("Trait Index");

        return new QuteTraitIndex(sources, index.categoryTraitMap());
    }

    enum TraitField implements JsonNodeReader {
        implies,
    }
}
