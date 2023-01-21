package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteTrait;
import dev.ebullient.convert.tools.pf2e.qute.QuteTraitIndex;

public class Json2QuteTrait extends Json2QuteBase {

    public Json2QuteTrait(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode, Pf2eSources.findSources(rootNode));
    }

    @Override
    public Pf2eQuteBase build() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();
        List<String> categories = new ArrayList<>();

        Field.categories.getListOfStrings(rootNode, tui()).forEach(c -> {
            tags.add(cfg().traitCategoryTagOf(c));

            JsonNode implied = Field.implies.getFrom(rootNode);
            if (implied != null) {
                implied.fieldNames().forEachRemaining(n -> {
                    if ("spell".equals(n.toLowerCase())) {
                        String school = implied.get(n).get("_fSchool").asText();
                        tags.add(cfg().traitCategoryTagOf("spell", school));
                        categories.add(String.format("%s (%s)", c, school));
                    } else {
                        tags.add(cfg().traitCategoryTagOf(n));
                    }
                });
            } else {
                categories.add(c);
            }
        });

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        return new QuteTrait(
                getSources(),
                getSources().getName(),
                getSources().getSourceText(),
                List.of(index().linkify(Pf2eIndexType.trait, getSources().getName())),
                categories,
                String.join("\n", text),
                tags);
    }

    static QuteNote buildIndex(Pf2eIndex index) {
        Pf2eSources sources = Pf2eSources.constructSyntheticSource("Trait Index");

        return new QuteTraitIndex(sources, index.categoryTraitMap());
    }
}
