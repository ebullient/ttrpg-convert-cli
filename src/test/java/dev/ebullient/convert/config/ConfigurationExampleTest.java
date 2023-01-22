package dev.ebullient.convert.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Path out = Path.of("examples/config/sourceMap.md");

        JsonNode node = Tui.MAPPER.readTree(TtrpgConfig.class.getResourceAsStream("/sourceMap.json"));
        JsonNode config5e = ConfigKeys.config5e.get(node);
        JsonNode configPf2e = ConfigKeys.configPf2e.get(node);

        StringBuilder tools5e = new StringBuilder();

        tools5e.append("| Abbreviation | Long name |\n");
        tools5e.append("|--------------|-----------|\n");

        StringBuilder toolsPf2e = new StringBuilder();
        toolsPf2e.append(tools5e.toString());

        ConfigKeys.abvToName.getAsMap(config5e).entrySet()
                .stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                .forEach(e -> tools5e.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append(" |\n"));

        ConfigKeys.abvToName.getAsMap(configPf2e).entrySet()
                .stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                .forEach(e -> toolsPf2e.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append(" |\n"));

        String result = Files.readString(in)
                .replace("<!--%% 5etools %% -->", tools5e.toString())
                .replace("<!--%% Pf2eTools %% -->", toolsPf2e.toString());
        Files.writeString(out, result, StandardOpenOption.CREATE);

    }

    @Test
    public void exportExample() throws Exception {
        Map<String, Object> map = new HashMap<>();
        CompendiumConfig.InputConfig tools5Config = new CompendiumConfig.InputConfig();
        map.put(Datasource.tools5e.shortName(), tools5Config);

        tools5Config.from.add("PHB");
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
        tools5Config.fullSource.book.add("PHB");
        tools5Config.fullSource.adventure.add("LMoP");

        CompendiumConfig.InputConfig p2feConfig = new CompendiumConfig.InputConfig();
        map.put(Datasource.toolsPf2e.shortName(), p2feConfig);

        tools5Config.from.add("CRB");

        tui.writeJsonFile(Path.of("examples/config/config.json"), map);
        tui.writeYamlFile(Path.of("examples/config/config.yaml"), map);
    }
}
