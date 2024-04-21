package dev.ebullient.convert.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ImgurUrlTest {
    protected static Tui tui;
    protected static Tools5eIndex index;

    @BeforeAll
    public static void prepare() {
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);
        index = new Tools5eIndex(TtrpgConfig.getConfig());
    }

    @Test
    public void testImgurUrl() throws MalformedURLException, UnsupportedEncodingException {
        String input = "https://imgur.com/lQfZ1dF.png";

        assertThat(tui.escapeUrlImagePath(input))
                .isEqualTo("https://i.imgur.com/lQfZ1dF.png");
    }

    @Test
    public void testAccentedCharacters() throws MalformedURLException, UnsupportedEncodingException {
        String input = "https://whatever.com/áé.png?raw=true";

        assertThat(tui.escapeUrlImagePath(input))
                .isEqualTo("https://whatever.com/%C3%A1%C3%A9.png?raw=true");

        Tools5eSources sources = Tools5eSources.findOrTemporary(
                Tui.MAPPER.createObjectNode()
                        .put("name", "Critter")
                        .put("source", "DMG"));

        ImageRef ref = sources.buildTokenImageRef(index,
                "https://raw.githubusercontent.com/TheGiddyLimit/homebrew/master/_img/MonsterManualExpanded3/creature/Hill%20Giant%20Warlock%20Of%20Ogrémoch.jpg",
                Path.of("something.png"),
                false);

        assertThat(tui.escapeUrlImagePath(ref.url()))
                .isEqualTo(
                        "https://raw.githubusercontent.com/TheGiddyLimit/homebrew/master/_img/MonsterManualExpanded3/creature/Hill%20Giant%20Warlock%20Of%20Ogr%C3%A9moch.jpg");

        ref = sources.buildTokenImageRef(index,
                "https://raw.githubusercontent.com/TheGiddyLimit/homebrew/master/_img/MonsterManualExpanded3/creature/token/Stone%20Giant%20Warlock%20Of%20Ogrémoch%20%28Token%29.png",
                Path.of("something.png"),
                false);

        assertThat(tui.escapeUrlImagePath(ref.url()))
                .isEqualTo(
                        "https://raw.githubusercontent.com/TheGiddyLimit/homebrew/master/_img/MonsterManualExpanded3/creature/token/Stone%20Giant%20Warlock%20Of%20Ogr%C3%A9moch%20%28Token%29.png");
    }

}
