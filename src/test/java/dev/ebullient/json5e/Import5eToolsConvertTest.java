package dev.ebullient.json5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.json5e.io.Json5eTui;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class Import5eToolsConvertTest {
    static final Path outputPath = TestUtils.OUTPUT_ROOT.resolve("test-cli");
    static Json5eTui tui;

    @BeforeAll
    public static void setupDir() {
        tui = new Json5eTui();
        tui.init(null, true, true);
        outputPath.toFile().mkdirs();
    }

    @Test
    @Launch({ "--help" })
    void testCommandHelp(LaunchResult result) {
        result.echoSystemOut();
        assertThat(result.getOutput())
                .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                .contains("Usage: 5e-convert");
    }

    @Test
    @Launch({ "--version" })
    void testCommandVersion(LaunchResult result) {
        result.echoSystemOut();
        assertThat(result.getOutput())
                .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                .contains("5e-convert version");
    }

    @Test
    void testCommandLiveData(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            // SRD
            LaunchResult result = launcher.launch("--index", "-v",
                    "-o", outputPath.resolve("srd-index").toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            // Subset
            result = launcher.launch("--index", "-s", "PHB,DMG,XGE,SCAG", "-v",
                    "-o", outputPath.resolve("subset-index").toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }

    @Test
    void testCommandLiveDataAllSources(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            // All
            final Path allIndex = outputPath.resolve("all-index");
            TestUtils.deleteDir(allIndex);

            LaunchResult result = launcher.launch("--index", "-s", "ALL",
                    "-o", allIndex.toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            Json5eTui tui = new Json5eTui();
            tui.init(null, false, true);
            List<String> errors = new ArrayList<>();
            TestUtils.assertDirectoryContents(allIndex, tui, (p, content) -> {
                content.forEach(l -> TestUtils.checkMarkdownLinks(allIndex.toString(), p, l, errors));
                return List.of();
            });
        }
    }

    @Test
    void testCommandLiveDataOneSource(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            // No basics
            LaunchResult result = launcher.launch("-s", "ERLW", "-v",
                    "-o", outputPath.resolve("erlw").toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }

    @Test
    void testCommandTemplates(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path target = outputPath.resolve("srd-templates");

            // SRD only, just templates
            LaunchResult result = launcher.launch("-v",
                    "--background", "src/test/resources/other/background.txt",
                    "--class", "src/test/resources/other/class.txt",
                    "--deity", "src/test/resources/other/deity.txt",
                    "--feat", "src/test/resources/other/feat.txt",
                    "--item", "src/test/resources/other/item.txt",
                    "--name", "src/test/resources/other/name.txt",
                    "--note", "src/test/resources/other/note.txt",
                    "--race", "src/test/resources/other/race.txt",
                    "--spell", "src/test/resources/other/spell.txt",
                    "--subclass", "src/test/resources/other/subclass.txt",
                    "-o", target.toString(), TestUtils.TOOLS_PATH.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            List.of(
                    // target.resolve("compendium/backgrounds"), TODO
                    target.resolve("compendium/classes"),
                    target.resolve("compendium/deities"),
                    target.resolve("compendium/feats"),
                    target.resolve("compendium/items"),
                    target.resolve("compendium/races"),
                    target.resolve("compendium/spells")).forEach(directory -> {
                        TestUtils.assertDirectoryContents(directory, tui, new BiFunction<Path, List<String>, List<String>>() {
                            @Override
                            public List<String> apply(Path p, List<String> content) {
                                if (content.stream().noneMatch(l -> l.equals("- test")) &&
                                        content.stream().noneMatch(l -> l.startsWith("# Index"))) {
                                    return List.of("Unable to find the - test tag in file " + p);
                                }
                                return List.of();
                            }
                        });
                    });
        }
    }
}
