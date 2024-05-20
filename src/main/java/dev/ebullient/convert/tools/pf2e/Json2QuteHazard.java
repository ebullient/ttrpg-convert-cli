package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Json2QuteAbility.Pf2eAbility;
import dev.ebullient.convert.tools.pf2e.Json2QuteAffliction.Pf2eAffliction;
import dev.ebullient.convert.tools.pf2e.qute.QuteAbility;
import dev.ebullient.convert.tools.pf2e.qute.QuteHazard;

public class Json2QuteHazard extends Json2QuteBase {

    public Json2QuteHazard(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.hazard, rootNode);
    }

    @Override
    protected QuteHazard buildQuteResource() {
        Tags tags = new Tags(sources);
        List<String> text = new ArrayList<>();

        appendToText(text, Pf2eHazard.description.getFrom(rootNode), "##");

        return new QuteHazard(sources, text, tags,
                collectTraitsFrom(rootNode, tags),
                Pf2eHazard.level.getTextOrDefault(rootNode, "0"),
                Pf2eHazard.disable.transformTextFrom(rootNode, "\n", index),
                Pf2eHazard.reset.transformTextFrom(rootNode, "\n", index),
                Pf2eHazard.routine.transformTextFrom(rootNode, "\n", index),
                Pf2eHazard.defenses.getObjectFrom(rootNode)
                        .map(n -> Pf2eDefenses.createInlineDefenses(n, this))
                        .orElse(null),
                Pf2eHazard.attacks.streamFrom(rootNode)
                        .map(n -> Pf2eAttack.createInlineAttack(n, this))
                        .toList(),
                renderAbilities(Pf2eHazard.abilities),
                renderAbilities(Pf2eHazard.actions),
                buildAttributes(Pf2eHazard.stealth),
                buildAttributes(Pf2eHazard.perception));
    }

    QuteHazard.QuteHazardAttributes buildAttributes(Pf2eHazard field) {
        QuteHazard.QuteHazardAttributes attr = field.fieldFromTo(rootNode,
                QuteHazard.QuteHazardAttributes.class, tui());
        if (attr != null) {
            attr.notes = replaceText(attr.notes);
        }
        return attr;
    }

    List<String> renderAbilities(Pf2eHazard field) {
        return field.streamFrom(rootNode)
                .map(n -> Pf2eAffliction.isAfflictionBlock(n)
                        ? Pf2eAffliction.createInlineAffliction(n, this)
                        : Pf2eAbility.createEmbeddedAbility(n, this))
                .map(obj -> obj instanceof QuteAbility
                        ? ((QuteAbility) obj).render()
                        : renderInlineTemplate(obj, obj.indexType().name()))
                .toList();
    }

    enum Pf2eHazard implements JsonNodeReader {
        abilities,
        actions,
        attacks,
        defenses,
        description,
        disable,
        level,
        perception,
        reset,
        routine,
        stealth,
    }
}
