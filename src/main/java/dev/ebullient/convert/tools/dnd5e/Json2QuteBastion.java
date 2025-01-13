package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteBastion;
import dev.ebullient.convert.tools.dnd5e.qute.QuteBastion.Hireling;
import dev.ebullient.convert.tools.dnd5e.qute.QuteBastion.Space;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteBastion extends Json2QuteCommon {

    Map<String, Space> spaceMap = new HashMap<>();

    Json2QuteBastion(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());
        tags.add("bastion");

        List<ImageRef> fluffImages = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.facilityFluff, "##", fluffImages);
        appendToText(text, rootNode, "##");

        String type = BastionFields.facilityType.getTextOrThrow(rootNode);

        String prereqs = "";
        if (BastionFields.prerequisite.existsIn(rootNode)) {
            prereqs = listPrerequisites(rootNode);
        } else if (!"basic".equals(type)) {
            prereqs = "None";
        }

        List<Hireling> hirelings = new ArrayList<>();
        for (JsonNode h : BastionFields.hirelings.iterateArrayFrom(rootNode)) {
            hirelings.add(new Hireling(
                    BastionFields.exact.intOrNull(h),
                    BastionFields.min.intOrNull(h),
                    BastionFields.max.intOrNull(h),
                    spaceForName(BastionFields.space.getTextOrEmpty(h))));
        }

        List<Space> spaces = new ArrayList<>();
        for (JsonNode s : BastionFields.space.iterateArrayFrom(rootNode)) {
            Space space = spaceForName(s.asText());
            if (space == null) {
                // TODO: At some point, there will be a custom bastion space..
                tui().warnf(Msg.UNRESOLVED, "Bastion space %s not found (%s)", s, getSources().getKey());
            } else {
                spaces.add(space);
            }
        }

        return new QuteBastion(
                sources,
                getName(),
                getSourceText(getSources()),
                hirelings,
                BastionFields.level.getTextOrEmpty(rootNode),
                BastionFields.orders.getListOfStrings(rootNode, tui()),
                prereqs,
                spaces,
                type,
                String.join("\n", text),
                fluffImages,
                tags);
    }

    private Space spaceForName(String name) {
        if (!isPresent(name)) {
            return null;
        }
        if (spaceMap.isEmpty()) {
            Space cramped = new Space("Cramped", 4, 500, 20);
            Space roomy = new Space("Roomy", 16, 1000, 45, cramped);
            Space vast = new Space("Vast", 36, 3000, 125, roomy);
            spaceMap.put("cramped", cramped);
            spaceMap.put("roomy", roomy);
            spaceMap.put("vast", vast);
        }
        return spaceMap.get(name.toLowerCase());
    }

    enum BastionFields implements JsonNodeReader {
        exact,
        facilityType,
        hirelings,
        level,
        min,
        max,
        orders,
        space,
        prerequisite,
        fluffImages;
    }
}
