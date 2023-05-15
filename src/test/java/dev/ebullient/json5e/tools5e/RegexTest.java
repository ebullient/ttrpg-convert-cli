package dev.ebullient.json5e.tools5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;

import dev.ebullient.json5e.io.Json5eTui;

public class RegexTest {

    Json5eTui tui = new Json5eTui();
    JsonIndex index = new JsonIndex(List.of("ALL"), tui);
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
