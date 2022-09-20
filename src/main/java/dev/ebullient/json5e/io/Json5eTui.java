package dev.ebullient.json5e.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.github.slugify.Slugify;

import dev.ebullient.json5e.qute.ImageRef;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;

@ApplicationScoped
public class Json5eTui {
    public final static ObjectMapper MAPPER = new ObjectMapper()
            .setVisibility(VisibilityChecker.Std.defaultInstance().with(JsonAutoDetect.Visibility.ANY));

    private static Slugify slugify;

    static Slugify slugifier() {
        Slugify s = slugify;
        if (s == null) {
            slugify = s = Slugify.builder()
                    .customReplacement("\"", "")
                    .customReplacement("'", "")
                    .lowerCase(true)
                    .build();
        }
        return s;
    }

    private static Yaml plainYaml;

    public static Yaml plainYaml() {
        Yaml y = plainYaml;
        if (y == null) {
            DumperOptions options = new DumperOptions();
            options.setDefaultScalarStyle(ScalarStyle.PLAIN);
            options.setPrettyFlow(true);
            y = plainYaml = new Yaml(options);
        }
        return y;
    }

    private static Yaml quotedYaml;

    public static Yaml quotedYaml() {
        Yaml y = quotedYaml;
        if (y == null) {
            DumperOptions options = new DumperOptions();
            options.setDefaultScalarStyle(ScalarStyle.DOUBLE_QUOTED);
            options.setPrettyFlow(true);
            y = quotedYaml = new Yaml(options);
        }
        return y;
    }

    static final boolean picocliDebugEnabled = "DEBUG".equalsIgnoreCase(System.getProperty("picocli.trace"));

    public static final int NOT_FOUND = 3;
    public static final int INSUFFICIENT_FUNDS = 4;

    Ansi ansi;
    ColorScheme colors;

    PrintWriter out;
    PrintWriter err;

    private boolean debug;
    private boolean verbose;
    private Path output = Paths.get("");
    private Set<Path> inputRoot = new TreeSet<>();

    public Json5eTui() {
        this.ansi = Help.Ansi.OFF;
        this.colors = Help.defaultColorScheme(ansi);

        this.out = new PrintWriter(System.out);
        this.err = new PrintWriter(System.err);
        this.debug = false;
        this.verbose = true;
    }

    public void init(CommandSpec spec, boolean debug, boolean verbose) {
        if (spec != null) {
            this.ansi = spec.commandLine().getHelp().ansi();
            this.colors = spec.commandLine().getHelp().colorScheme();
            this.out = spec.commandLine().getOut();
            this.err = spec.commandLine().getErr();
        }

        this.debug = debug;
        this.verbose = verbose;
    }

    public void setOutputPath(Path output) {
        this.output = output;
    }

    public void close() {
        out.flush();
        err.flush();
    }

    public boolean isDebug() {
        return debug || picocliDebugEnabled;
    }

    public void debugf(String format, Object... params) {
        if (isDebug()) {
            debug(String.format(format, params));
        }
    }

