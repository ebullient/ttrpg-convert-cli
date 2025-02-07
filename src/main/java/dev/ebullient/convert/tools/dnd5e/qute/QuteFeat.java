package dev.ebullient.convert.tools.dnd5e.qute;

import static dev.ebullient.convert.StringUtil.joinConjunct;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.SkillOrAbility;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools feat and optional feat attributes ({@code feat2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteFeat extends Tools5eQuteBase {

    /** Prerequisite level */
    public final String level;
    /** Formatted text listing other prerequisite conditions (optional) */
    public final String prerequisite;

    /** Formatted text listing ability score increase (optional) */
    public final String ability;

    public QuteFeat(Tools5eSources sources, String name, String source,
            String prerequisite, String level,
            List<ImageRef> images, String text, Tags tags, String ability) {
        super(sources, name, source, images, text, tags);
        withTemplate("feat2md.txt"); // Feat and OptionalFeature
        this.level = level;
        this.prerequisite = prerequisite; // optional
        this.ability = ability; // optional
    }

    public void abilityScoreIncreases(JsonNode abilityNode) {
        if (abilityNode.has("choose.from")) {
            Tui.instance().debugf("has options");

            return;
        }

    }

    // public String abilityScoreOptions(Collection<String> abilities, int numAbilities) {
    //     if (abilities.isEmpty() || abilities.size() >= 6) {
    //         String abilityString = "||%s abilities".formatted(numAbilities == 1 ? "of your" : "");
    //         return "**Ability Score Increase**: Increase %s %s, up to a maximum of 20".formatted(numAbilities, abilityString);
    //     }

    //     List<String> formatted = abilities.stream().map(x -> sources.findSkillOrAbility(x.toUpperCase(), getSources()))
    //             .filter(x -> x != null)
    //             .sorted(SkillOrAbility.comparator)
    //             .map(x -> linkifySkill(x))
    //             .toList();

    //     return "**Ability Score Increase**: Increase your %s %s".formatted(joinConjunct(" or ", formatted), numAbilities);
    // }

    public enum FeatAbility {
        str,
        dex,
        con,
        wis,
        cha,
        choose
    }
}
