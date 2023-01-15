package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Templates;
import dev.ebullient.convert.io.Tui;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class Pf2JsonDataTest {
    static final Path outputPath = TestUtils.OUTPUT_ROOT_PF2.resolve("all");

    protected static TtrpgConfig ttrpgConfig;
    protected static Templates templates;
    protected static Tui tui;

    protected JsonIndex index;

    @BeforeAll
    public static void prepare() {
        outputPath.toFile().mkdirs();
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);

        ttrpgConfig = Arc.container().instance(TtrpgConfig.class).get();
        templates = Arc.container().instance(Templates.class).get();
    }

    @Test
    public void testDataIndex_pf2e() throws Exception {
        TtrpgConfig ttrpgConfig = new TtrpgConfig();
        Configurator configurator = new Configurator(ttrpgConfig, tui, Datasource.toolsp2fe);

        if (TestUtils.TOOLS_PATH_PF2E.toFile().exists()) {
            index = new JsonIndex(ttrpgConfig.getConfig());

            configurator.setSources(List.of("*"));

            for (String x : List.of("books.json",
                    "book/book-crb.json", "book/book-gmg.json")) {
                tui.readFile(TestUtils.TOOLS_PATH_PF2E.resolve(x), index::importTree);
            }
            tui.readPf2eTools(outputPath, index::importTree);
            index.prepare();
        }
    }
}
