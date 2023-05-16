package dev.ebullient.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

import dev.ebullient.convert.io.Tui;
import io.quarkus.test.junit.main.LaunchResult;

public class TestUtils {
    public final static Path PROJECT_PATH = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    public final static Path OUTPUT_ROOT_5E = PROJECT_PATH.resolve("target/test-5e");
    public final static Path OUTPUT_ROOT_PF2 = PROJECT_PATH.resolve("target/test-pf2");

    public final static Path TEST_SOURCES_FROM_ALL = PROJECT_PATH.resolve("src/test/resources/sources-from-all.json");
    public final static Path TEST_FLAT_PATH_JSON = PROJECT_PATH.resolve("src/test/resources/paths.json");
    public final static Path TEST_SOURCES_BAD_TEMPL_JSON = PROJECT_PATH.resolve("src/test/resources/sources-bad-template.json");

    // for compile/test purposes. Must clone/sync separately.
    public final static Path TOOLS_PATH_5E = PROJECT_PATH.resolve("sources/5etools-mirror-1.github.io/data");
    public final static Path TEST_SOURCES_JSON_5E = PROJECT_PATH.resolve("src/test/resources/sources.json");
    public final static Path TEST_SOURCES_BOOK_ADV_JSON_5E = PROJECT_PATH
            .resolve("src/test/resources/sources-book-adventure.json");

    public final static Path TOOLS_PATH_PF2E = PROJECT_PATH.resolve("sources/Pf2eTools/data");

    final static Pattern markdownLinkPattern = Pattern.compile("\\[.*?]\\((.*?)\\)");
    final static Pattern blockRefPattern = Pattern.compile("[^#\\[]+(\\^[^ ]+)");
    final static String replaceHeaderParens = "\\((Level(%20| )\\d+|\\d+[thrds]+(%20| )level|\\d*d\\d+|(\\d+|one|two|three)(%20| )?(/Day|dice|die|extra|uses?)?|CR(%20| )[\\d/]+|Firearm)\\)";

    final static Map<Path, List<String>> pathHeadings = new HashMap<>();
    final static Map<Path, List<String>> pathBlockReferences = new HashMap<>();

    static void assertContents(Path path1, Path path2, boolean areEqual) throws IOException {
        try (RandomAccessFile randomAccessFile1 = new RandomAccessFile(path1.toFile(), "r");
                RandomAccessFile randomAccessFile2 = new RandomAccessFile(path2.toFile(), "r")) {

            FileChannel ch1 = randomAccessFile1.getChannel();
            FileChannel ch2 = randomAccessFile2.getChannel();
            if (areEqual) {
                assertThat(ch1.size()).isEqualTo(ch2.size());
            } else {
                assertThat(ch1.size()).isNotEqualTo(ch2.size());
            }

            MappedByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_ONLY, 0L, ch1.size());
            MappedByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_ONLY, 0L, ch2.size());

