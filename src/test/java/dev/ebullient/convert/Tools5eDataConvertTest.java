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
public class Tools5eDataConvertTest {
    static Path outputPath_5e;
    static Tui tui;

    @BeforeAll
    public static void setupDir() {
        setupDir("Tools5eDataConvertTest");
    }

    public static void setupDir(String root) {
        tui = new Tui();
        tui.init(null, false, false);
        outputPath_5e = TestUtils.OUTPUT_ROOT_5E.resolve(root).resolve("test-cli");
        outputPath_5e.toFile().mkdirs();
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
    void testLiveData_5e(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            final Path target = outputPath_5e.resolve("srd-index");
            TestUtils.deleteDir(target);

            // SRD
            LaunchResult result = launcher.launch("--index",
                    "-o", target.toString(), TestUtils.TOOLS_PATH_5E.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            // Subset
            result = launcher.launch("--index", "-s", "PHB,DMG,XGE,SCAG",
                    "-o", outputPath_5e.resolve("subset-index").toString(), TestUtils.TOOLS_PATH_5E.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }

    @Test
    void testLiveData_5eAllSources(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            // All, I mean it. Really for real.. ALL.
            final Path allIndex = outputPath_5e.resolve("all-index");
            TestUtils.deleteDir(allIndex);

            List<String> args = new ArrayList<>(List.of("--index", "--debug", "-s", "ALL",
                    "--background", "examples/templates/tools5e/images-background2md.txt",
                    "--item", "examples/templates/tools5e/images-item2md.txt",
                    "--monster", "examples/templates/tools5e/images-monster2md.txt",
                    "--race", "examples/templates/tools5e/images-race2md.txt",
                    "--spell", "examples/templates/tools5e/images-spell2md.txt",
                    "-o", allIndex.toString(),
                    TestUtils.TOOLS_PATH_5E.toString()));

            args.addAll(TestUtils.getFilesFrom(TestUtils.TOOLS_PATH_5E.resolve("adventure")));
            args.addAll(TestUtils.getFilesFrom(TestUtils.TOOLS_PATH_5E.resolve("book")));

            LaunchResult result = launcher.launch(args.toArray(new String[0]));
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            Tui tui = new Tui();
            tui.init(null, false, false);
            TestUtils.assertDirectoryContents(allIndex, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> TestUtils.checkMarkdownLinks(allIndex.toString(), p, l, errors));
                return errors;
            });
        }
    }

    @Test
    void testLiveData_5eOneSource(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path target = outputPath_5e.resolve("erlw");
            TestUtils.deleteDir(target);

            // No basics
            LaunchResult result = launcher.launch("-s", "ERLW",
                    "-o", target.toString(), TestUtils.TOOLS_PATH_5E.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }

    @Test
    void testLiveData_5eHomebrew(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists() && TestUtils.HOMEBREW_PATH_5E.toFile().exists()) {
            Path target = outputPath_5e.resolve("homebrew");
            TestUtils.deleteDir(target);

            // No basics
            LaunchResult result = launcher.launch("--debug", "--index",
                    "-c", TestUtils.TEST_RESOURCES.resolve("sources-homebrew.json").toString(),
                    "-o", target.toString(),
                    TestUtils.TOOLS_PATH_5E.toString(),
                    TestUtils.TEST_RESOURCES.resolve("psion.json").toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("adventure/Anthony Joyce; The Blood Hunter Adventure.json")
                            .toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("adventure/Kobold Press; Book of Lairs.json").toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("background/D&D Wiki; Featured Quality Backgrounds.json")
                            .toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("book/Darrington Press; Tal'Dorei Campaign Setting Reborn.json")
                            .toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("book/Ghostfire Gaming; Grim Hollow Campaign Guide.json")
                            .toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("book/Ghostfire Gaming; Stibbles Codex of Companions.json")
                            .toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("book/MCDM Productions; Arcadia Issue 3.json").toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("class/badooga; Badooga's Psion.json").toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("class/D&D Wiki; Swashbuckler.json").toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("class/Foxfire94; Vampire.json").toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("collection/Arcana Games; Arkadia.json").toString(),
                    TestUtils.HOMEBREW_PATH_5E
                            .resolve("collection/Ghostfire Gaming; Grim Hollow - The Monster Grimoire.json")
                            .toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("collection/Keith Baker; Exploring Eberron.json").toString(),
                    TestUtils.HOMEBREW_PATH_5E
                            .resolve("collection/MCDM Productions; The Talent and Psionics Open Playtest Round 2.json")
                            .toString(),
                    TestUtils.HOMEBREW_PATH_5E
                            .resolve("creature/Nerzugal Role-Playing; Nerzugal's Extended Bestiary.json")
                            .toString(),
                    TestUtils.HOMEBREW_PATH_5E.resolve("deity/Frog God Games; The Lost Lands.json").toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            TestUtils.assertDirectoryContents(target, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> TestUtils.checkMarkdownLinks(target.toString(), p, l, errors));
                return errors;
            });
        }
    }

    @Test
    void testCommandTemplates_5e(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path target = outputPath_5e.resolve("srd-templates");
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
                    "-o", target.toString(), TestUtils.TOOLS_PATH_5E.toString());

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
    void testCommandBadTemplates(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path target = outputPath_5e.resolve("bad-templates");

            LaunchResult result = launcher.launch("--index",
                    "--background=garbage.txt",
                    "-o", target.toString(),
                    TestUtils.TOOLS_PATH_5E.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command did not fail as expected. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(CommandLine.ExitCode.USAGE);
        }
    }

    @Test
    void testCommandBadTemplatesInJson(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path target = outputPath_5e.resolve("bad-templates-json");

            LaunchResult result = launcher.launch("--index",
                    "-o", target.toString(),
                    TestUtils.TOOLS_PATH_5E.toString(),
                    TestUtils.TEST_RESOURCES.resolve("sources-bad-template.json").toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command did not fail as expected. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(CommandLine.ExitCode.USAGE);
        }
    }

    @Test
    void testCommand_5eBookAdventureInJson(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path target = outputPath_5e.resolve("json-book-adventure");
            TestUtils.deleteDir(target);

            LaunchResult result = launcher.launch("--index",
                    "-o", target.toString(),
                    TestUtils.TOOLS_PATH_5E.toString(),
                    TestUtils.TEST_RESOURCES.resolve("sources-book-adventure.json").toString());

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
