package dev.ebullient.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.dnd5e.Tools5eLinkifier;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class CustomTemplatesTest {
    static Path rootTestOutput;
    static Tui tui;

    Path testOutput;

    @BeforeAll
    public static void setupDir() {
        setupDir("templates");
    }

    public static void setupDir(String name) {
        tui = new Tui();
        tui.init(null, false, false);
        rootTestOutput = TestUtils.OUTPUT_ROOT_5E.resolve(name);
        rootTestOutput.toFile().mkdirs();
    }

    @AfterAll
    public static void cleanup() {
        System.out.println("Done.");
    }

    @BeforeEach
    public void setup() {
        testOutput = null; // test should set this to something readable
    }

    @AfterEach
    public void moveLogFile() throws IOException {
        assertThat(testOutput).isNotNull(); // make sure test set this

        Path logFile = Path.of("ttrpg-convert.out.txt");
        if (Files.exists(logFile) && Files.exists(testOutput)) {
            String content = Files.readString(logFile, StandardCharsets.UTF_8);

            Path filePath = testOutput.resolve(logFile);
            Files.move(logFile, filePath, StandardCopyOption.REPLACE_EXISTING);

            if (content.contains("Exception")) {
                tui.errorf("Exception found in %s", filePath);
            }
        }
        TestUtils.cleanupReferences();
    }

    @Test
    @Launch({ "--help" })
    void testCommandHelp(LaunchResult result) {
        testOutput = rootTestOutput.resolve("help");
        result.echoSystemOut();
        assertThat(result.getOutput())
                .withFailMessage("Usage statement not found in output. Output:%n%s", TestUtils.dump(result))
                .contains("Usage:");
    }

    @Test
    @Launch({ "--version" })
    void testCommandVersion(LaunchResult result) {
        testOutput = rootTestOutput.resolve("version");
        result.echoSystemOut();
        assertThat(result.getOutput())
                .withFailMessage("Version statement not found in output. Output:%n%s", TestUtils.dump(result))
                .contains("ttrpg-convert version");
    }

    @Test
    void testCommandBadTemplatesInJson(QuarkusMainLauncher launcher) throws IOException {
        testOutput = rootTestOutput.resolve("bad-templates-json");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            TestUtils.deleteDir(testOutput);

            LaunchResult result = launcher.launch("--index",
                    "-o", testOutput.toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString(),
                    TestUtils.TEST_RESOURCES.resolve("sources-bad-template.json").toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command did not fail as expected. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(2);
        }
    }

    @Test
    void testCommandTemplates_5eJson(QuarkusMainLauncher launcher) throws IOException {
        testOutput = rootTestOutput.resolve("json-templates");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            TestUtils.deleteDir(testOutput);

            LaunchResult result = launcher.launch("--log", "--index",
                    "-c", TestUtils.TEST_RESOURCES.resolve("5e/sources-templates.json").toString(),
                    "-o", testOutput.toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            // test extra cp value attribute in yaml frontmatter
            Path abacus = testOutput.resolve("compendium/items/%s.md".formatted(
                    Tools5eLinkifier.instance().getTargetFileName("Abacus", "PHB", Tools5eIndexType.item)));
            assertThat(abacus).exists();
            assertThat(abacus).content().contains("cost: 200");

            List.of(
                    testOutput.resolve("compendium/backgrounds"),
                    testOutput.resolve("compendium/bestiary"),
                    testOutput.resolve("compendium/classes"),
                    testOutput.resolve("compendium/deities"),
                    testOutput.resolve("compendium/feats"),
                    testOutput.resolve("compendium/items"),
                    testOutput.resolve("compendium/races"),
                    testOutput.resolve("compendium/spells"),
                    testOutput.resolve("rules"))
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
                                if (l.contains("\\")) {
                                    errors.add(
                                            String.format("Backslash in heading/link in %s: %s", p.toString(), l));
                                }
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
