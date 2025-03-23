package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.joinConjunct;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteFeat;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteFeat extends Json2QuteCommon {
    public Json2QuteFeat(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());
        tags.add("feat");

        List<ImageRef> images = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.featFluff, "##", images);
        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        JsonNode abilityNode = FeatFields.ability.getFrom(rootNode);

        // TODO: update w/ category, additionalSpells
        return new QuteFeat(sources,
                type.decoratedName(rootNode),
                getSourceText(sources),
                listPrerequisites(rootNode),
                null, // Level coming someday..
                getAbilityScoreIncreases(abilityNode),
                images,
                String.join("\n", text),
                tags);
    }

    public String getAbilityScoreIncreases(JsonNode abilityNode) {
        List<String> abilityScoreIncreases = new ArrayList<>();
        JsonNode scoreIncreases = ensureArray(abilityNode);

        for (JsonNode scoreIncrease : scoreIncreases) {
            Integer max = Optional.ofNullable(scoreIncrease.findValue("max"))
                    .map(value -> value.asInt())
                    .orElse(null);

            Integer maximumScore = max != null ? max : 20;

            List<String> keys = streamOfFieldNames(scoreIncrease).toList();

            List<AbilityScoreIncreaseFields> fields = keys.stream().map(x -> {
                return abilityScoreIncreaseFieldFromString(x);
            }).toList();

            for (AbilityScoreIncreaseFields field : fields) {
                switch (field) {
                    case str, dex, con, intel, wis, cha -> {
                        String increase = field.getTextOrEmpty(scoreIncrease);
                        abilityScoreIncreases.add("Increase your %s by %s, to a maximum of %s."
                                .formatted(field.longName(), increase, maximumScore));
                    }
                    case choose -> abilityScoreIncreases
                            .add(getAbilityScoreIncreaseWithOptions(
                                    AbilityScoreIncreaseFields.choose.getFrom(scoreIncrease),
                                    maximumScore));
                    default -> {
                    }
                }
            }
        }

        if (abilityScoreIncreases.size() == 0)
            return null;

        return String.join("\n- ", abilityScoreIncreases);
    }

    public String getAbilityScoreIncreaseWithOptions(JsonNode chooseNode, Integer maximumScore) {
        if (chooseNode == null) {
            return null;
        }

        JsonNode entry = chooseNode.findValue("entry");
        if (entry != null) {
            return entry.asText();
        }

        List<String> options = toListOfStrings(chooseNode.get("from"));
        Integer amount = Optional.ofNullable(
                chooseNode.get("amount")).map(x -> x.asInt()).orElse(1);

        if (options.size() == 6) {
            return String.format("Increase one ability score of your choice by %s, to a maximum of %s.",
                    amount,
                    maximumScore);
        }

        return String.format("Increase your %s by %s, to a maximum of %s.",
                joinConjunct(", ", " or ", options),
                amount,
                maximumScore);
    }

    enum FeatFields implements JsonNodeReader {
        ability,
        additionalSpells,
        category,
        ;
    }
}
