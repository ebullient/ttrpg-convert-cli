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
}
