package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Json2QuteAbility.Pf2eAbility;
import dev.ebullient.convert.tools.pf2e.Json2QuteAffliction.Pf2eAffliction;
import dev.ebullient.convert.tools.pf2e.qute.QuteAbilityOrAffliction;
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
                Pf2eHazard.defenses.getObjectFrom(rootNode)
                        .map(n -> Pf2eDefenses.createInlineDefenses(n, this))
                        .orElse(null),
                Pf2eHazard.attacks.streamFrom(rootNode)
                        .map(n -> Pf2eAttack.createInlineAttack(n, this))
                        .toList(),
                Pf2eHazard.abilities.streamFrom(rootNode)
                        .map(n -> Pf2eAbility.createEmbeddedAbility(n, this))
                        .toList(),
                Pf2eHazard.actions.streamFrom(rootNode)
                        .map(n -> Pf2eAffliction.isAfflictionBlock(n)
                                ? Pf2eAffliction.createInlineAffliction(n, this)
                                : (QuteAbilityOrAffliction) Pf2eAbility.createEmbeddedAbility(n, this))
                        .toList(),
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
                    bonus.getIntFrom(node).orElse(null),
                    dc.getIntFrom(node).orElse(null),
                    minProf.getTextOrNull(node),
                    notes.getTextFrom(node).map(convert::replaceText).orElse(null));
        }

        static QuteDataGenericStat.SimpleStat buildPerception(JsonNode node, JsonTextConverter<?> convert) {
            return new QuteDataGenericStat.SimpleStat(
                    bonus.getIntOrThrow(node),
                    notes.getTextFrom(node).map(convert::replaceText).orElse(null));
        }
    }
}
