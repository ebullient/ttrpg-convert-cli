package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.CompendiumConfig.DiceRoller;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonSourceCopier;

public class TextReplacementTest implements JsonSource {

    Tui tui = new Tui();
    CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);

    Tools5eIndex index = new Tools5eIndex(config) {
        @Override
        public boolean isIncluded(String name) {
            return true;
        }
    };

    @BeforeEach
    public void before() {
        Tools5eLinkifier.instance().reset();
    }

    @Test
    public void testToHitStr() {
        String s = " +<$to_hit__str$> ";
        Matcher m = JsonSourceCopier.VARIABLE_SUBST_PAT.matcher(s);
        assertThat(m.find()).isTrue();
        assertThat(m.group("variable")).isEqualTo("to_hit__str");
        String[] pieces = m.group("variable").split("__");
        assertThat(pieces).containsExactly("to_hit", "str");
    }

    @Test
    public void testDamageAvg() {
        String s = "2.5+str";
        Matcher m = Tools5eJsonSourceCopier.dmg_avg_subst.matcher(s);
        assertThat(m.matches()).describedAs("damage_avg regex should match " + s).isTrue();
        assertThat(m.group(1)).isEqualTo("2.5");
        assertThat(m.group(2)).isEqualTo("+");
        assertThat(m.group(3)).isEqualTo("str");
    }

    @Test
    public void testPromptString() {
        // #$prompt_number:default=0,min=0,max=2,title=Enter +2 if the characters provide a bribe or incentive$#
        // #$prompt_number:min=1,title=Enter the creature's CR!,default=1$#

        String value = "{@dice d12 + #$prompt_number:default=0,min=0,max=2,title=Enter +2 if the characters provide a bribe or incentive$#}";
        String result = this.replacePromptStrings(value);
        assertThat(result).isEqualTo(
                "{@dice d12 + <span title='default=0, max=2, min=0'>[+2 if the characters provide a bribe or incentive]</span>}");

        value = "{@dice d20 + #$prompt_number:title=Enter a Modifier$#}";
        result = this.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@dice d20 + <span>[Modifier]</span>}");

        value = "{@dice 2d4 + #$prompt_number:title=Enter Alert Level$#}";
        result = this.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@dice 2d4 + <span>[Alert Level]</span>}");

        value = "#$prompt_number:min=1,title=Enter the creature's CR!,default=1$#";
        result = this.replacePromptStrings(value);
        assertThat(result).isEqualTo("<span title='default=1, min=1'>[creature's CR]</span>");

        value = "{@dice #$prompt_number:min=1,title=Number of crew,default=10$#d4|1d4} gp per crew";
        result = this.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@dice <span title='default=10, min=1'>[Number of crew]</span>d4|1d4} gp per crew");

        value = "#$prompt_number:min=1,max=5,default=123$#";
        result = this.replacePromptStrings(value);
        assertThat(result).isEqualTo("<span title='max=5, min=1'>[123]</span>");

        value = "{@damage #$prompt_number:min=1,max=13,title=Enter amount of psi to spend!,default=1$#|1}";
        result = this.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@damage <span title='default=1, max=13, min=1'>[amount of psi to spend]</span>|1}");

        value = "{@dice 1d20+#$prompt_number:min=1,title=Enter your spell attack bonus$#|+X}";
        result = this.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@dice 1d20+<span title='min=1'>[spell attack bonus]</span>|+X}");

        value = "{@damage (#$prompt_number:min=1,title=Enter the creature's CR!,default=1$#)d8|A number of  d8s of necrotic damage equal to the creature's challenge rating}";
        result = this.replacePromptStrings(value);
        assertThat(result).isEqualTo(
                "{@damage (<span title='default=1, min=1'>[creature's CR]</span>)d8|A number of  d8s of necrotic damage equal to the creature's challenge rating}");
    }

    @Test
    public void testDiceTableHeader() {
        String value = "{@dice d12 + #$prompt_number:default=0,min=0,max=2,title=Enter +2 if the characters provide a bribe or incentive$#}";
        String result = this.tableHeader(value);
        assertThat(result).isEqualTo(
                "d12 + <span title='default=0, max=2, min=0'>[+2 if the characters provide a bribe or incentive]</span>");

        value = "{@dice 1d6;2d6|d6s}";
        result = this.tableHeader(value);
        assertThat(result).isEqualTo("1d6;2d6");

        value = "{@dice d6;d8}";
        result = this.tableHeader(value);
        assertThat(result).isEqualTo("d6;d8");
    }

    @Test
    public void testDiceFormla() {
        Configurator configurator = new Configurator(tui);

        List<String> example = List.of(
                "Spells cast from the spell gem have a save DC of 15 and an attack bonus of {@hit 9}.",
                "It has a Strength of 26 ({@d20 8}) and a Dexterity of 10 ({@d20 0})",
                "{@dice 1d2-2+2d3+5} for regular dice rolls",
                "{@dice 1d6;2d6} for multiple options;",
                "{@dice 1d6 + #$prompt_number:min=1,title=Enter a Number!,default=123$#} for input prompts)",
                "with extended {@dice 1d20+2|display text} and {@dice 1d20+2|display text|rolled by name}",
                "a special 'hit' version which assumes a d20 is to be rolled {@hit +7}",
                "There's also {@damage 1d12+3} and {@d20 -4}",
                "scaledamage: {@scaledamage 2d6;3d6|2-9|1d6}, scaledice: {@scaledice 2d6|1,3,5,7,9|1d6|psi|extra amount}",
                "{@ability str 20}, {@savingThrow str 5}, and {@skillCheck animal_handling 5}",
                "with an Intelligence of {@d20 3|16}, a Wisdom of {@d20 0|10}, and a Charisma of {@d20 4|18}",
                "{@hit 3 plus PB} to hit;  {@h}7 ({@damage 1d6 + 4}) piercing damage plus 7 ({@damage 2d6}) poison damage.",
                "{@d20 2|10|Perception} {@d20 -2|8|Perception}",
                "hit display text: {@hit +3|+3 to hit}",
                "{@atk mw} {@hit 9} to hit, reach 5 ft., one target. {@h}9 ({@damage 1d8 + 5}) piercing damage plus 7 ({@damage 2d6}) necrotic damage.");

        List<String> disabled = List.of(
                "Spells cast from the spell gem have a save DC of 15 and an attack bonus of `+9`.",
                "It has a Strength of 26 (`+8`) and a Dexterity of 10 (`+0`)",
                "`1d2-2+2d3+5` for regular dice rolls",
                "`1d6` or `2d6` for multiple options;",
                "<span title='default=123, min=1'>`1d6 + [Number]`</span> for input prompts)",
                "with extended display text (`+2`) and display text (`+2`)",
                "a special 'hit' version which assumes a d20 is to be rolled `+7`",
                "There's also `1d12+3` and `-4`",
                "scaledamage: `1d6`, scaledice: extra amount (`1d6`)",
                "<span title='Strength'>`20` (`+5`)</span>, <span title='Strength'>`+5`</span>, and [Animal Handling](rules/skills.md#Animal%20Handling) (`+5`)",
                "with an Intelligence of `+3` (`16`), a Wisdom of `+0` (`10`), and a Charisma of `+4` (`18`)",
                "`+3 plus PB` to hit;  *Hit:* 7 (`1d6 + 4`) piercing damage plus 7 (`2d6`) poison damage.",
                "Perception (`+2`) Perception (`-2`)",
                "hit display text: +3 to hit",
                "*Melee Weapon Attack:* `+9` to hit, reach 5 ft., one target. *Hit:* 9 (`1d8 + 5`) piercing damage plus 7 (`2d6`) necrotic damage.");

        configurator.setUseDiceRoller(DiceRoller.disabled);
        for (int i = 0; i < example.size(); i++) {
            String result = this.replaceText(example.get(i));
            assertThat(result).isEqualTo(disabled.get(i));
        }

        List<String> enabled = List.of(
                "Spells cast from the spell gem have a save DC of 15 and an attack bonus of `dice:1d20+9|noform|noparens|text(+9)`.",
                "It has a Strength of `dice:1d20+8|noform|noparens|text(26)` (`+8`) and a Dexterity of `dice:1d20+0|noform|noparens|text(10)` (`+0`)",
                "`dice:1d2-2+2d3+5|noform|noparens|avg` (`1d2-2+2d3+5`) for regular dice rolls",
                "`dice:1d6|noform|noparens|avg|text(1d6)` or `dice:2d6|noform|noparens|avg|text(2d6)` for multiple options;",
                "<span title='default=123, min=1'>`1d6 + [Number]`</span> for input prompts)",
                "with extended `dice:1d20+2|noform|noparens|avg|text(display text)` (`+2`) and `dice:1d20+2|noform|noparens|avg|text(display text)` (`+2`)",
                "a special 'hit' version which assumes a d20 is to be rolled `dice:1d20+7|noform|noparens|text(+7)`",
                "There's also `dice:1d12+3|noform|noparens|avg` (`1d12+3`) and `dice:1d20-4|noform|noparens|text(-4)`",
                "scaledamage: `dice:1d6|noform|noparens|avg|text(1d6)`, scaledice: `dice:1d6|noform|noparens|avg|text(extra amount)` (`1d6`)",
                "<span title='Strength'>`20` (`dice:d20+5|noform|noparens|text(+5)`)</span>, <span title='Strength'>`dice:d20+5|noform|noparens|text(+5)`</span>, and [Animal Handling](rules/skills.md#Animal%20Handling) (`dice:1d20+5|noform|noparens|text(+5)`)",
                "with an Intelligence of `dice:1d20+3|noform|noparens|text(+3)` (`16`), a Wisdom of `dice:1d20+0|noform|noparens|text(+0)` (`10`), and a Charisma of `dice:1d20+4|noform|noparens|text(+4)` (`18`)",
                "`+3 plus PB` to hit;  *Hit:* `dice:1d6+4|noform|noparens|avg|text(7)` (`1d6 + 4`) piercing damage plus `dice:2d6|noform|noparens|avg|text(7)` (`2d6`) poison damage.",
                "Perception (`dice:1d20+2|noform|noparens|text(+2)`) Perception (`dice:1d20-2|noform|noparens|text(-2)`)",
                "hit display text: +3 to hit",
                "*Melee Weapon Attack:* `dice:1d20+9|noform|noparens|text(+9)` to hit, reach 5 ft., one target. *Hit:* `dice:1d8+5|noform|noparens|avg|text(9)` (`1d8 + 5`) piercing damage plus `dice:2d6|noform|noparens|avg|text(7)` (`2d6`) necrotic damage.");

        configurator.setUseDiceRoller(DiceRoller.enabled);
        for (int i = 0; i < example.size(); i++) {
            String result = this.replaceText(example.get(i));
            assertThat(result).isEqualTo(enabled.get(i));
        }

        // With no context change, all should render the same
        configurator.setUseDiceRoller(DiceRoller.enabledUsingFS);
        for (int i = 0; i < example.size(); i++) {
            String result = this.replaceText(example.get(i));
            assertThat(result).isEqualTo(enabled.get(i));
        }

        List<String> traits = List.of(
                "Spells cast from the spell gem have a save DC of 15 and an attack bonus of +9.",
                "It has a Strength of 26 (+8) and a Dexterity of 10 (+0)",
                "1d2-2+2d3+5 for regular dice rolls",
                "1d6 or 2d6 for multiple options;",
                "<span title='default=123, min=1'>1d6 + [Number]</span> for input prompts)",
                "with extended display text (+2) and display text (+2)",
                "a special 'hit' version which assumes a d20 is to be rolled +7",
                "There's also 1d12+3 and -4",
                "scaledamage: 1d6, scaledice: extra amount (1d6)",
                "<span title='Strength'>20 (+5)</span>, <span title='Strength'>+5</span>, and [Animal Handling](rules/skills.md#Animal%20Handling) (+5)",
                "with an Intelligence of +3 (16), a Wisdom of +0 (10), and a Charisma of +4 (18)",
                "+3 plus PB to hit;  *Hit:* 7 (1d6 + 4) piercing damage plus 7 (2d6) poison damage.",
                "Perception (+2) Perception (-2)",
                "hit display text: +3 to hit",
                "*Melee Weapon Attack:* +9 to hit, reach 5 ft., one target. *Hit:* 9 (1d8 + 5) piercing damage plus 7 (2d6) necrotic damage.");

        // Now we'll indicate that we're within a trait (for a statblock)
        boolean pushed = parseState().pushTrait();
        try {
            for (int i = 0; i < example.size(); i++) {
                String result = this.replaceText(example.get(i));
                assertThat(result).isEqualTo(traits.get(i));
            }

            // We should get the same result for disabledUsingFS (no backticks)
            configurator.setUseDiceRoller(DiceRoller.disabledUsingFS);
            for (int i = 0; i < example.size(); i++) {
                String result = this.replaceText(example.get(i));
                assertThat(result).isEqualTo(traits.get(i));
            }
        } finally {
            parseState().pop(pushed);
        }
    }

    @Test
    void testSimplify() {
        String example = " 7 (`dice:2d6|avg|noform` (`2d6`))";
        String result = this.simplifyFormattedDiceText(example);
        assertThat(result).isEqualTo(" `dice:2d6|avg|noform|text(7)` (`2d6`)");
    }

    @Test
    void testSimplifyTable() {
        boolean pushed = parseState().pushMarkdownTable(true);
        try {
            String example = " 7 (`dice:2d6|avg|noform` (`2d6`))";
            String result = this.simplifyFormattedDiceText(example);
            assertThat(result).isEqualTo(" `dice:2d6\\|avg\\|noform\\|text(7)` (`2d6`)");
        } finally {
            parseState().pop(pushed);
        }
    }

    @Test
    void testPlainD20() {
        Configurator configurator = new Configurator(tui);

        String d20 = "{@dice d20}";
        String oneD20 = "{@dice 1d20}";
        String tag20 = "{@d20}";

        assertThat(this.replaceText(d20)).isEqualTo("`d20`");
        assertThat(this.replaceText(oneD20)).isEqualTo("`1d20`");
        assertThat(this.replaceText(tag20)).isEqualTo("`d20`");

        configurator.setUseDiceRoller(DiceRoller.enabled);

        assertThat(this.replaceText(d20)).isEqualTo("`dice:1d20|noform|noparens|avg|text(d20)`");
        assertThat(this.replaceText(oneD20)).isEqualTo("`dice:1d20|noform|noparens|avg|text(1d20)`");
        assertThat(this.replaceText(tag20)).isEqualTo("`dice:1d20|noform|noparens|avg|text(d20)`");

        configurator.setUseDiceRoller(DiceRoller.enabledUsingFS);

        assertThat(this.replaceText(d20)).isEqualTo("`dice:1d20|noform|noparens|avg|text(d20)`");
        assertThat(this.replaceText(oneD20)).isEqualTo("`dice:1d20|noform|noparens|avg|text(1d20)`");
        assertThat(this.replaceText(tag20)).isEqualTo("`dice:1d20|noform|noparens|avg|text(d20)`");

        boolean pushed = parseState().pushMarkdownTable(true);
        try {
            assertThat(this.replaceText(d20)).isEqualTo("`dice:1d20\\|noform\\|noparens\\|avg\\|text(d20)`");
            assertThat(this.replaceText(oneD20)).isEqualTo("`dice:1d20\\|noform\\|noparens\\|avg\\|text(1d20)`");
            assertThat(this.replaceText(tag20)).isEqualTo("`dice:1d20\\|noform\\|noparens\\|avg\\|text(d20)`");
        } finally {
            parseState().pop(pushed);
        }

        pushed = parseState().pushTrait();
        try {
            assertThat(this.replaceText(d20)).isEqualTo("d20");
            assertThat(this.replaceText(oneD20)).isEqualTo("1d20");
            assertThat(this.replaceText(tag20)).isEqualTo("d20");

            configurator.setUseDiceRoller(DiceRoller.disabledUsingFS);

            assertThat(this.replaceText(d20)).isEqualTo("d20");
            assertThat(this.replaceText(oneD20)).isEqualTo("1d20");
            assertThat(this.replaceText(tag20)).isEqualTo("d20");
        } finally {
            parseState().pop(pushed);
        }
    }

    @Test
    void testPlainD12() {
        Configurator configurator = new Configurator(tui);

        String d12 = "{@dice d12}";
        String oneD12 = "{@dice 1d12}";

        assertThat(this.replaceText(d12)).isEqualTo("`d12`");
        assertThat(this.replaceText(oneD12)).isEqualTo("`1d12`");

        configurator.setUseDiceRoller(DiceRoller.enabled);

        assertThat(this.replaceText(d12)).isEqualTo("`dice:1d12|noform|noparens|avg|text(d12)`");
        assertThat(this.replaceText(oneD12)).isEqualTo("`dice:1d12|noform|noparens|avg|text(1d12)`");

        configurator.setUseDiceRoller(DiceRoller.enabledUsingFS);

        assertThat(this.replaceText(d12)).isEqualTo("`dice:1d12|noform|noparens|avg|text(d12)`");
        assertThat(this.replaceText(oneD12)).isEqualTo("`dice:1d12|noform|noparens|avg|text(1d12)`");

        boolean pushed = parseState().pushMarkdownTable(true);
        try {
            assertThat(this.replaceText(d12)).isEqualTo("`dice:1d12\\|noform\\|noparens\\|avg\\|text(d12)`");
            assertThat(this.replaceText(oneD12)).isEqualTo("`dice:1d12\\|noform\\|noparens\\|avg\\|text(1d12)`");
        } finally {
            parseState().pop(pushed);
        }

        pushed = parseState().pushTrait();
        try {
            assertThat(this.replaceText(d12)).isEqualTo("d12");
            assertThat(this.replaceText(oneD12)).isEqualTo("1d12");

            configurator.setUseDiceRoller(DiceRoller.disabledUsingFS);

            assertThat(this.replaceText(d12)).isEqualTo("d12");
            assertThat(this.replaceText(oneD12)).isEqualTo("1d12");
        } finally {
            parseState().pop(pushed);
        }
    }

    @Test
    public void testHitYourSpellAttack() {
        String s = "{@hitYourSpellAttack} to hit,";
        String result = this.replaceText(s);
        assertThat(result).isEqualTo("your spell attack modifier to hit,");

        s = "{@hitYourSpellAttack Bonus equals your spell attack modifier}";
        result = this.replaceText(s);
        assertThat(result).isEqualTo("Bonus equals your spell attack modifier");
    }

    @Override
    public Tools5eIndex index() {
        return index;
    }

    @Override
    public Tools5eSources getSources() {
        return Tools5eSources.findOrTemporary(Tui.MAPPER.createObjectNode());
    }
}
