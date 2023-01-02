package dev.ebullient.json5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.json5e.io.Json5eTui;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import picocli.CommandLine;

@QuarkusMainTest
public class Import5eToolsConvertTest {
    static final Path outputPath = TestUtils.OUTPUT_ROOT.resolve("test-cli");
    static Json5eTui tui;

    @BeforeAll
    public static void setupDir() {
        tui = new Json5eTui();
        tui.init(null, false, false);
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
            final Path target = outputPath.resolve("srd-index");
            TestUtils.deleteDir(target);

            // SRD
            LaunchResult result = launcher.launch("--index",
                    "-o", target.toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            // Subset
            result = launcher.launch("--index", "-s", "PHB,DMG,XGE,SCAG",
                    "-o", outputPath.resolve("subset-index").toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }

    @Test
    void testCommandLiveDataAllSources(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            // All, I mean it. Really for real.. ALL.
            final Path allIndex = outputPath.resolve("all-index");
            TestUtils.deleteDir(allIndex);

            List<String> args = new ArrayList<>(List.of("--index", "-s", "ALL",
                    "--background", "src/main/resources/templates/images-background2md.txt",
                    "--item", "src/main/resources/templates/images-item2md.txt",
                    "--monster", "src/main/resources/templates/images-monster2md.txt",
                    "--race", "src/main/resources/templates/images-race2md.txt",
                    "-o", allIndex.toString(),
                    TestUtils.TOOLS_PATH.toString()));

            args.addAll(getFilesFrom(TestUtils.TOOLS_PATH.resolve("adventure")));
            args.addAll(getFilesFrom(TestUtils.TOOLS_PATH.resolve("book")));

            LaunchResult result = launcher.launch(args.toArray(new String[0]));
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            Json5eTui tui = new Json5eTui();
            tui.init(null, false, false);
            List<String> errors = new ArrayList<>();
            TestUtils.assertDirectoryContents(allIndex, tui, (p, content) -> {
                content.forEach(l -> TestUtils.checkMarkdownLinks(allIndex.toString(), p, l, errors));
                return List.of();
            });
        }
    }

    List<String> getFilesFrom(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                    .filter(p -> p.toFile().isFile())
                    .map(Path::toString)
                    .filter(s -> s.endsWith(".json"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCommandLiveDataOneSource(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path target = outputPath.resolve("erlw");
            TestUtils.deleteDir(target);

            // No basics
            LaunchResult result = launcher.launch("-s", "ERLW",
                    "-o", target.toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }

    @Test
    void testCommandTemplates(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path target = outputPath.resolve("srd-templates");
            TestUtils.deleteDir(target);

            // SRD only, just templates
            LaunchResult result = launcher.launch(
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
                    target.resolve("compendium/spells"))
                    .forEach(directory -> TestUtils.assertDirectoryContents(directory, tui, (p, content) -> {
                        if (content.stream().noneMatch(l -> l.equals("- test")) &&
                                content.stream().noneMatch(l -> l.startsWith("# Index"))) {
                            return List.of("Unable to find the - test tag in file " + p);
                        }
                        return List.of();
                    }));
        }
    }

    @Test
    void testCommandBadTemplates(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path target = outputPath.resolve("bad-templates");

            LaunchResult result = launcher.launch("--index",
                    "--background=garbage.txt",
                    "-o", target.toString(),
                    TestUtils.TOOLS_PATH.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command did not fail as expected. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(CommandLine.ExitCode.USAGE);
        }
    }

    @Test
    void testCommandBadTemplatesInJson(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path target = outputPath.resolve("bad-templates-json");

            LaunchResult result = launcher.launch("--index",
                    "-o", target.toString(),
                    TestUtils.TOOLS_PATH.toString(),
                    TestUtils.TEST_SOURCES_BAD_TEMPL_JSON.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command did not fail as expected. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(CommandLine.ExitCode.USAGE);
        }
    }

    @Test
    void testCommandBookAdventureInJson(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path target = outputPath.resolve("json-book-adventure");
            TestUtils.deleteDir(target);

            LaunchResult result = launcher.launch("--index",
                    "-o", target.toString(),
                    TestUtils.TOOLS_PATH.toString(),
                    TestUtils.TEST_SOURCES_BOOK_ADV_JSON.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            Path wbtw = target.resolve("compend ium/adventures/the-wild-beyond-the-witchlight");
            assertThat(wbtw).exists();
            assertThat(wbtw).isDirectory();

            Path phb = target.resolve("compend ium/books/players-handbook");
            assertThat(phb).exists();
            assertThat(phb).isDirectory();

            List.of(
                    target.resolve("compend ium/adventures"),
                    target.resolve("compend ium/backgrounds"),
                    target.resolve("compend ium/books"),
                    target.resolve("compend ium/classes"),
                    target.resolve("compend ium/deities"),
                    target.resolve("compend ium/feats"),
                    target.resolve("compend ium/items"),
                    target.resolve("compend ium/races"),
                    target.resolve("compend ium/spells"))
                    .forEach(directory -> TestUtils.assertDirectoryContents(directory, tui, (p, content) -> {
                        List<String> errors = new ArrayList<>();
                        if (content.stream().anyMatch(l -> l.contains("/ru les/"))) {
                            errors.add("Found '/ru les/' " + p); // not escaped
                        }
                        if (content.stream().anyMatch(l -> l.contains("/compend ium/"))) {
                            errors.add("Found '/compend ium/' " + p); // not escaped
                        }
                        return errors;
                    }));
        }
    }
}
