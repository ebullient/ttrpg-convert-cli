package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteFeat;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteFeat extends Json2QuteCommon {
    static final Pattern featPattern = Pattern.compile("([^|]+)\\|?.*");

    public Json2QuteFeat(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        String prerequisite = listPrerequisites();
        Tags tags = new Tags(getSources());

        return new QuteFeat(sources,
                type.decoratedName(rootNode),
                getSourceText(sources),
                prerequisite,
                null, // Level coming someday..
                getText("##"),
                tags);
    }

    String listPrerequisites() {
        List<String> prereqs = new ArrayList<>();
        Tools5eIndex index = index();
        for (JsonNode entry : iterableElements(FeatFields.prerequisite.getFrom(rootNode))) {
            if (FeatFields.level.existsIn(entry)) {
                prereqs.add(levelToText(entry.get("level")));
            }

            for (JsonNode r : iterableElements(FeatFields.race.getFrom(entry))) {
                prereqs.add(index.linkifyByName(Tools5eIndexType.race, raceToText(r)));
            }

            Map<String, List<String>> abilityScores = new HashMap<>();

            for (JsonNode a : iterableElements(FeatFields.ability.getFrom(entry))) {
                for (Entry<String, JsonNode> score : iterableFields(a)) {
                    abilityScores.computeIfAbsent(score.getValue().asText(), k -> new ArrayList<>())
                            .add(SkillOrAbility.format(score.getKey(), index(), getSources()));
                }
            }

            abilityScores.forEach(
                    (k, v) -> prereqs.add(String.format("%s %s or higher", String.join(" or ", v), k)));

            if (FeatFields.spellcasting.existsIn(entry) && FeatFields.spellcasting.booleanOrDefault(entry, false)) {
                prereqs.add("The ability to cast at least one spell");
            }
            if (FeatFields.pact.existsIn(entry)) {
                prereqs.add("Pact of the " + FeatFields.pact.replaceTextFrom(entry, this));
            }
            if (FeatFields.patron.existsIn(entry)) {
                prereqs.add(FeatFields.patron.replaceTextFrom(entry, this) + " Patron");
            }
            FeatFields.spell.streamOf(entry).forEach(s -> {
                String text = s.asText().replaceAll("#c", "");
                prereqs.add(index.linkifyByName(Tools5eIndexType.spell, text));
            });
            FeatFields.feat.streamOf(entry).forEach(f -> prereqs
                    .add(featPattern.matcher(f.asText())
                            .replaceAll(m -> index.linkifyByName(Tools5eIndexType.feat, m.group(1)))));
            FeatFields.feature.streamOf(entry).forEach(f -> prereqs.add(featPattern.matcher(f.asText())
                    .replaceAll(m -> index.linkifyByName(Tools5eIndexType.optionalfeature, m.group(1)))));
            FeatFields.background.streamOf(entry).forEach(b -> prereqs
                    .add(index.linkifyByName(Tools5eIndexType.background, SourceField.name.getTextOrEmpty(b)) + " background"));
            FeatFields.item.streamOf(entry).forEach(i -> prereqs.add(index.linkifyByName(Tools5eIndexType.item, i.asText())));

            if (FeatFields.psionics.existsIn(entry)) {
                prereqs.add("Psionics");
            }

            List<String> profs = new ArrayList<>();
            FeatFields.proficiency.streamOf(entry).forEach(f -> f.fields().forEachRemaining(field -> {
                String key = field.getKey();
                if ("weapon".equals(key)) {
                    key += "s";
                }
                profs.add(String.format("%s %s", field.getValue().asText(), key));
            }));
            if (!profs.isEmpty()) {
                prereqs.add(String.format("Proficiency with %s", String.join(" or ", profs)));
            }

            if (FeatFields.other.existsIn(entry)) {
                prereqs.add(FeatFields.other.replaceTextFrom(entry, this));
            }
        }
        return prereqs.isEmpty() ? null : String.join(", ", prereqs);
    }

    enum FeatFields implements JsonNodeReader {
        ability,
        background,
        feat,
        feature,
        level,
        other,
        pact,
        patron,
        prerequisite,
        psionics,
        proficiency,
        race,
        spell,
        spellcasting,
        item,
    }
}
