package dev.ebullient.convert.tools.dnd5e.qute;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joinConjunct;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools class attributes ({@code class2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteClass extends Tools5eQuteBase {

    /** Formatted string describing the primary abilities for this class */
    public final String primaryAbility;

    /** Hit dice for this class as a single digit: 8 */
    public final int hitDice;

    /** Average Hit dice roll as a single digit */
    public final int hitRollAverage;

    /**
     * Hit point die for this class as
     * {@link dev.ebullient.convert.tools.dnd5e.qute.QuteClass.HitPointDie}
     */
    public final HitPointDie hitPointDie;

    /** Formatted callout containing class and feature progressions. */
    public final String classProgression;

    /**
     * Formatted text describing starting equipment as
     * {@link dev.ebullient.convert.tools.dnd5e.qute.QuteClass.StartingEquipment}
     */
    public final StartingEquipment startingEquipment;

    /**
     * Multiclassing requirements and proficiencies for this class as
     * {@link dev.ebullient.convert.tools.dnd5e.qute.QuteClass.Multiclassing}
     */
    public final Multiclassing multiclassing;

    public QuteClass(Tools5eSources sources, String name, String source,
            String classProgression,
            String primaryAbility, HitPointDie hitPointDie,
            StartingEquipment startingEquipment, Multiclassing multiclassing,
            String text, List<ImageRef> images, Tags tags) {
        super(sources, name, source, images, text, tags);
        this.primaryAbility = primaryAbility;
        this.hitPointDie = hitPointDie;
        // compat with previous version. Sidekicks do not have a hitPointDie
        this.hitDice = hitPointDie == null || hitPointDie.isSidekick()
                ? 0
                : hitPointDie.face();
        this.hitRollAverage = hitPointDie == null || hitPointDie.isSidekick()
                ? 0
                : hitPointDie.average();
        this.classProgression = classProgression;
        this.startingEquipment = startingEquipment;
        this.multiclassing = multiclassing;
    }

    /**
     * Describes the multiclassing information for the class.
     *
     * If referenced as a unit (ignoring inner attributes), it will render
     * formatted text describing multiclassing requirements and proficiencies.
     *
     * @param primaryAbility Primary ability for multiclassing as formatted
     *        string (optional)
     * @param requirements Prerequisites for multiclassing as formatted
     *        string (optional)
     * @param requirementsSpecial Special prerequisites for multiclassing as
     *        formatted string (optional)
     * @param skills Skill proficiencies gained as formatted string
     *        (optional)
     * @param weapons Weapon proficiencies gained as formatted string
     *        (optional)
     * @param tools Tool proficiencies gained as formatted string
     *        (optional)
     * @param armor Armor proficiencies gained as formatted string
     *        (optional)
     * @param text Formatted text describing this multiclass
     *        (optional)
     * @param isClassic True if this class is from the 2014 edition
     */
    @TemplateData
    public record Multiclassing(
            String primaryAbility,
            String requirements,
            String requirementsSpecial,
            String skills,
            String weapons,
            String tools,
            String armor,
            String text,
            boolean isClassic) implements QuteUtil {
        public String prereq() {
            if (isPresent(this.primaryAbility)) {
                return "To qualify for a new class, you must have a score of at least 13 in the primary ability of the new class (%s) and your current classes."
                        .formatted(primaryAbility);
            }
            if (isPresent(requirements)) {
                return requirements;
            }
            return "";
        }

        public String prereqSpecial() {
            if (isPresent(requirementsSpecial)) {
                List<String> content = new ArrayList<>();
                if (isPresent(requirements)) {
                    content.add(
                            "To qualify for a new class, you must meet the %sprerequisites for both your current class and your new one."
                                    .formatted(isPresent(requirementsSpecial) ? "" : "ability score "));
                }
                maybeAddBlankLine(content);
                content.add("**%sPrerequisites:** %s".formatted(
                        isPresent(requirements) ? "Other " : "",
                        requirementsSpecial));
                return String.join("\n", content);
            }
            return "";
        }

        public String profIntro() {
            return "When you gain a level in a class other than your first, you gain only some of that class's starting proficiencies.";
        }

        @Override
        public String toString() {
            boolean hasRequirements = isPresent(primaryAbility) || isPresent(requirements)
                    || isPresent(requirementsSpecial);
            boolean hasProficiencies = isPresent(armor) || isPresent(weapons) || isPresent(tools) || isPresent(skills);

            List<String> content = new ArrayList<>();
            if (hasRequirements) {
                content.add(prereq());
                if (isPresent(requirementsSpecial)) {
                    maybeAddBlankLine(content);
                    content.add(prereqSpecial());
                }
            }
            if (isPresent(text)) {
                maybeAddBlankLine(content);
                content.add(text);
            }
            if (hasProficiencies) {
                if (isPresent(requirements)) {
                    maybeAddBlankLine(content);
                    content.add(profIntro());
                }
                maybeAddBlankLine(content);
                if (isClassic) {
                    if (isPresent(armor)) {
                        content.add("- **Armor**: " + armor);
                    }
                    if (isPresent(weapons)) {
                        content.add("- **Weapons**: " + weapons);
                    }
                    if (isPresent(tools)) {
                        content.add("- **Tools**: " + tools);
                    }
                    if (isPresent(skills)) {
                        content.add("- **Skills**: " + skills);
                    }
                } else {
                    if (isPresent(skills)) {
                        content.add("- **Skill Proficiencies**: " + skills);
                    }
                    if (isPresent(weapons)) {
                        content.add("- **Weapon Proficiencies**: " + weapons);
                    }
                    if (isPresent(tools)) {
                        content.add("- **Tool Proficiencies**: " + tools);
                    }
                    if (isPresent(armor)) {
                        content.add("- **Armor Training**: " + armor);
                    }
                }
            }

            return String.join("\n", content);
        }
    }

    /**
     * Describes the starting equipment for the class.
     *
     * If referenced as a unit (ignoring inner attributes), it will render
     * structured text describing starting proficiencies and equipment *2014* vs
     * *2024*.
     *
     * @param savingThrows List of saving throws
     * @param skills List of skills as formatted strings (links)
     * @param weapons List of weapons as formatted strings (links)
     * @param tools List of tools as formatted strings (links)
     * @param armor List of armor as formatted strings (links)
     * @param equipment List of equipment as formatted strings (links)
     * @param isClassic True if this class is from the 2014 edition
     */
    @TemplateData
    public record StartingEquipment(
            List<String> savingThrows,
            List<String> skills,
            List<String> weapons,
            List<String> tools,
            List<String> armor,
            String equipment,
            boolean isClassic) implements QuteUtil {

        @Override
        public String toString() {
            List<String> text = new ArrayList<>();
            text.add(getProficiencies());
            if (isPresent(equipment)) {
                maybeAddBlankLine(text);
                text.add((isClassic ? "" : "**Starting Equipment:** ") + equipment);
            }
            return String.join("\n", text);
        }

        /** Formatted string of class proficiencies */
        public String getProficiencies() {
            List<String> text = new ArrayList<>();
            if (isClassic) {
                text.add("- **Saving Throws**: " + getJoinOrDefault(savingThrows, null));
                text.add("- **Armor**: " + (isPresent(armor) ? getArmorString() : "none"));
                text.add("- **Weapons**: " + getJoinOrDefault(weapons, isClassic ? null : " and "));
                text.add("- **Tools**: " + getJoinOrDefault(tools, isClassic ? null : " and "));
                text.add("- **Skills**: " + join(" *or* ", skills));
            } else {
                text.add("- **Saving Throw Proficiencies**: " + getJoinOrDefault(savingThrows, null));
                text.add("- **Skill Proficiencies**: " + join(" *or* ", skills));
                text.add("- **Weapon Proficiencies**: " + getJoinOrDefault(weapons, isClassic ? null : " and "));
                if (isPresent(tools)) {
                    text.add("- **Tool Proficiencies**: " + getJoinOrDefault(tools, isClassic ? null : " and "));
                }
                if (isPresent(armor)) {
                    text.add("- **Armor Training**: " + getArmorString());
                }
            }
            return String.join("\n", text);
        }

        /**
         * Create a structured string describing armor training.
         * Slighly different formatting and joining for 2014 vs 2024 materials.
         *
         * @return formatted string with links to armor item types and shield items
         */
        public String getArmorString() {
            if (isClassic) {
                return join(", ", armor);
            }
            List<String> armorLinks = armor.stream()
                    .filter(s -> s.matches("Light|Medium|Heavy"))
                    .collect(Collectors.toCollection(ArrayList::new));
            List<String> otherLinks = armor.stream()
                    .filter(s -> !s.matches("Light|Medium|Heavy"))
                    .toList();
            if (armorLinks.size() > 1) {
                // remove " armor" from all but the last item
                for (int i = 0; i < armorLinks.size() - 1; i++) {
                    armorLinks.set(i, armorLinks.get(i).replace(" armor", ""));
                }
                String joined = joinConjunct("and", armorLinks);
                armorLinks.clear();
                armorLinks.add(joined);
            }
            armorLinks.addAll(otherLinks);
            return joinConjunct(" and ", armorLinks);
        }

        /**
         * Given a list of strings, return a formatted string with a conjunction.
         *
         * @param value List of strings.
         * @param conjunct Conjunction (and, or). If null, elements will be
         *        comma-separated.
         *        Otherwise, the first n elements comma-separated and the last
         *        element will be joined with conjunction.
         * @return Formatted string. If value is empty, will return "none".
         */
        public String getJoinOrDefault(List<String> value, String conjunct) {
            if (value == null || value.isEmpty()) {
                return "none";
            }
            return conjunct == null
                    ? join(", ", value)
                    : joinConjunct(conjunct, value);
        }
    }

    /**
     * Describes the hit point die used by the class.
     *
     * If referenced as a unit (ignoring inner attributes), it will render
     * formatted strings based on the class version (2024 or not).
     *
     * @param number How many dice to roll (pretty much always 1)
     * @param face Die to roll (8, 10); This will be 0 for sidekicks
     * @param average The average value of a hit dice roll
     * @param isClassic True if this is a 2014 class
     * @param isSidekick Explicit test for sidekick (alternate to 0 face)
     */
    @TemplateData
    public record HitPointDie(
            String name,
            int number,
            int face,
            int average,
            boolean isClassic,
            boolean isSidekick) {
        public HitPointDie(String name, int number, int face, boolean isClassic, boolean isSidekick) {
            this(name, number, face, (number * face) / 2 + 1, isClassic, isSidekick);
        }

        @Override
        public String toString() {
            // return
            // `<div><strong>Hit Point Die:</strong>
            // ${renderer.render(Renderer.class.getHitDiceEntry(cls.hd, {styleHint}))} per
            // ${cls.name} level</div>
            // <div><strong>Hit Points at Level 1:</strong>
            // ${Renderer.class.getHitPointsAtFirstLevel(cls.hd, {styleHint})}</div>
            // <div><strong>Hit Points per additional ${cls.name} Level:</strong>
            // ${Renderer.class.getHitPointsAtHigherLevels(cls.name, cls.hd,
            // {styleHint})}</div>`;
            // return styleHint === "classic" -- hit dice entry
            // ? `{@dice ${clsHd.number}d${clsHd.faces}||Hit die}`
            // : `{@dice ${clsHd.number}d${clsHd.faces}|${clsHd.number === 1 ? "" :
            // clsHd.number}D${clsHd.faces}|Hit die}`;
            if (isSidekick) {
                String suffix = isClassic ? "its Constitution modifier" : "its Con. modifier";
                return """
                        - **Hit Point Die**: *x*; specified in the sidekick's statblock (human, gnome, kobold, etc.)
                        - **Hit Points at Level 1:** 1d*x* + %s
                        - **Hit Points per additional %s lvel:** 1d*x* + %s (minimum of 1 hit point per level)
                        """
                        .stripIndent()
                        .formatted(suffix, name, suffix);
            }

            String dieEntry = isClassic
                    ? "%sd%s".formatted(number, face)
                    : "%sD%s".formatted(number == 1 ? "" : number, face);

            // classic ? `${clsHd.number * clsHd.faces} + your Constitution modifier`
            // : `${clsHd.number * clsHd.faces} + Con. modifier`;
            String level1 = "%s + %s".formatted(
                    number * face,
                    isClassic ? "your Constitution modifier" : "Con. modifier");

            // classic ? `${Renderer.get().render(Renderer.class.getHitDiceEntry(clsHd,
            // {styleHint}))} (or ${((clsHd.number * clsHd.faces) / 2 + 1)}) + your
            // Constitution modifier per ${className} level after 1st`
            // : `${Renderer.get().render(Renderer.class.getHitDiceEntry(clsHd,
            // {styleHint}))} + your Con. modifier, or, ${((clsHd.number * clsHd.faces) / 2
            // + 1)} + your Con. modifier`;
            String levelUp = isClassic
                    ? "%s (or %s) + your Constitution modifier".formatted(
                            dieEntry, average)
                    : "%s + your Con. modifier or %s + your Con. modifier".formatted(
                            dieEntry, average); // average

            return """
                    - **Hit Point Die:** %s per %s level
                    - **Hit Points at Level 1:** %s
                    - **Hit Points per additional %s Level:** %s</div>
                    """.formatted(dieEntry, name, level1, name, levelUp);
        }
    }
}
