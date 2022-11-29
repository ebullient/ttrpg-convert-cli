package dev.ebullient.json5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.ebullient.json5e.io.Json5eTui;
import dev.ebullient.json5e.io.Templates;
import dev.ebullient.json5e.tools5e.Json5eConfig;
import dev.ebullient.json5e.tools5e.JsonIndex;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JsonSourcesTest {

    protected Json5eTui tui;
    protected Templates templates;
    protected JsonIndex index;

    @Test
    public void testFromAllSources() {
        tui = Arc.container().instance(Json5eTui.class).get();
        templates = Arc.container().instance(Templates.class).get();
        tui.init(null, false, false);

        index = new JsonIndex(List.of(), tui);

        tui.readFile(TestUtils.TEST_SOURCES_FROM_ALL, index::importTree);

        Json5eConfig config = index.getExtraConfig();
        assertThat(config.allSources()).isTrue();
        assertThat(config.srdOnly()).isFalse();
        assertThat(config.getAllowedSourcePattern()).isEqualTo("([^|]+)");
    }

    @Test
    public void testFromSomeSources() {
        tui = Arc.container().instance(Json5eTui.class).get();
        templates = Arc.container().instance(Templates.class).get();
        tui.init(null, false, false);

        index = new JsonIndex(List.of(), tui);

        tui.readFile(TestUtils.TEST_SOURCES_JSON, index::importTree);

        Json5eConfig config = index.getExtraConfig();
        assertThat(config.allSources()).isFalse();
        assertThat(config.srdOnly()).isFalse();
        assertThat(config.getAllowedSourcePattern()).contains("phb|");
    }

    @Test
    public void testSrdOnly() {
        tui = Arc.container().instance(Json5eTui.class).get();
        templates = Arc.container().instance(Templates.class).get();
        tui.init(null, false, false);

        index = new JsonIndex(List.of(), tui);

        Json5eConfig config = index.getExtraConfig();
        assertThat(config.allSources()).isFalse();
        assertThat(config.srdOnly()).isTrue();
        assertThat(config.getAllowedSourcePattern()).isEqualTo("()");
    }
}
