package dev.ebullient.json5e;

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
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

import dev.ebullient.json5e.io.Json5eTui;
import io.quarkus.test.junit.main.LaunchResult;

public class TestUtils {
    final static Path PROJECT_PATH = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    // for compile/test purposes. Must clone/sync separately.
    final static Path TOOLS_PATH = PROJECT_PATH.resolve("5etools-mirror-1.github.io/data");
    final static Path TEST_PATH_JSON = PROJECT_PATH.resolve("src/test/resources/paths.json");
    final static Path TEST_SOURCES_JSON = PROJECT_PATH.resolve("src/test/resources/sources.json");
    final static Path OUTPUT_ROOT = PROJECT_PATH.resolve("target/test-data");

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

    static void commonTests(Path p, String l, List<String> errors) {
        if (l.contains("{@")) {
            errors.add(String.format("Found {@ in %s: %s", p, l));
        }
        if (l.contains("{#")) {
            errors.add(String.format("Found {# in %s: %s", p, l));
        }
    }

    static BiFunction<Path, List<String>, List<String>> checkContents = new BiFunction<Path, List<String>, List<String>>() {
        @Override
        public List<String> apply(Path p, List<String> content) {
            List<String> e = new ArrayList<>();
            content.forEach(l -> commonTests(p, l, e));
            return e;
        }
    };

    static void assertDirectoryContents(Path directory, Json5eTui tui) {
        assertDirectoryContents(directory, tui, checkContents);
    }

    static void assertDirectoryContents(Path directory, Json5eTui tui, BiFunction<Path, List<String>, List<String>> checker) {
        List<String> errors = checkDirectoryContents(directory, tui, checkContents);
        assertThat(errors).isEmpty();
    }

    static List<String> checkDirectoryContents(Path directory, Json5eTui tui) {
        return checkDirectoryContents(directory, tui, checkContents);
    }

    static List<String> checkDirectoryContents(Path directory, Json5eTui tui,
            BiFunction<Path, List<String>, List<String>> checker) {
        List<String> errors = new ArrayList<>();
        try (Stream<Path> walk = Files.list(directory)) {
            walk.forEach(p -> {
                if (!p.toFile().isFile()) {
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

        if (!errors.isEmpty()) {
            errors.forEach(tui::warn);
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

        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Assertions.assertFalse(path.toFile().exists());
    }
}
