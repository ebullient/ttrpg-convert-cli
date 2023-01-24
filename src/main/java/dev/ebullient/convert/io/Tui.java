package dev.ebullient.convert.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder;
import com.github.slugify.Slugify;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@ApplicationScoped
public class Tui {
    public final static TypeReference<List<String>> LIST_STRING = new TypeReference<>() {
    };
    public final static TypeReference<List<Integer>> LIST_INT = new TypeReference<>() {
    };
    public final static TypeReference<Map<String, String>> MAP_STRING_STRING = new TypeReference<>() {
    };

    public final static ObjectMapper MAPPER = initMapper(new ObjectMapper());

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

    public static ObjectMapper mapper(Path p) {
        return p.getFileName().toString().endsWith(".json") ? MAPPER : yamlMapper();
    }

    private static ObjectMapper yamlMapper;

    private static ObjectMapper yamlMapper() {
        if (yamlMapper == null) {
            DumperOptions options = new DumperOptions();
            options.setDefaultScalarStyle(ScalarStyle.PLAIN);
            options.setDefaultFlowStyle(FlowStyle.AUTO);
            options.setPrettyFlow(true);
            yamlMapper = initMapper(new ObjectMapper(new YAMLFactoryBuilder(new YAMLFactory())
                    .dumperOptions(options).build()));
        }
        return yamlMapper;
    }

    private static ObjectMapper initMapper(ObjectMapper mapper) {
        new ObjectMapper()
                .setVisibility(VisibilityChecker.Std.defaultInstance()
                        .with(JsonAutoDetect.Visibility.ANY));
        return mapper;
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

    Ansi ansi;
    ColorScheme colors;

    PrintWriter out;
    PrintWriter err;

    private Templates templates;
    private CommandLine commandLine;
    private boolean debug;
    private boolean verbose;
    private Path output = Paths.get("");
    private final Set<Path> inputRoot = new TreeSet<>();

    public Tui() {
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
            this.commandLine = spec.commandLine();
        }

        this.debug = debug;
        this.verbose = verbose;
    }

    public void setOutputPath(Path output) {
        this.output = output;
    }

    public void setTemplates(Templates templates) {
        this.templates = templates;
    }

    public void close() {
        out.flush();
        err.flush();
    }

    public CommandLine getCommandLine() {
        return commandLine;
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
        err.println(colors.errorText("🛑 " + errorMsg));
        if (ex != null && isDebug()) {
            ex.printStackTrace(err);
        }
        err.flush();
    }

    public void throwInvalidArgumentException(String message) {
        if (commandLine != null) {
            throw new ParameterException(commandLine, message);
        } else {
            throw new IllegalArgumentException(message);
        }
    }

    public void showUsage(CommandSpec spec) {
        spec.commandLine().usage(out, ansi);
    }

    public void errShowUsage(CommandSpec spec) {
        spec.commandLine().usage(err, ansi);
    }

    public static String slugify(String s) {
        return slugifier().slugify(s);
    }

    public void copyImages(List<ImageRef> images, Map<String, String> fallbackPaths) {
        for (ImageRef image : images) {
            if (image.sourcePath.toString().startsWith("stream/")) {
                copyImageResource(image);
                continue;
            }

            Optional<Path> sourceRoot = inputRoot.stream()
                    .filter(x -> x.resolve(image.sourcePath).toFile().exists())
                    .findFirst();

            Path basePath = image.sourcePath;

            if (sourceRoot.isEmpty()) {
                String path = image.sourcePath.toString();

                // Find relocated image
                String adjustedPath = fallbackPaths.get(path);
                if (adjustedPath != null) {
                    sourceRoot = inputRoot.stream()
                            .filter(x -> x.resolve(adjustedPath).toFile().exists())
                            .findFirst();
                    basePath = Path.of(adjustedPath);
                }

                // If the file is still not found, bail..
                if (sourceRoot.isEmpty()) {
                    errorf("Unable to find image for %s", image.sourcePath);
                    continue;
                }
            }

            // Resolve the image path from data against the correct parent path
            Path sourcePath = sourceRoot.get().resolve(basePath);
            Path targetPath = output.resolve(image.targetPath);

            // target path must be pre-resolved to compendium or rules root
            // so just make sure the image dir exists
            targetPath.getParent().toFile().mkdirs();
            try {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                errorf(e, "Unable to copy image from %s to %s", image.sourcePath, image.targetPath);
            }
        }
    }

