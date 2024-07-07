package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataGenericStat;
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
                Pf2eHazard.defenses.getDefensesFrom(rootNode, this),
                Pf2eHazard.attacks.getAttacksFrom(rootNode, this),
                Pf2eHazard.abilities.streamFrom(rootNode)
                        .map(n -> new Json2QuteAbility(index, n, true).buildQuteNote())
                        .toList(),
                Pf2eHazard.actions.getAbilityOrAfflictionsFrom(rootNode, this),
                Pf2eHazard.stealth.getObjectFrom(rootNode)
                        .map(n -> Pf2eHazardAttribute.buildStealth(n, this)).orElse(null),
                Pf2eHazard.perception.getObjectFrom(rootNode)
                        .map(n -> Pf2eHazardAttribute.buildPerception(n, this)).orElse(null));
    }

    enum Pf2eHazard implements Pf2eJsonNodeReader {
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

    enum Pf2eHazardAttribute implements Pf2eJsonNodeReader {
        dc,
        bonus,
        minProf,
        notes;

        static QuteHazard.QuteHazardStealth buildStealth(JsonNode node, JsonTextConverter<?> convert) {
            return new QuteHazard.QuteHazardStealth(
                    bonus.intOrNull(node),
                    dc.intOrNull(node),
                    minProf.getTextOrNull(node),
                    notes.getTextFrom(node).map(convert::replaceText).orElse(null));
        }

        static QuteDataGenericStat.SimpleStat buildPerception(JsonNode node, JsonTextConverter<?> convert) {
            return new QuteDataGenericStat.SimpleStat(
                    bonus.intOrThrow(node),
                    notes.getTextFrom(node).map(convert::replaceText).orElse(null));
        }
    }
}
