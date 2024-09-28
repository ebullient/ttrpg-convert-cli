package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.PsionicType.PsionicTypeEnum;
import dev.ebullient.convert.tools.dnd5e.Tools5eHomebrewIndex.HomebrewMetaTypes;
import dev.ebullient.convert.tools.dnd5e.qute.QutePsionic;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QutePsionicTalent extends Json2QuteCommon {

    Json2QutePsionicTalent(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());

        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, "##");

        return new QutePsionic(sources,
                getName(),
                getSourceText(sources),
                getPsionicTypeOrder(),
                PsionicFields.focus.getTextOrDefault(rootNode, "\u2014"),
                getPsionicModes(),
                String.join("\n", text),
                tags);
    }

    String getPsionicTypeOrder() {
        String order = PsionicFields.order.replaceTextFrom(rootNode, this);
        HomebrewMetaTypes meta = index.getHomebrewMetaTypes(sources);

        String typeName = PsionicFields.type.getTextOrDefault(rootNode, "\u2014");
        PsionicType type = switch (typeName) {
            case "D" -> PsionicTypeEnum.Discipline;
            case "T" -> PsionicTypeEnum.Talent;
            default -> meta.getPsionicType(typeName);
        };

        return type == null ? order : type.combineWith(order);
    }

    Collection<NamedText> getPsionicModes() {
        List<NamedText> traits = new ArrayList<>();
        for (JsonNode e : PsionicFields.modes.iterateArrayFrom(rootNode)) {
            String name = getModeName(e);
            List<String> text = collectEntries(e);

            if (PsionicFields.submodes.existsIn(e)) {
                maybeAddBlankLine(text);
                for (JsonNode submode : PsionicFields.submodes.iterateArrayFrom(e)) {
                    String submodeName = getModeName(submode);
                    List<String> submodeText = collectEntries(submode);
                    prependField(submodeName, submodeText);

                    if (submodeText.size() > 0) {
                        text.add("- " + submodeText.get(0) + "  ");
                        submodeText.remove(0);
                        submodeText.forEach(x -> text.add(x.isEmpty() ? "" : "    " + x + "  "));
                    }
                }
            }

            traits.add(new NamedText(name, String.join("\n", text)));
        }
        return traits;
    }

    String getModeName(JsonNode mode) {
        String name = SourceField.name.replaceTextFrom(mode, this);
        List<String> amendWith = new ArrayList<>();
        if (PsionicFields.cost.existsIn(mode)) {
            JsonNode cost = PsionicFields.cost.getFrom(mode);
            int max = PsionicFields.max.intOrThrow(cost);
            int min = PsionicFields.min.intOrThrow(cost);
            if (max == min) {
                amendWith.add(max + " psi");
            } else {
                amendWith.add(min + "-" + max + " psi");
            }
        }
        if (PsionicFields.concentration.existsIn(mode)) {
            JsonNode concentration = PsionicFields.concentration.getFrom(mode);
            amendWith.add(String.format("conc., %s %s",
                    PsionicFields.duration.intOrThrow(concentration),
                    PsionicFields.unit.getTextOrThrow(concentration)));
        }

        return amendWith.isEmpty() ? name
                : (name + " (" + String.join("; ", amendWith) + ")");
    }

    enum PsionicFields implements JsonNodeReader {
        concentration,
        cost,
        duration,
        focus,
        max,
        min,
        modes,
        order,
        submodes,
        type,
        unit
    }
}
