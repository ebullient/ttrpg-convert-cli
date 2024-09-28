package dev.ebullient.convert.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.TtrpgConfig.ConfigKeys;
import dev.ebullient.convert.config.TtrpgConfig.SourceReference;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ExportDocsTest {
    protected static Tui tui;

    @BeforeAll
    public static void prepare() {
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);
    }

    @Test
    public void exportSourceMap() throws Exception {
        Path in = Path.of("src/test/resources/sourcemap.txt");
        Path out = Path.of("docs/sourceMap.md");
        Path sourceTypes = Path.of("src/test/resources/5e-sourceTypes.json");

        final SourceTypes types;

        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            JsonNode adventures = Tui.MAPPER.readTree(TestUtils.PATH_5E_TOOLS_DATA.resolve("adventures.json").toFile());
            var adventureIds = AdventureList.adventure.streamFrom(adventures)
                    .map(x -> x.get("id").asText())
                    .toList();

            JsonNode books = Tui.MAPPER.readTree(TestUtils.PATH_5E_TOOLS_DATA.resolve("books.json").toFile());
            var bookIds = AdventureList.book.streamFrom(books)
                    .map(x -> x.get("id").asText())
                    .toList();

            types = new SourceTypes(adventureIds, bookIds);

            // Update list
            tui.writeJsonFile(sourceTypes, types);
        } else {
            types = Tui.MAPPER.readValue(sourceTypes.toFile(), SourceTypes.class);
        }

        JsonNode node = Tui.readTreeFromResource("/sourceMap.yaml");

        StringBuilder tools5e = new StringBuilder();
        writeToBuilder(ConfigKeys.config5e.getFrom(node), tools5e, types, "5eTools");

        StringBuilder toolsPf2e = new StringBuilder();
        writeToBuilder(ConfigKeys.configPf2e.getFrom(node), toolsPf2e, null, "Pf2eTools");

        String result = Files.readString(in)
                .replace("<!--%% 5etools %% -->", tools5e.toString())
                .replace("<!--%% Pf2eTools %% -->", toolsPf2e.toString());
        Files.writeString(out, result, StandardOpenOption.CREATE);
    }

    void writeToBuilder(JsonNode configMap, StringBuilder builder, SourceTypes sourceTypes, String section) {
        if (ConfigKeys.reference.existsIn(configMap)) {
            if (sourceTypes == null) {
                builder.append("### " + section + " Abbreviations to long name\n\n");
                builder.append("| Abbreviation | Long name |\n");
                builder.append("|--------------|-----------|\n");
            } else {
                builder.append("### " + section + " Abbreviations to long name\n\n");
                builder.append("| Abbreviation | Long name | Type |\n");
                builder.append("|--------------|-----------|-------|\n");
            }

            ConfigKeys.reference.getAs(configMap, TtrpgConfig.MAP_REFERENCE).entrySet()
                    .stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                    .forEach(e -> {
                        SourceReference ref = e.getValue();
                        builder.append("| ").append(e.getKey())
                                .append(" | ").append(ref.name);
                        if (sourceTypes != null) {
                            String type = "reference";
                            if (sourceTypes.adventure.contains(e.getKey())) {
                                type = "adventure";
                            } else if (sourceTypes.book.contains(e.getKey())) {
                                type = "book";
                            }
                            builder.append(" | ").append(type);
                        }
                        builder.append(" |\n");
                    });
        }

        if (ConfigKeys.longToAbv.existsIn(configMap)) {
            builder.append("\n");
            builder.append("### " + section + " Alternate abbreviation mapping\n\n");
            builder.append(
                    "You may see these abbreviations referenced in source material, this is how they map to sources listed above.\n\n");
            builder.append("| Abbreviation | Alias     |\n");
            builder.append("|--------------|-----------|\n");

            ConfigKeys.longToAbv.getAsMap(configMap).entrySet()
                    .stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                    .forEach(e -> builder.append("| ").append(e.getKey()).append(" | ").append(e.getValue())
                            .append(" |\n"));
        }
    }

    @Test
    public void exportExample() throws Exception {
        UserConfig tools5Config = new UserConfig();

        tools5Config.sources.toolsRoot = "local/5etools/data";
        tools5Config.sources.reference.add("DMG");
        tools5Config.sources.book.add("PHB");
        tools5Config.sources.adventure.add("LMoP");
        tools5Config.sources.homebrew.add("homebrew/collection/Kobold Press; Deep Magic 14 Elemental Magic.json");

        tools5Config.paths.compendium = "/compendium/";
        tools5Config.paths.rules = "/compendium/rules/";

        tools5Config.excludePattern.add("race|.*|dmg");
        tools5Config.exclude.addAll(List.of(
                "monster|expert|dc",
                "monster|expert|sdw",
                "monster|expert|slw"));
        tools5Config.include.add("race|changeling|mpmm");
        tools5Config.includeGroup.add("familiars");

        tools5Config.template.put("background", "examples/templates/tools5e/images-background2md.txt");

        tools5Config.images.copyExternal = Boolean.TRUE;
        tools5Config.images.copyInternal = Boolean.TRUE;
        tools5Config.images.internalRoot = "local/path/for/remote/images";

        tools5Config.useDiceRoller = true;
        tools5Config.yamlStatblocks = true;
        tools5Config.tagPrefix = "ttrpg-cli";

        tui.writeJsonFile(Path.of("examples/config/config.5e.json"), tools5Config);
        tui.writeYamlFile(Path.of("examples/config/config.5e.yaml"), tools5Config);

        UserConfig pf2eConfig = new UserConfig();
        pf2eConfig.sources.reference.add("CRB");
        pf2eConfig.sources.reference.add("GMG");
        pf2eConfig.sources.book.add("crb");
        pf2eConfig.sources.book.add("gmg");

        pf2eConfig.paths.compendium = "compendium/";
        pf2eConfig.paths.rules = "compendium/rules/";

        pf2eConfig.include.add("ability|buck|b1");
        pf2eConfig.exclude.add("background|insurgent|apg");
        pf2eConfig.excludePattern.add("background|.*|lowg");
        pf2eConfig.template.put("ability", "../path/to/ability2md.txt");

        pf2eConfig.tagPrefix = "ttrpg-cli";
        pf2eConfig.useDiceRoller = true;

        tui.writeJsonFile(Path.of("examples/config/config.pf2e.json"), pf2eConfig);
        tui.writeYamlFile(Path.of("examples/config/config.pf2e.yaml"), pf2eConfig);
    }

    @Test
    public void exportSchema() throws IOException {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON)
                .withObjectMapper(Tui.MAPPER)
                .build();

        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(UserConfig.class);

        Files.writeString(Path.of("examples/config/config.schema.json"), jsonSchema.toPrettyString());
    }

    static class SourceTypes {
        List<String> adventure = new ArrayList<>();
        List<String> book = new ArrayList<>();

        SourceTypes() {
        }

        SourceTypes(List<String> adventure, List<String> book) {
            this.adventure.addAll(adventure);
            this.book.addAll(book);
        }
    }

    enum AdventureList implements JsonNodeReader {
        adventure,
        book,
        id
    }
}