    public void copyImageResource(ImageRef image) {
        String sourcePath = image.sourcePath.toString().replace("stream", "");
        Path targetPath = output.resolve(image.targetPath);
        targetPath.getParent().toFile().mkdirs();

        try {
            InputStream in = TtrpgConfig.class.getResourceAsStream(sourcePath);
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            errorf(e, "Unable to copy resource from %s to %s", sourcePath, image.targetPath);
        }
    }

    public boolean readResource(String name, BiConsumer<String, JsonNode> callback) {
        try {
            JsonNode node = MAPPER.readTree(this.getClass().getResourceAsStream(name));
            callback.accept(name, node);
            verbosef("🔖 Finished reading %s", name);
        } catch (IOException e) {
            errorf(e, "Unable to read resource %s", name);
            return false;
        }
        return true;
    }

    public boolean readFile(Path p, BiConsumer<String, JsonNode> callback) {
        inputRoot.add(p.getParent().toAbsolutePath());
        try {
            File f = p.toFile();
            JsonNode node = MAPPER.readTree(f);
            callback.accept(f.getName(), node);
            verbosef("🔖 Finished reading %s", p);
        } catch (IOException e) {
            errorf(e, "Unable to read source file at path %s", p);
            return false;
        }
        return true;
    }

    public boolean readDirectory(Path dir, BiConsumer<String, JsonNode> callback) {
        debugf("📁 %s\n", dir);

        inputRoot.add(dir.toAbsolutePath());

        boolean result = true;
        String basename = dir.getFileName().toString();
        try (Stream<Path> stream = Files.list(dir)) {
            Iterator<Path> i = stream.iterator();
            while (i.hasNext()) {
                Path p = i.next();
                File f = p.toFile();
                String name = p.getFileName().toString();
                if (f.isDirectory()) {
                    result |= readDirectory(p, callback);
                } else if ((name.startsWith("fluff") || name.startsWith(basename)) && name.endsWith(".json")) {
                    result |= readFile(p, callback);
                }
            }
        } catch (Exception e) {
            errorf(e, "Error reading %s", dir.toString());
            return false;
        }
        return result;
    }

    public boolean read5eTools(Path toolsBase, BiConsumer<String, JsonNode> callback) {
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

        if (!toolsBase.resolve("adventures.json").toFile().exists()) {
            debugf("Unable to find 5eTools data: %s", toolsBase.toString());
            return false;
        }
        inputRoot.add(toolsBase.getParent());

        return readToolsList(toolsBase, inputs, callback);
    }

    public boolean readPf2eTools(Path toolsBase, BiConsumer<String, JsonNode> callback) {
        List<String> inputs = List.of(
                "books.json", "book/book-crb.json",
                "actions.json", "afflictions.json", "archetypes.json",
                "conditions.json", "feats", "rituals.json",
                "skills.json", "spells", "tables.json", "traits.json");

        if (toolsBase.resolve("archetypes.json").toFile().exists()
                && toolsBase.resolve("book/book-crb.json").toFile().exists()) {
            inputRoot.add(toolsBase.getParent());
            return readToolsList(toolsBase, inputs, callback);
        }
        debugf("Unable to find pf2e data: %s", toolsBase.toString());
        return false;
    }

    private boolean readToolsList(Path toolsBase, List<String> inputs, BiConsumer<String, JsonNode> callback) {
        boolean result = true;
        for (String input : inputs) {
            Path p = toolsBase.resolve(input);
            if (p.toFile().isFile()) {
                result |= readFile(p, callback);
            } else {
                result |= readDirectory(p, callback);
            }
        }
        return result;
    }

    public void writeJsonFile(Path outputFile, Map<String, Object> keys) throws IOException {
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
        pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        MAPPER.writer()
                .with(pp)
                .writeValue(outputFile.toFile(), keys);
    }

    public void writeYamlFile(Path outputFile, Map<String, Object> map) throws IOException {
        yamlMapper().writer().writeValue(outputFile.toFile(), map);
    }

    public String applyTemplate(QuteBase resource) {
        return templates.render(resource);
    }

    public String applyTemplate(QuteNote note) {
        return templates.renderNote(note);
    }

    static class ToLowerDeserializer extends KeyDeserializer {

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            return key.toLowerCase();
        }

    }
}
