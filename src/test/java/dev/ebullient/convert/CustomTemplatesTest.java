package dev.ebullient.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.io.Tui;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import picocli.CommandLine;

@QuarkusMainTest
public class CustomTemplatesTest {
    static Path testOutput;
    static Tui tui;

    @BeforeAll
    public static void setupDir() {
        setupDir("Tools5eDataConvertTest");
    }

    public static void setupDir(String root) {
        tui = new Tui();
        tui.init(null, false, false);
        testOutput = TestUtils.OUTPUT_ROOT_5E.resolve(root).resolve("test-cli");
        testOutput.toFile().mkdirs();
    }

    @Test
    @Launch({ "--help" })
    void testCommandHelp(LaunchResult result) {
        result.echoSystemOut();
        assertThat(result.getOutput())
                .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                .contains("Usage: ttrpg-convert");
    }

    @Test
    @Launch({ "--version" })
    void testCommandVersion(LaunchResult result) {
        result.echoSystemOut();
        assertThat(result.getOutput())
                .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                .contains("ttrpg-convert version");
    }

    @Test
    void testCommandBadTemplates(QuarkusMainLauncher launcher) {
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            Path target = testOutput.resolve("bad-templates");

            LaunchResult result = launcher.launch("--index",
                    "--background=garbage.txt",
                    "-o", target.toString(),
                    TestUtils.TEST_RESOURCES.resolve("images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command did not fail as expected. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(CommandLine.ExitCode.USAGE);
        }
    }

    @Test
    void testCommandBadTemplatesInJson(QuarkusMainLauncher launcher) {
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            Path target = testOutput.resolve("bad-templates-json");

            LaunchResult result = launcher.launch("--index",
                    "-o", target.toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString(),
                    TestUtils.TEST_RESOURCES.resolve("images-remote.json").toString(),
                    TestUtils.TEST_RESOURCES.resolve("sources-bad-template.json").toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command did not fail as expected. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(CommandLine.ExitCode.USAGE);
        }
    }

    @Test
    void testCommandTemplates_5e(QuarkusMainLauncher launcher) {
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            Path target = testOutput.resolve("srd-templates");
            TestUtils.deleteDir(target);

            // SRD only, just templates
            LaunchResult result = launcher.launch(
                    "--background", TestUtils.TEST_RESOURCES.resolve("other/background.txt").toString(),
                    "--class", TestUtils.TEST_RESOURCES.resolve("other/class.txt").toString(),
                    "--deity", TestUtils.TEST_RESOURCES.resolve("other/deity.txt").toString(),
                    "--feat", TestUtils.TEST_RESOURCES.resolve("other/feat.txt").toString(),
                    "--item", TestUtils.TEST_RESOURCES.resolve("other/item.txt").toString(),
                    "--note", TestUtils.TEST_RESOURCES.resolve("other/note.txt").toString(),
                    "--race", TestUtils.TEST_RESOURCES.resolve("other/race.txt").toString(),
                    "--spell", TestUtils.TEST_RESOURCES.resolve("other/spell.txt").toString(),
                    "--subclass", TestUtils.TEST_RESOURCES.resolve("other/subclass.txt").toString(),
                    "-o", target.toString(),
                    TestUtils.TEST_RESOURCES.resolve("images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            List.of(
                    target.resolve("compendium/backgrounds"),
                    target.resolve("compendium/classes"),
                    target.resolve("compendium/deities"),
                    target.resolve("compendium/feats"),
                    target.resolve("compendium/items"),
                    target.resolve("compendium/races"),
                    target.resolve("compendium/spells"),
                    target.resolve("rules"))
                    .forEach(directory -> TestUtils.assertDirectoryContents(directory, tui, (p, content) -> {
                        List<String> errors = new ArrayList<>();
                        boolean index = false;
                        boolean frontmatter = false;
                        boolean foundTestTag = false;
                        for (String l : content) {
                            if ("---".equals(l)) {
                                frontmatter = !frontmatter;
                            } else if (frontmatter && l.equals("- test")) {
                                foundTestTag = true;
                            } else if (l.startsWith("# Index ")) {
                                index = true;
                            } else if (l.startsWith("# ") && !l.matches("^# \\[.*]\\(.*\\)")) {
                                errors.add(
                                        String.format("H1 does not contain markdown link in %s: %s", p.toString(), l));
                            }
                            if (l.startsWith("# ")) {
                                break;
                            }
                        }

                        if (!index && !foundTestTag) {
                            errors.add("Unable to find the - test tag in file " + p);
                        }
                        return errors;
                    }));
        }
    }

    @Test
    void testCommandTemplates_5eJson(QuarkusMainLauncher launcher) {
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            Path target = testOutput.resolve("json-templates");
            TestUtils.deleteDir(target);

            LaunchResult result = launcher.launch("--debug", "--index",
                    "-c", TestUtils.TEST_RESOURCES.resolve("sources-templates.json").toString(),
                    "-o", target.toString(),
                    TestUtils.TEST_RESOURCES.resolve("images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            // test extra cp value attribute in yaml frontmatter
            Path abacus = target.resolve("compendium/items/abacus.md");
            assertThat(abacus).exists();
            assertThat(abacus).content().contains("cost: 200");

            List.of(
                    target.resolve("compendium/backgrounds"),
                    target.resolve("compendium/classes"),
                    target.resolve("compendium/deities"),
                    target.resolve("compendium/feats"),
                    target.resolve("compendium/items"),
                    target.resolve("compendium/races"),
                    target.resolve("compendium/spells"),
                    target.resolve("rules"))
                    .forEach(directory -> TestUtils.assertDirectoryContents(directory, tui, (p, content) -> {
                        List<String> errors = new ArrayList<>();
                        boolean frontmatter = false;
                        boolean foundTestTag = false;
                        for (String l : content) {
                            if ("---".equals(l)) {
                                frontmatter = !frontmatter;
                            } else if (frontmatter && l.equals("- test")) {
                                foundTestTag = true;
                            } else if (l.startsWith("# ")) {
                                break;
                            }
                        }

                        if (!foundTestTag) {
                            errors.add("Unable to find the - test tag in file " + p);
                        }
                        return errors;
                    }));
        }
    }
}
