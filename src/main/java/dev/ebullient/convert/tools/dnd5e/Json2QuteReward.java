package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteReward;

public class Json2QuteReward extends Json2QuteCommon {

    Json2QuteReward(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected QuteReward buildQuteResource() {
        Tags tags = new Tags(getSources());

        for (String type : SourceField.type.getListOfStrings(rootNode, tui())) {
            tags.add("reward", type);
        }

        List<ImageRef> images = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.rewardFluff, "##", images);
        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        List<String> details = new ArrayList<>();
        String type = RewardFields.type.getTextOrNull(rootNode);
        if (type != null) {
            details.add(type);
        }
        String rarity = RewardFields.rarity.getTextOrNull(rootNode);
        if (rarity != null) {
            details.add(rarity);
        }
        String detail = String.join(", ", details);

        return new QuteReward(getSources(),
                getName(),
                getSourceText(getSources()),
                RewardFields.ability.transformTextFrom(rootNode, "\n", index),
                getName().startsWith(detail) ? "" : detail,
                RewardFields.signaturespells.transformTextFrom(rootNode, "\n", index),
                images,
                String.join("\n", text),
                tags);
    }

    enum RewardFields implements JsonNodeReader {
        ability,
        rarity,
        signaturespells,
        type
    }
}