    public void debug(String output) {
        if (isDebug()) {
            out.println(ansi.new Text("@|faint 🔧 " + output + "|@", colors));
        }
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void verbosef(String format, Object... params) {
        if (isVerbose()) {
            verbose(String.format(format, params));
        }
    }

    public void verbose(String output) {
        if (isVerbose()) {
            out.println(ansi.new Text("@|faint 🔹 " + output + "|@", colors));
        }
    }

    public void warnf(String format, Object... params) {
        warn(String.format(format, params));
    }

    public void warn(String output) {
        out.println(ansi.new Text("🔸 " + output));
    }

    public void donef(String format, Object... params) {
        done(String.format(format, params));
    }

    public void done(String output) {
        out.println(ansi.new Text("✅ " + output));
    }

    public void outPrintf(String format, Object... args) {
        String output = String.format(format, args);
        out.print(ansi.new Text(output, colors));
        out.flush();
    }

    public void outPrintln(String output) {
        out.println(ansi.new Text(output));
        out.flush();
    }

    public void errorf(String format, Object... args) {
        error(null, String.format(format, args));
    }

    public void errorf(Throwable th, String format, Object... args) {
        error(th, String.format(format, args));
    }

    public void error(String errorMsg) {
        error(null, errorMsg);
    }

    public void error(Throwable ex, String errorMsg) {
        err.println(ansi.new Text("🛑 @|fg(red) " + errorMsg + "|@", colors));
        if (ex != null && isDebug()) {
            ex.printStackTrace(err);
        }
        err.flush();
    }

    public void showUsage(CommandSpec spec) {
        spec.commandLine().usage(out, ansi);
    }

    public void errShowUsage(CommandSpec spec) {
        spec.commandLine().usage(err, ansi);
    }

    public String slugify(String s) {
        return slugifier().slugify(s);
    }

    public void copyImages(List<ImageRef> images) {
        for (ImageRef image : images) {
            Optional<Path> sourceRoot = inputRoot.stream()
                    .filter(x -> x.resolve(image.sourcePath).toFile().exists())
                    .findFirst();

            if (sourceRoot.isEmpty()) {
                errorf("Unable to find image for %s", image.sourcePath);
                continue;
            }

            // Resolve the image path from data against the correct parent path
            Path sourcePath = sourceRoot.get().resolve(image.sourcePath);
            Path targePath = output.resolve(image.targetPath);

            // target path must be pre-resolved to compendium or rules root
            // so just make sure the image dir exists
            targePath.getParent().toFile().mkdirs();
            try {
                Files.copy(sourcePath, targePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                errorf(e, "Unable to copy image from %s to %s", image.sourcePath, image.targetPath);
            }
        }
    }

    public void readFile(Path p, BiConsumer<String, JsonNode> callback) throws IOException {
        inputRoot.add(p.getParent().toAbsolutePath());

        File f = p.toFile();
        JsonNode node = MAPPER.readTree(f);
        callback.accept(f.getName(), node);
        verbosef("🔖 Finished reading %s", p);
    }

    public void readDirectory(Path dir, BiConsumer<String, JsonNode> callback) {
        inputRoot.add(dir.toAbsolutePath());

        String basename = dir.getFileName().toString();
        debugf("📁 %s\n", dir);
        try (Stream<Path> stream = Files.list(dir)) {
            stream.forEach(p -> {
                File f = p.toFile();
                String name = p.getFileName().toString();
                if (f.isDirectory()) {
                    try {
                        readDirectory(p, callback);
                    } catch (Exception e) {
                        errorf(e, "Error reading directory %s", p.toString());
                    }
                } else if ((name.startsWith("fluff") || name.startsWith(basename)) && name.endsWith(".json")) {
                    try {
                        readFile(p, callback);
                    } catch (Exception e) {
                        errorf(e, "Error parsing file %s", p.toString());
                    }
                }
            });
        } catch (Exception e) {
            errorf(e, "Error parsing %s", dir.toString());
        }
    }

    public void read5eTools(Path dir, BiConsumer<String, JsonNode> callback) throws IOException {
        List<String> inputs = List.of(
            "adventures.json", "books.json", "names.json", "variantrules.json",
                "actions.json", "conditionsdiseases.json", "skills.json", "senses.json", "loot.json",
                "bestiary", "bestiary/traits.json", "bestiary/legendarygroups.json",
                "backgrounds.json", "fluff-backgrounds.json",
                "class",
                "deities.json",
                "feats.json", "optionalfeatures.json",
                "items.json", "items-base.json", "fluff-items.json", "magicvariants.json",
                "races.json", "fluff-races.json",
                "spells");

        if (!dir.resolve("adventures.json").toFile().exists()) {
            debugf("Unable to find 5eTools data: %s", dir.toString());
            return;
        }
        inputRoot.add(dir.getParent());

        for (String input : inputs) {
            Path p = dir.resolve(input);
            if (p.toFile().isFile()) {
                readFile(p, callback);
            } else {
                readDirectory(p, callback);
            }
        }
    }
}
