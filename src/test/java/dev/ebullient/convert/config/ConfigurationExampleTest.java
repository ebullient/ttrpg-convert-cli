package dev.ebullient.convert.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig.ConfigKeys;
import dev.ebullient.convert.io.Tui;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigurationExampleTest {
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

        JsonNode node = Tui.MAPPER.readTree(TtrpgConfig.class.getResourceAsStream("/sourceMap.json"));

        StringBuilder tools5e = new StringBuilder();
        writeToBuilder(ConfigKeys.config5e.get(node), tools5e);

        StringBuilder toolsPf2e = new StringBuilder();
        writeToBuilder(ConfigKeys.configPf2e.get(node), toolsPf2e);

        String result = Files.readString(in)
                .replace("<!--%% 5etools %% -->", tools5e.toString())
                .replace("<!--%% Pf2eTools %% -->", toolsPf2e.toString());
        Files.writeString(out, result, StandardOpenOption.CREATE);

    }

    void writeToBuilder(JsonNode configMap, StringBuilder builder) {
        if (ConfigKeys.abvToName.existsIn(configMap)) {
            builder.append("### Abbreviations to long name\n\n");
            builder.append("| Abbreviation | Long name |\n");
            builder.append("|--------------|-----------|\n");

            ConfigKeys.abvToName.getAsMap(configMap).entrySet()
                    .stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                    .forEach(e -> builder.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append(" |\n"));
        }

        if (ConfigKeys.longToAbv.existsIn(configMap)) {
            builder.append("\n");
            builder.append("### Alternate abbreviation mapping\n\n");
            builder.append(
                    "You may see these abbreviations referenced in source material, this is how they map to sources listed above.\n\n");
            builder.append("| Abbreviation | Alias     |\n");
            builder.append("|--------------|-----------|\n");

            ConfigKeys.longToAbv.getAsMap(configMap).entrySet()
                    .stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                    .forEach(e -> builder.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append(" |\n"));
        }

    }

    @Test
    public void exportExample() throws Exception {
        CompendiumConfig.InputConfig tools5Config = new CompendiumConfig.InputConfig();

        tools5Config.from.add("PHB");
        tools5Config.fullSource.book.add("PHB");
        tools5Config.fullSource.adventure.add("LMoP");
        tools5Config.fullSource.homebrew.add("homebrew/collection/Kobold Press; Deep Magic 14 Elemental Magic.json");
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
        tools5Config.images.checkRemote = false;
        tools5Config.images.copyRemote = false;
        tools5Config.images.relativeRemoteRoot = "alternate/path/for/remote/images";
        tools5Config.useDiceRoller = true;
        tools5Config.tagPrefix = "ttrpg-cli";

        tui.writeJsonFile(Path.of("examples/config/config.5e.json"), tools5Config);
        tui.writeYamlFile(Path.of("examples/config/config.5e.yaml"), tools5Config);

        CompendiumConfig.InputConfig pf2eConfig = new CompendiumConfig.InputConfig();
        pf2eConfig.from.add("CRB");
        pf2eConfig.from.add("GMG");
        pf2eConfig.fullSource.book.add("crb");
        pf2eConfig.fullSource.book.add("gmg");

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
}
