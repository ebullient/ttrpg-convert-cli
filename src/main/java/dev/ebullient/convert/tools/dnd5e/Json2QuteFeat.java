package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.joinConjunct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

        /* void abilityIncreases = */
        // List<AbilityIncrease> increases = abilityScoreIncreases(rootNode.get("ability"));

        // String ability = "";

        // for (AbilityIncrease ai : increases) {
        //     ability += "\n" + abilityScoreOptions(ai.names, ai.amount);
        // }

        // List<String> abilityOptions = new ArrayList<>();

        JsonNode abilityNode = FeatFields.ability.getFrom(rootNode);
        // JsonNode a = abilityNode.findValue("from");
        // if (a == null) {
        //     tui().debugf("from wasn't found");
        // }

        // String ability = String.join(", ", this.toListOfStrings(a));
        // tui().infof("ability: %s", ability);

        // for (JsonNode abilityIncrease : abilityNode) {
        //     if (abilityIncrease.hasNonNull("choose") && abilityIncrease.get("choose").hasNonNull("from")) {
        //         tui().infof("assigning ability...");
        //         ability = String.join(", ", FeatFields.ability.getListOfStrings(abilityIncrease, tui()));
        //     }
        // }

        // tui().debugf("Generated ability: %s", ability);

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

    public class AbilityIncrease {
        private final List<String> names;
        private final Integer amount;

        public AbilityIncrease(List<String> names, Integer amount) {
            this.names = names;
            this.amount = amount;
        }

        public List<String> getNames() {
            return names;
        }

        public Integer getAmount() {
            return amount;
        }

        public String toString() {
            return String.format("{names: %s, amount: %s}", names, amount);
        }
    }

    public List<AbilityIncrease> abilityScoreIncreases(JsonNode abilityNode) {
        List<AbilityIncrease> increases = new ArrayList<>();

        for (JsonNode asi : ensureArray(abilityNode)) {
            if (asi != null && asi.hasNonNull("choose")) {
                JsonNode options = asi.get("choose");
                // AbilityIncrease a = new AbilityIncrease("", 2);
                List<String> names = new ArrayList<>();
                options.get("from").forEach(n -> names.add(n.asText()));
                increases.add(new AbilityIncrease(names, 1));
            }
        }

        return increases;

    }

    public String abilityScoreOptions(Collection<String> abilities, int numAbilities) {
        if (abilities.isEmpty() || abilities.size() >= 6) {
            String abilityString = "||%s abilities".formatted(numAbilities == 1 ? "of your" : "");
            return "**Ability Score Increase**: Increase %s %s, up to a maximum of 20".formatted(numAbilities, abilityString);
        }

        List<String> formatted = abilities.stream().map(x -> index.findSkillOrAbility(x.toUpperCase(), getSources()))
                .filter(x -> x != null)
                .sorted(SkillOrAbility.comparator)
                .map(x -> linkifySkill(x))
                .toList();

        return "**Ability Score Increase**: Increase your %s %s".formatted(joinConjunct(" or ", formatted), numAbilities);
    }

    public String getAbilityScoreIncreases(JsonNode abilityNode) {
        List<String> abilityIncreases = new ArrayList<>();

        JsonNode entries = ensureArray(abilityNode);

        for (JsonNode entry : entries) {
            JsonNode choice = AbilityScoreIncrease.choose.getFrom(entry);
            JsonNode amount = entry.findValue("amount");
            JsonNode max = AbilityScoreIncrease.max.getFrom(entry);

            if (choice != null) {
                List<String> options = choice.findValuesAsText("from");
                tui().infof(options.toString());
                // collect this group of options
                if (options.size() == 6) {
                    abilityIncreases.add(
                            String.format("Increase one ability score of your choice by %s%s.",
                                    amount != null ? amount : 1,
                                    max != null ? String.format(", to a maximum of %s", max) : ""));
                    continue;
                }

                abilityIncreases.add(
                        String.format("Increase your %s by %s%s.",
                                // TODO: Fix this to get list of strings
                                String.join(", ", options),
                                amount != null ? amount.asInt() : "1",
                                max != null ? String.format(", to a maximum of %s", max) : ""));

                continue;
            }

            // Otherwise look for named ability increase
            for (AbilityScoreIncrease a : AbilityScoreIncrease.values()) {
                JsonNode b = a.getFrom(entry);
                if (b != null)
                    abilityIncreases.add(String.format("Increase %s by %s", a.toString(), b.asInt()));
                break;
            }
        }

        if (abilityIncreases.size() == 0)
            return null;

        tui().infof("generated ability increases: %s", abilityIncreases);

        return String.join("\n- ", abilityIncreases);
    }

    enum AbilityScoreIncrease implements JsonNodeReader {
        choose,
        str,
        dex,
        con,
        intl,
        wis,
        cha,
        max
    }
}
