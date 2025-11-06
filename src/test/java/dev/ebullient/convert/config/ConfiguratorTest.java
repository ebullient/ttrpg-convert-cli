package dev.ebullient.convert.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.io.Tui;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfiguratorTest {
    protected static Tui tui;

    @BeforeAll
    public static void prepare() {
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);
    }

    @Test
    public void testPath() throws Exception {
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("paths.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();
            assertThat(config).isNotNull();
            assertThat(config.compendiumVaultRoot()).isEqualTo("");
            assertThat(config.compendiumFilePath()).isEqualTo(CompendiumConfig.CWD);
            assertThat(config.rulesVaultRoot()).isEqualTo("rules/");
            assertThat(config.rulesFilePath()).isEqualTo(Path.of("rules/"));
            assertThat(config.images.copyInternal()).isFalse();
        });
    }

    @Test
    public void testSources() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("5e/sources.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();
            config.resolveAdventures();
            config.resolveBooks();
            config.resolveHomebrew();

            assertThat(config).isNotNull();
            assertThat(config.allSources()).isFalse();
            assertThat(config.sourceIncluded("phb")).isTrue();
            assertThat(config.sourceIncluded("scag")).isFalse();
            assertThat(config.sourceIncluded("dmg")).isTrue();
            assertThat(config.sourceIncluded("xge")).isTrue();
            assertThat(config.sourceIncluded("tce")).isTrue();
            assertThat(config.sourceIncluded("wbtw")).isTrue();
        });
    }

    @Test
    public void testFromAll() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("sources-from-all.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();

            assertThat(config).isNotNull();
            assertThat(config.allSources()).isTrue();
            assertThat(config.sourceIncluded("scag")).isTrue();
        });
    }

    @Test
    public void testBooksAdventures() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("5e/sources-2014-book-adventure.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();

            Collection<String> books = config.resolveBooks();
            Collection<String> adventures = config.resolveAdventures();
            Collection<String> homebrew = config.resolveHomebrew();

            assertThat(config).isNotNull();

            // empty values should be filtered out
            assertThat(books).size().isEqualTo(2);
            assertThat(adventures).size().isEqualTo(3);
            assertThat(homebrew).size().isEqualTo(0);

            assertThat(books).contains("book/book-phb.json");
            assertThat(adventures).contains("adventure/adventure-wbtw.json");

            assertThat(config.compendiumVaultRoot()).isEqualTo("/compend%20ium/");
            assertThat(config.compendiumFilePath()).isEqualTo(Path.of("compend ium/"));
            assertThat(config.rulesVaultRoot()).isEqualTo("/ru%20les/");
            assertThat(config.rulesFilePath()).isEqualTo(Path.of("ru les/"));
        });
    }

    @Test
    public void testSourcesBadTemplates() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("sources-bad-template.json"), List.of(), (f, node) -> {
            assertThrows(IllegalArgumentException.class,
                    () -> test.readConfigIfPresent(node));

            CompendiumConfig config = TtrpgConfig.getConfig();
            assertThat(config).isNotNull();
            assertThat(config.getCustomTemplate("background")).isNull();
        });
    }

    @Test
    public void testSourcesNoImages() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();

            assertThat(config).isNotNull();
            assertThat(config.imageOptions()).isNotNull();
            assertThat(config.imageOptions().copyInternal()).isFalse();
        });
    }
}
