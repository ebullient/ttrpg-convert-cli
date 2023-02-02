package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;
import dev.ebullient.convert.tools.pf2e.qute.QuteAbility;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses;
import dev.ebullient.convert.tools.pf2e.qute.QuteHazard;

public class Json2QuteHazard extends Json2QuteBase {

    public Json2QuteHazard(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.hazard, rootNode);
    }

    @Override
    protected QuteHazard buildQuteResource() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Pf2eHazard.description.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        return new QuteHazard(sources, text, tags,
                collectTraitsFrom(rootNode, tags),
                Pf2eHazard.level.getTextOrDefault(rootNode, "0"),
                Pf2eHazard.disable.transformTextFrom(rootNode, "\n", index),
                Pf2eHazard.reset.transformTextFrom(rootNode, "\n", index),
                Pf2eHazard.routine.transformTextFrom(rootNode, "\n", index),
                buildDefenses(),
                buildAbilities(),
                buildActions(),
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

    QuteDataDefenses buildDefenses() {
        JsonNode defenseNode = Pf2eHazard.defenses.getFrom(rootNode);
        if (defenseNode == null) {
            return null;
        }
        return Pf2eDefenses.createInlineDefenses(defenseNode, this);
    }

    List<String> buildAbilities() {
        List<QuteAbility> inlineAbilities = new ArrayList<>();
        Pf2eHazard.abilities.withArrayFrom(rootNode)
                .forEach(a -> inlineAbilities.add(Pf2eTypeAbility.createAbility(a, this, true)));

        return inlineAbilities.stream()
                .map(x -> render(x, Pf2eIndexType.ability))
                .collect(Collectors.toList());
    }

    List<String> buildActions() {
        List<Pf2eQuteNote> inlineThings = new ArrayList<>();
        Pf2eHazard.actions.withArrayFrom(rootNode)
                .forEach(a -> {
                    if (AppendTypeValue.attack.isValueOfField(a, Field.type)) {
                        inlineThings.add(AttackField.createInlineAttack(a, this));
                    } else {
                        inlineThings.add(Pf2eTypeAbility.createAbility(a, this, true));
                    }
                });

        return inlineThings.stream()
                .map(x -> render(x, x.type))
                .collect(Collectors.toList());
    }

    String render(Pf2eQuteNote embeddedElement, Pf2eIndexType type) {
        List<String> inner = new ArrayList<>();
        if (type == Pf2eIndexType.ability) {
            renderEmbeddedTemplate(inner, embeddedElement, type.name(), List.of());
        } else {
            String admonition = type == null ? null : (type == Pf2eIndexType.syntheticGroup ? "attack" : type.name());
            renderInlineTemplate(inner, embeddedElement, admonition);
        }
        return String.join("\n", inner);
    }

    enum Pf2eHazard implements NodeReader {
        abilities,
        actions,
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