            if (areEqual) {
                assertThat(m1).isEqualTo(m2);
            } else {
                assertThat(m1).isNotEqualTo(m2);
            }
        }
    }

    static void checkMarkdownLinks(String baseDir, Path p, String line, List<String> errors) {
        if (!p.toString().endsWith(".md")) {
            return;
        }
        List<String> e = new ArrayList<>();
        // replace (Level x) or (1st level) or (Firearms) with :whatever: to simplify matching
        line = line.replaceAll(replaceHeaderParens, ":$1:");
        Matcher links = markdownLinkPattern.matcher(line);
        links.results().forEach(m -> {
            String path = m.group(1).replaceAll(" \\\".*\\\"$", "");
            String anchor = null;
            Path resource = p;
            int hash = path.indexOf('#');

            if (hash == 0) {
                anchor = path.substring(1);
            } else if (hash > 0) {
                anchor = path.substring(hash + 1);
                path = path.substring(0, hash);
                if (!p.toString().endsWith(path)) {
                    resource = Path.of(baseDir, path);
                }

                Path indexResource = p.getParent().resolve(path);
                if (!resource.toFile().exists() && !indexResource.toFile().exists()) {
                    e.add(String.format("Unresolvable reference (%s) in %s", m.group(0), p));
                    return;
                }
            }

            if (anchor != null) {
                if (!resource.toString().endsWith(".md")) {
                    return;
                }
                if (anchor.startsWith("^")) {
                    List<String> blockRefs = findBlockRefsIn(resource);
                    if (!blockRefs.contains(anchor)) {
                        e.add(String.format("Unresolvable block reference (%s) %s in %s", anchor, m.group(0), p));
                    }
                } else {
                    String heading = anchor.toLowerCase().replaceAll("%20", " ");
                    List<String> headings = findHeadingsIn(resource);
                    if (!headings.contains(heading)) {
                        e.add(String.format("Unresolvable anchor (%s) %s in %s", heading, m.group(0), p));
                    }
                }
            }
        });
        errors.addAll(e);
    }

    public static List<String> findHeadingsIn(Path p) {
        if (!p.toString().endsWith(".md")) {
            return List.of();
        }
        return pathHeadings.computeIfAbsent(p, key -> {
            List<String> headings = new ArrayList<>();
            try (Stream<String> lines = Files.lines(key)) {
                lines.forEach(l -> {
                    if (l.startsWith("#")) {
                        if (l.contains(".")) {
                            System.out.println("🔮 Found dot in heading in " + p + ": " + l);
                        }
                        headings.add(l
                                .replaceAll("#", "")
                                .replaceAll("\\.", "")
                                .replaceAll(":", "")
                                // replace (Level x) with :Level x: to simplify matching
                                .replaceAll(replaceHeaderParens, ":$1:")
                                .toLowerCase()
                                .trim());
                    }
                });
            } catch (UncheckedIOException | IOException e) {
                System.err.println(String.format("🛑 Error finding headings in %s: %s", p, e.toString()));
            }
            return headings;
        });
    }

    public static List<String> findBlockRefsIn(Path p) {
        if (!p.toString().endsWith(".md")) {
            return List.of();
        }
        return pathBlockReferences.computeIfAbsent(p, key -> {
            List<String> blockrefs = new ArrayList<>();
            try (Stream<String> lines = Files.lines(key)) {
                lines.forEach(l -> {
                    if (l.startsWith("^")) {
                        blockrefs.add(l.trim());
                    } else {
                        Matcher blockRefs = blockRefPattern.matcher(l);
                        blockRefs.results().forEach(m -> {
                            blockrefs.add(m.group(1));
                        });
                    }
                });
            } catch (UncheckedIOException | IOException e) {
                System.err.println(String.format("🛑 Error finding block references in %s: %s", p, e.toString()));
            }
            return blockrefs;
        });
    }

    public static void commonTests(Path p, String l, List<String> errors) {
        if (l.contains("{@")) {
            errors.add(String.format("Found {@ in %s: %s", p, l));
        }
        if (l.contains("{#")) {
            errors.add(String.format("Found {# in %s: %s", p, l));
        }
    }

    static final BiFunction<Path, List<String>, List<String>> checkContents = (p, content) -> {
        List<String> e = new ArrayList<>();
        content.forEach(l -> commonTests(p, l, e));
        return e;
    };

    public static void assertDirectoryContents(Path directory, Tui tui) {
        assertDirectoryContents(directory, tui, checkContents);
    }

    public static void assertDirectoryContents(Path directory, Tui tui, BiFunction<Path, List<String>, List<String>> checker) {
        List<String> errors = checkDirectoryContents(directory, tui, checker);
        assertThat(errors).isEmpty();
    }

    static List<String> checkDirectoryContents(Path directory, Tui tui) {
        return checkDirectoryContents(directory, tui, checkContents);
    }

    static List<String> checkDirectoryContents(Path directory, Tui tui,
            BiFunction<Path, List<String>, List<String>> checker) {
        List<String> errors = new ArrayList<>();
        try (Stream<Path> walk = Files.list(directory)) {
            walk.forEach(p -> {
                if (p.toFile().isDirectory()) {
                    errors.addAll(checkDirectoryContents(p, tui, checker));
                    return;
                }
                if (!p.toString().endsWith(".md")) {
                    if (!p.toString().endsWith(".png")
                            && !p.toString().endsWith(".jpg")
                            && !p.toString().endsWith(".svg")
                            && !p.toString().endsWith(".webp")
                            && !p.toString().endsWith(".json")
                            && !p.toString().endsWith(".yaml")) {
                        errors.add(String.format("Found file that was not markdown: %s", p));
                    }
                    return;
                }
                try {
                    errors.addAll(checker.apply(p, Files.readAllLines(p)));
                } catch (IOException e) {
                    e.printStackTrace();
                    errors.add(String.format("Unable to read lines from %s: %s", p, e));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            errors.add(String.format("Unable to parse files in directory %s: %s", directory, e));
        }

        return errors;
    }

    public static String dump(LaunchResult result) {
        return "\n" + result.getOutput() + "\nSystem err:\n" + result.getErrorOutput();
    }

    public static void deleteDir(Path path) {
        if (!path.toFile().exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Assertions.assertFalse(path.toFile().exists());
    }
}
