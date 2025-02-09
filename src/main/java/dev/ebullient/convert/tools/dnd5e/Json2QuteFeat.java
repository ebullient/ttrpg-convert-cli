package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.joinConjunct;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Msg;
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
        QuteFeat feat = new QuteFeat(sources,
                type.decoratedName(rootNode),
                getSourceText(sources),
                listPrerequisites(rootNode),
                null, // Level coming someday..
                images,
                String.join("\n", text),
                tags,
                getAbilityScoreIncreases(abilityNode));

        tui().debugf("output: %s", feat.toString());

        return feat;
    }

    enum FeatFields implements JsonNodeReader {
        ability,
        additionalSpells,
        category,
        ;
    }

    public String getAbilityScoreIncreases(JsonNode abilityNode) {
        List<String> abilityScoreIncreases = new ArrayList<>();
        JsonNode scoreIncreases = ensureArray(abilityNode);

        for (JsonNode scoreIncrease : scoreIncreases) {
            Integer max = Optional.ofNullable(scoreIncrease.findValue("max"))
                    .map(value -> value.asInt())
                    .orElse(null);
            Boolean hasMaxValue = max != null;

            List<String> keys = streamOfFieldNames(scoreIncrease).toList();

            List<AbilityScoreIncreaseFields> fields = keys.stream().map(x -> {
                return abilityScoreIncreaseFieldFromString(x);
            }).toList();

            for (AbilityScoreIncreaseFields field : fields) {
                switch (field) {
                    case str -> abilityScoreIncreases.add(String.format(
                            "Increase %s by %s.",
                            field,
                            AbilityScoreIncreaseFields.str.getFrom(scoreIncrease)));
                    case dex -> abilityScoreIncreases.add(String.format(
                            "Increase %s by %s.",
                            field,
                            AbilityScoreIncreaseFields.dex.getFrom(scoreIncrease)));

                    case con -> abilityScoreIncreases.add(String.format(
                            "Increase %s by %s.",
                            field,
                            AbilityScoreIncreaseFields.con.getFrom(scoreIncrease)));

                    case intel -> abilityScoreIncreases.add(String.format(
                            "Increase %s by %s.",
                            field,
                            AbilityScoreIncreaseFields.intel.getFrom(scoreIncrease)));

                    case wis -> abilityScoreIncreases.add(String.format(
                            "Increase %s by %s.",
                            field,
                            AbilityScoreIncreaseFields.wis.getFrom(scoreIncrease)));

                    case cha -> abilityScoreIncreases.add(String.format(
                            "Increase %s by %s.",
                            field,
                            AbilityScoreIncreaseFields.cha.getFrom(scoreIncrease)));

                    case choose -> abilityScoreIncreases
                            .add(getAbilityScoreIncreaseWithOptions(
                                    AbilityScoreIncreaseFields.choose.getFrom(scoreIncrease),
                                    hasMaxValue ? max : null));
                    default -> {
                    }
                }
            }
        }

        if (abilityScoreIncreases.size() == 0)
            return null;

        return String.join("\n- ", abilityScoreIncreases);
    }

    public String getAbilityScoreIncreaseWithOptions(JsonNode chooseNode, Integer max) {
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
        Boolean hasMaxValue = max != null;

        if (options.size() == 6) {
            return String.format("Increase one ability score of your choice by %s%s.",
                    amount,
                    hasMaxValue ? String.format(", to a maximum of %s", max) : "");

        }

        return String.format("Increase your %s by %s%s.",
                joinConjunct(", ", " or ", options),
                amount,
                hasMaxValue ? String.format(", to a maximum of %s", max) : "");
    }
}
