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
        List<AbilityIncrease> increases = abilityScoreIncreases(rootNode.get("ability"));

        String ability = "";

        for (AbilityIncrease ai : increases) {
            ability += "\n" + abilityScoreOptions(ai.names, ai.amount);
        }

        // TODO: update w/ category, additionalSpells
        QuteFeat feat = new QuteFeat(sources,
                type.decoratedName(rootNode),
                getSourceText(sources),
                listPrerequisites(rootNode),
                null, // Level coming someday..
                images,
                String.join("\n", text),
                tags,
                ability);

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
}
