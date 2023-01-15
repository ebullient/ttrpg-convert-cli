package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;

import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;

public class RegexTest {

    Tui tui = new Tui();
    Tools5eIndex index = new Tools5eIndex(ConfiguratorUtil.testConfig(tui));
    JsonSourceCopier copier = new JsonSourceCopier(index);

    @Test
    public void testToHitStr() {
        String s = " +<$to_hit__str$> ";
        Matcher m = JsonSourceCopier.to_hit_subst.matcher(s);
        assertThat(m.find()).isTrue();
        m.results().forEach(x -> assertThat(x.group(1)).isEqualTo("str"));
    }

    @Test
    public void testDamageAvg() {
        String s = " <$damage_avg__2.5+str$> ";
        Matcher m = JsonSourceCopier.dmg_avg_subst.matcher(s);
        assertThat(m.find()).describedAs("damage_avg regex should match " + s).isTrue();
        m.results().forEach(x -> assertThat(x.group(1)).isEqualTo("2.5"));
        m.results().forEach(x -> assertThat(x.group(2)).isEqualTo("+"));
        m.results().forEach(x -> assertThat(x.group(3)).isEqualTo("str"));
    }
}
