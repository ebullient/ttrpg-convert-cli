package dev.ebullient.convert.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Tui;
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
    public void testImgurUrl() {
        String input = "https://imgur.com/lQfZ1dF.png";
        String expected = "https://i.imgur.com/lQfZ1dF.png";

        Tools5eSources sources = Tools5eSources.findOrTemporary(
                Tui.MAPPER.createObjectNode()
                        .put("name", "Critter")
                        .put("source", "DMG"));

        // Test token image path
        ImageRef ref = sources.buildTokenImageRef(index,
                input,
                Path.of("something.png"),
                false);

        assertThat(ref.url()).isEqualTo(expected);
    }
}
