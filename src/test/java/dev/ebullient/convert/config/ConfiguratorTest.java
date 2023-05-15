package dev.ebullient.convert.config;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Collection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

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

        tui.readFile(TestUtils.TEST_PATH_JSON, (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();
            assertThat(config).isNotNull();
            assertThat(config.compendiumRoot()).isEqualTo("/");
            assertThat(config.compendiumPath()).isEqualTo(CompendiumConfig.CWD);
            assertThat(config.rulesRoot()).isEqualTo("/rules/");
            assertThat(config.rulesPath()).isEqualTo(Path.of("rules/"));
        });
    }

    @Test
    public void testPathNested() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_PATH_JSON, (f, node) -> {
            ObjectNode parent = Tui.MAPPER.createObjectNode();
            ObjectNode ttrpg = Tui.MAPPER.createObjectNode();
            parent.set("ttrpg", ttrpg);
            ttrpg.set("5e", node);

            test.readConfigIfPresent(parent);
            CompendiumConfig config = TtrpgConfig.getConfig();

            assertThat(config).isNotNull();
            assertThat(config).isNotNull();
            assertThat(config.compendiumRoot()).isEqualTo("/");
            assertThat(config.compendiumPath()).isEqualTo(CompendiumConfig.CWD);
            assertThat(config.rulesRoot()).isEqualTo("/rules/");
            assertThat(config.rulesPath()).isEqualTo(Path.of("rules/"));
        });

    }

    @Test
    public void testSources() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_SOURCES_JSON_5E, (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();

            assertThat(config).isNotNull();
            assertThat(config.allSources()).isFalse();
            assertThat(config.sourceIncluded("phb")).isTrue();
            assertThat(config.sourceIncluded("scag")).isFalse();
            assertThat(config.getAllowedSourcePattern()).contains("phb");
            assertThat(config.getAllowedSourcePattern()).contains("dmg");
            assertThat(config.getAllowedSourcePattern()).contains("xge");
            assertThat(config.getAllowedSourcePattern()).contains("tce");
            assertThat(config.getAllowedSourcePattern()).contains("wbtw");
        });
    }

    @Test
    public void testFromAll() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_SOURCES_FROM_ALL, (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();

            assertThat(config).isNotNull();
            assertThat(config.allSources()).isTrue();
            assertThat(config.sourceIncluded("scag")).isTrue();
            assertThat(config.getAllowedSourcePattern()).contains("([^|]+)");
        });
    }

    @Test
    public void testBooksAdventures() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_SOURCES_BOOK_ADV_JSON_5E, (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();
            Collection<String> books = config.getBooks();
            Collection<String> adventures = config.getAdventures();

            assertThat(config).isNotNull();
            assertThat(books).contains("book/book-phb.json");
            assertThat(adventures).contains("adventure/adventure-wbtw.json");

            assertThat(config.compendiumRoot()).isEqualTo("/compend%20ium/");
            assertThat(config.compendiumPath()).isEqualTo(Path.of("compend ium/"));
            assertThat(config.rulesRoot()).isEqualTo("/ru%20les/");
            assertThat(config.rulesPath()).isEqualTo(Path.of("ru les/"));
        });
    }

    @Test
    public void testSourcesBadTemplates() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_SOURCES_BAD_TEMPL_JSON, (f, node) -> {
            assertThrows(IllegalArgumentException.class,
                    () -> test.readConfigIfPresent(node));

            CompendiumConfig config = TtrpgConfig.getConfig();
            assertThat(config).isNotNull();
            assertThat(config.getCustomTemplate("background")).isNull();
        });
    }

}
