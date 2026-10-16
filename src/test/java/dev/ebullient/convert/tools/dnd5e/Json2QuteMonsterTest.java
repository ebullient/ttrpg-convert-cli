package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster;

public class Json2QuteMonsterTest {

    static Tools5eIndex index;

    @BeforeAll
    static void setupIndex() {
        Tui tui = new Tui();
        CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);
        index = new Tools5eIndex(config) {
            @Override
            public boolean isIncluded(String name) {
                return true;
            }
        };
        // Avoid "Programmer error: Called isSrdBasicFreeOnly while indexing"
        // for sources resolved outside the normal index/prepare flow.
        index.prepared.set(true);
    }

    @Test
    void choosableTypeRendersAsChooseOneOf() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Empyrean",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fiend"]}}
                }
                """);

        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, source);

        assertThat(json2Qute.creatureType).isEqualTo("Choose one of celestial or fiend");

        QuteMonster monster = (QuteMonster) json2Qute.build();
        assertThat(monster.type).isEqualTo("Choose one of celestial or fiend");
    }

    @Test
    void choosableTypeCategoryRemainsMiscellaneous() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Empyrean",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fiend"]}}
                }
                """);

        Json2QuteMonster.MonsterType category = Json2QuteMonster.MonsterType.fromNode(source, index);

        assertThat(category).isEqualTo(Json2QuteMonster.MonsterType.miscellaneous);
    }

    @Test
    void findMonsterVariantsGeneratesTypedSiblingsForChooseWithNoVersions() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Empyrean",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fiend"]}}
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        assertThat(variants).hasSize(3);
        assertThat(variants.get(0)).isSameAs(source);

        JsonNode celestial = variants.get(1);
        assertThat(celestial.get("name").asText()).isEqualTo("Empyrean (Celestial)");
        assertThat(celestial.get("type").asText()).isEqualTo("celestial");
        assertThat(TtrpgValue.indexParentKey.getTextOrNull(celestial)).isEqualTo(key);
        assertThat(Tools5eIndexType.monster.createKey(celestial))
                .isEqualTo(Tools5eIndexType.monster.createKey("Empyrean (Celestial)", "TEST"));

        JsonNode fiend = variants.get(2);
        assertThat(fiend.get("name").asText()).isEqualTo("Empyrean (Fiend)");
        assertThat(fiend.get("type").asText()).isEqualTo("fiend");
        assertThat(TtrpgValue.indexParentKey.getTextOrNull(fiend)).isEqualTo(key);
        assertThat(Tools5eIndexType.monster.createKey(fiend))
                .isEqualTo(Tools5eIndexType.monster.createKey("Empyrean (Fiend)", "TEST"));

        assertThat(Json2QuteMonster.MonsterType.fromNode(celestial, index))
                .isEqualTo(Json2QuteMonster.MonsterType.celestial);
        assertThat(Json2QuteMonster.MonsterType.fromNode(fiend, index))
                .isEqualTo(Json2QuteMonster.MonsterType.fiend);
    }

    @Test
    void findMonsterVariantsAliasesFluffAndTokenToParent() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Empyrean",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fiend"]}},
                  "hasToken": true,
                  "hasFluff": true,
                  "hasFluffImages": true
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        JsonNode celestial = variants.get(1);
        assertThat(celestial.get("name").asText()).isEqualTo("Empyrean (Celestial)");
        assertThat(Json2QuteMonster.MonsterFields.original.getTextOrNull(celestial)).isEqualTo("Empyrean");
        assertThat(TtrpgValue.indexFluffKey.getTextOrNull(celestial))
                .isEqualTo(Tools5eIndexType.monsterFluff.createKey("Empyrean", "TEST"));

        JsonNode fiend = variants.get(2);
        assertThat(Json2QuteMonster.MonsterFields.original.getTextOrNull(fiend)).isEqualTo("Empyrean");
        assertThat(TtrpgValue.indexFluffKey.getTextOrNull(fiend))
                .isEqualTo(Tools5eIndexType.monsterFluff.createKey("Empyrean", "TEST"));
    }

    @Test
    void findMonsterVariantsPreservesTypeTagsOnGeneratedSiblings() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Empyrean",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fiend"]}, "tags": ["titan"]}
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        JsonNode celestial = variants.get(1);
        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, celestial);
        assertThat(json2Qute.creatureType).isEqualTo("celestial");
        assertThat(json2Qute.subtype).isEqualTo("titan");
    }

    @Test
    void findMonsterVariantsCoveredByVersionsGeneratesNoSiblings() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Otherworldly Steed",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fey", "fiend"]}},
                  "_versions": [
                    {"name": "Otherworldly Steed (Celestial)", "source": "TEST", "type": "celestial"},
                    {"name": "Otherworldly Steed (Fey)", "source": "TEST", "type": "fey"},
                    {"name": "Otherworldly Steed (Fiend)", "source": "TEST", "type": "fiend"}
                  ]
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        // Original (unmodified, kept since no version name matches origname) + 3 versions = 4
        assertThat(variants).hasSize(4);
        assertThat(variants.get(0)).isSameAs(source);
    }

    @Test
    void findMonsterVariantsPartiallyCoveredGeneratesOnlyUncoveredSiblings() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Partial Steed",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fey", "fiend"]}},
                  "_versions": [
                    {"name": "Partial Steed (Celestial)", "source": "TEST", "type": "celestial"},
                    {"name": "Partial Steed (Fey)", "source": "TEST", "type": "fey"}
                  ]
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        // Original + 2 versions (covering celestial, fey) + 1 new sibling for fiend = 4
        assertThat(variants).hasSize(4);
        assertThat(variants.get(0)).isSameAs(source);

        JsonNode fiendSibling = variants.get(3);
        assertThat(fiendSibling.get("name").asText()).isEqualTo("Partial Steed (Fiend)");
        assertThat(fiendSibling.get("type").asText()).isEqualTo("fiend");
        assertThat(TtrpgValue.indexParentKey.getTextOrNull(fiendSibling)).isEqualTo(key);
    }

    @Test
    void findMonsterVariantsGeneratesFormAndTypeSiblingsWhenNoVersionOverridesType() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Battle Familiar",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fey", "fiend"]}},
                  "_versions": [
                    {"name": "Battle Familiar (Brute)", "source": "TEST"},
                    {"name": "Battle Familiar (Flyer)", "source": "TEST"},
                    {"name": "Battle Familiar (Stalker)", "source": "TEST"}
                  ]
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        // original (kept, no version matches origname) + 3 versions + 9 new (3 forms x 3 types)
        assertThat(variants).hasSize(13);

        List<String> names = variants.stream().map(v -> v.get("name").asText()).toList();
        assertThat(names).contains(
                "Battle Familiar",
                "Battle Familiar (Brute)", "Battle Familiar (Flyer)", "Battle Familiar (Stalker)",
                "Battle Familiar (Brute, Celestial)", "Battle Familiar (Brute, Fey)", "Battle Familiar (Brute, Fiend)",
                "Battle Familiar (Flyer, Celestial)", "Battle Familiar (Flyer, Fey)", "Battle Familiar (Flyer, Fiend)",
                "Battle Familiar (Stalker, Celestial)", "Battle Familiar (Stalker, Fey)", "Battle Familiar (Stalker, Fiend)");

        JsonNode bruteFiend = variants.stream()
                .filter(v -> v.get("name").asText().equals("Battle Familiar (Brute, Fiend)"))
                .findFirst().orElseThrow();
        assertThat(bruteFiend.get("type").asText()).isEqualTo("fiend");
        String bruteKey = Tools5eIndexType.monster.createKey("Battle Familiar (Brute)", "TEST");
        assertThat(TtrpgValue.indexParentKey.getTextOrNull(bruteFiend)).isEqualTo(bruteKey);

        // The original "choose" entries are kept unchanged (still ambiguous)
        JsonNode brute = variants.stream()
                .filter(v -> v.get("name").asText().equals("Battle Familiar (Brute)"))
                .findFirst().orElseThrow();
        assertThat(Json2QuteMonster.chooseTypeValues(brute)).containsExactly("celestial", "fey", "fiend");
    }

    @Test
    void findMonsterVariantsLeavesOrdinaryVersionsUnchanged() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Ordinary Beast",
                  "source": "TEST",
                  "type": "beast",
                  "_versions": [
                    {"name": "Ordinary Beast (Small)", "source": "TEST"},
                    {"name": "Ordinary Beast (Large)", "source": "TEST"}
                  ]
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        // original (kept, no version matches origname) + 2 versions, no choose-driven siblings
        assertThat(variants).hasSize(3);
        List<String> names = variants.stream().map(v -> v.get("name").asText()).toList();
        assertThat(names).containsExactly("Ordinary Beast", "Ordinary Beast (Small)", "Ordinary Beast (Large)");
    }

    @Test
    void generatedTypedSiblingRendersParentLink() throws Exception {
        // Build a small, fully-indexed Tools5eIndex (not just node construction) so the
        // backlink's lookup of the parent node/sources resolves the same way it would during
        // a real conversion run.
        Tui tui = new Tui();
        CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);
        new Configurator(config).allowSource("all");
        Tools5eIndex localIndex = new Tools5eIndex(config);
        Tools5eLinkifier.instance().reset();

        JsonNode tree = Tui.MAPPER.readTree("""
                {
                  "monster": [
                    {
                      "name": "Empyrean",
                      "source": "TEST",
                      "type": {"type": {"choose": ["celestial", "fiend"]}}
                    }
                  ]
                }
                """);
        localIndex.importTree("test.json", tree);
        localIndex.prepare();

        JsonNode celestial = localIndex.getOrigin(Tools5eIndexType.monster.createKey("Empyrean (Celestial)", "TEST"));
        assertThat(celestial).isNotNull();

        QuteMonster monster = (QuteMonster) new Json2QuteMonster(localIndex, Tools5eIndexType.monster, celestial).build();
        assertThat(monster.parentLink)
                .describedAs("generated typed sibling should have a working link back to the originating entry")
                .isNotNull()
                .contains("[Empyrean]")
                .contains("empyrean-test.md");

        // The originating "choose" entry itself should have no parentLink (nothing to link
        // forward to its generated siblings).
        JsonNode original = localIndex.getOrigin(Tools5eIndexType.monster.createKey("Empyrean", "TEST"));
        QuteMonster empyrean = (QuteMonster) new Json2QuteMonster(localIndex, Tools5eIndexType.monster, original).build();
        assertThat(empyrean.parentLink).isNull();
    }

    @Test
    void existingVersionEntryRendersParentLink() throws Exception {
        // S6a: a pre-existing `_versions` entry (not generated by this feature) already had
        // TtrpgValue.indexParentKey set before this change (hydrateVersion); it should now also
        // render a working parent link back to its base monster.
        Tui tui = new Tui();
        CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);
        new Configurator(config).allowSource("all");
        Tools5eIndex localIndex = new Tools5eIndex(config);
        Tools5eLinkifier.instance().reset();

        JsonNode tree = Tui.MAPPER.readTree("""
                {
                  "monster": [
                    {
                      "name": "Ordinary Beast",
                      "source": "TEST",
                      "type": "beast",
                      "_versions": [
                        {"name": "Ordinary Beast (Small)", "source": "TEST"}
                      ]
                    }
                  ]
                }
                """);
        localIndex.importTree("test.json", tree);
        localIndex.prepare();

        JsonNode small = localIndex.getOrigin(Tools5eIndexType.monster.createKey("Ordinary Beast (Small)", "TEST"));
        assertThat(small).isNotNull();

        QuteMonster monster = (QuteMonster) new Json2QuteMonster(localIndex, Tools5eIndexType.monster, small).build();
        assertThat(monster.parentLink)
                .describedAs("pre-existing version entry should now render a parent link to its base monster")
                .isNotNull()
                .contains("[Ordinary Beast]");
    }

    @Test
    void findMonsterVariantsLeavesNoChooseNoVersionsUnchanged() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Fiend",
                  "source": "TEST",
                  "type": "fiend"
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        assertThat(variants).hasSize(1);
        assertThat(variants.get(0)).isSameAs(source);
    }
}
