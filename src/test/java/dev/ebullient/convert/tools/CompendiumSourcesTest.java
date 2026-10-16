package dev.ebullient.convert.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;

/**
 * Exercises {@link CompendiumSources} directly via a minimal stub, avoiding the
 * full index-registration machinery a concrete subclass (e.g. {@code Tools5eSources})
 * requires for {@code getSourceText()} (which looks the node up in a live index
 * rather than holding it directly).
 */
public class CompendiumSourcesTest {

    static CompendiumConfig config;

    @BeforeAll
    static void setupConfig() throws Exception {
        Tui tui = new Tui();
        config = ConfiguratorUtil.createNewConfig(tui);

        // includedBy() short-circuits on config.noSources(), so allowedSources
        // must be non-empty for the inclusion checks below to exercise real logic.
        JsonNode userConfig = Tui.MAPPER.readTree("""
                {
                  "sources": {
                    "adventure": ["TEST-WBTW"],
                    "reference": ["TEST-VGM", "TEST-MPMM"]
                  }
                }
                """);
        new CompendiumConfig.Configurator(tui).readConfigIfPresent(userConfig);
    }

    private static final class StubIndexType implements IndexType {
        @Override
        public String name() {
            return "stub";
        }

        @Override
        public String templateName() {
            return "stub";
        }

        @Override
        public String defaultSourceString() {
            return null;
        }

        @Override
        public String createKey(JsonNode node) {
            return "test|" + node.get("name").asText();
        }
    }

    private static final IndexType STUB_TYPE = new StubIndexType();

    private static final class StubSources extends CompendiumSources {
        private final JsonNode node;

        StubSources(JsonNode node) {
            super(STUB_TYPE, STUB_TYPE.createKey(node), node);
            this.node = node;
        }

        @Override
        public JsonNode findNode() {
            return node;
        }

        @Override
        protected String findName(IndexType type, JsonNode jsonElement) {
            return jsonElement.get("name").asText();
        }
    }

    @Test
    void referenceSourcesAffectInclusionButNotSourceTextOrPrimarySource() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Test Creature A",
                  "source": "TEST-VGM",
                  "page": 134,
                  "referenceSources": ["TEST-WBTW"]
                }
                """);

        CompendiumSources sources = new StubSources(source);

        // inclusion: a referencing source (WBTW) includes the entity even though
        // its own source (VGM) is not independently allowed
        assertThat(sources.includedBy(Set.of("test-wbtw"))).isTrue();
        assertThat(sources.includedBy(Set.of("test-phb"))).isFalse();

        // display/naming: referenceSources must not leak into source text or primarySource()
        assertThat(sources.primarySource()).isEqualTo("TEST-VGM");
        assertThat(sources.getSourceText()).doesNotContain("TEST-WBTW");
    }

    @Test
    void absentReferenceSourcesBehavesAsBefore() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Test Creature C",
                  "source": "TEST-VGM",
                  "page": 134
                }
                """);

        CompendiumSources sources = new StubSources(source);

        assertThat(sources.includedBy(Set.of("test-vgm"))).isTrue();
        assertThat(sources.includedBy(Set.of("test-wbtw"))).isFalse();
    }

    @Test
    void otherSourcesAndReferenceSourcesAreDistinctInSourceText() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Test Creature D",
                  "source": "TEST-MPMM",
                  "page": 84,
                  "otherSources": [{"source": "TEST-VGM", "page": 134}],
                  "referenceSources": ["TEST-WBTW"]
                }
                """);

        CompendiumSources sources = new StubSources(source);

        assertThat(sources.getSourceText()).contains("TEST-VGM");
        assertThat(sources.getSourceText()).doesNotContain("TEST-WBTW");
    }
}
