package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Tui;

public class RegexTest {

    Tui tui = new Tui();

    CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);
    Tools5eIndex index = new Tools5eIndex(config);
    JsonSourceCopier copier = new JsonSourceCopier(index);

    @BeforeEach
    void init() {
        TtrpgConfig.init(tui, Datasource.tools5e);
    }

    @Test
    public void testToHitStr() {
        String s = " +<$to_hit__str$> ";
        Matcher m = JsonSourceCopier.variable_subst.matcher(s);
        assertThat(m.find()).isTrue();
        assertThat(m.group("variable")).isEqualTo("to_hit__str");
        String[] pieces = m.group("variable").split("__");
        assertThat(pieces).containsExactly("to_hit", "str");
    }

    @Test
    public void testDamageAvg() {
        String s = "2.5+str";
        Matcher m = JsonSourceCopier.dmg_avg_subst.matcher(s);
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
        String result = copier.replacePromptStrings(value);
        assertThat(result).isEqualTo(
                "{@dice d12 + <span title='default=0, max=2, min=0'>[+2 if the characters provide a bribe or incentive]</span>}");

        value = "{@dice d20 + #$prompt_number:title=Enter a Modifier$#}";
        result = copier.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@dice d20 + <span>[Modifier]</span>}");

        value = "{@dice 2d4 + #$prompt_number:title=Enter Alert Level$#}";
        result = copier.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@dice 2d4 + <span>[Alert Level]</span>}");

        value = "#$prompt_number:min=1,title=Enter the creature's CR!,default=1$#";
        result = copier.replacePromptStrings(value);
        assertThat(result).isEqualTo("<span title='default=1, min=1'>[creature's CR]</span>");

        value = "{@dice #$prompt_number:min=1,title=Number of crew,default=10$#d4|1d4} gp per crew";
        result = copier.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@dice <span title='default=10, min=1'>[Number of crew]</span>d4|1d4} gp per crew");

        value = "#$prompt_number:min=1,max=5,default=123$#";
        result = copier.replacePromptStrings(value);
        assertThat(result).isEqualTo("<span title='max=5, min=1'>[123]</span>");

        value = "{@damage #$prompt_number:min=1,max=13,title=Enter amount of psi to spend!,default=1$#|1}";
        result = copier.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@damage <span title='default=1, max=13, min=1'>[amount of psi to spend]</span>|1}");

        value = "{@dice 1d20+#$prompt_number:min=1,title=Enter your spell attack bonus$#|+X}";
        result = copier.replacePromptStrings(value);
        assertThat(result).isEqualTo("{@dice 1d20+<span title='min=1'>[spell attack bonus]</span>|+X}");

        value = "{@damage (#$prompt_number:min=1,title=Enter the creature's CR!,default=1$#)d8|A number of  d8s of necrotic damage equal to the creature's challenge rating}";
        result = copier.replacePromptStrings(value);
        assertThat(result).isEqualTo(
                "{@damage (<span title='default=1, min=1'>[creature's CR]</span>)d8|A number of  d8s of necrotic damage equal to the creature's challenge rating}");
    }

    @Test
    public void testDiceTableHeader() {
        String value = "{@dice d12 + #$prompt_number:default=0,min=0,max=2,title=Enter +2 if the characters provide a bribe or incentive$#}";
        String result = copier.tableHeader(value);
        assertThat(result).isEqualTo(
                "d12 + <span title='default=0, max=2, min=0'>[+2 if the characters provide a bribe or incentive]</span>");

        value = "{@dice 1d6;2d6|d6s}";
        result = copier.tableHeader(value);
        assertThat(result).isEqualTo("1d6;2d6");

        value = "{@dice d6;d8}";
        result = copier.tableHeader(value);
        assertThat(result).isEqualTo("d6;d8");
    }
}
