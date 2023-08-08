package dev.ebullient.convert.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder;
import com.github.slugify.Slugify;

import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.config.TtrpgConfig.Fix;
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
    public final static TypeReference<Map<String, List<String>>> MAP_STRING_LIST_STRING = new TypeReference<>() {
    };

    public final static ObjectMapper MAPPER = initMapper(new ObjectMapper());

    private static Slugify slugify;

    static Slugify slugifier() {
        Slugify s = slugify;
        if (s == null) {
            slugify = s = Slugify.builder()
                    .customReplacement("\"", "")
                    .customReplacement("'", "")
                    .customReplacement(",", "")
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
                    .dumperOptions(options).build()))
                    .setSerializationInclusion(Include.NON_DEFAULT);
        }
        return yamlMapper;
    }

    private static ObjectMapper initMapper(ObjectMapper mapper) {
        mapper.setVisibility(VisibilityChecker.Std.defaultInstance()
                .with(JsonAutoDetect.Visibility.ANY));
        return mapper;
    }

    private static Yaml plainYaml;

    public static Yaml plainYaml() {
        Yaml y = plainYaml;
        if (y == null) {
            DumperOptions options = new DumperOptions();
            options.setDefaultScalarStyle(ScalarStyle.PLAIN);
            options.setDefaultFlowStyle(FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Representer representer = new Representer(options);
            representer.addClassTag(dev.ebullient.convert.qute.NamedText.class, Tag.MAP); //

            y = plainYaml = new Yaml(representer, options);
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

            Representer representer = new Representer(options);
            representer.addClassTag(dev.ebullient.convert.qute.NamedText.class, Tag.MAP); //

            y = quotedYaml = new Yaml(representer, options);
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

    public void printlnf(String format, Object... args) {
        println(String.format(format, args));
    }

    public void println(String output) {
        out.println(ansi.new Text(output, colors));
        out.flush();
    }

    public void println(String... output) {
        Arrays.stream(output).forEach(l -> out.println(ansi.new Text(l, colors)));
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

    public static String slugify(String s) {
        return slugifier().slugify(s);
    }

    public Optional<Path> resolvePath(Path path) {
        if (path == null) {
            return Optional.empty();
        }
        // find the right source root (there could be several)
        return inputRoot.stream()
                .filter(x -> x.resolve(path).toFile().exists())
                .findFirst();
    }

    public void copyImages(Collection<ImageRef> images, Map<String, String> fallbackPaths) {
        for (ImageRef image : images) {
            Path targetPath = output.resolve(image.targetFilePath());
            if (targetPath.toFile().exists()) {
                continue;
            }
            if (image.sourcePath().toString().startsWith("stream/")) {
                copyImageResource(image);
                continue;
            }

            // find the right source root (there could be several)
            Optional<Path> sourceRoot = resolvePath(image.sourcePath());

            // adjust basePath for relocated image
            Path relativeImagePath = image.sourcePath();
            if (sourceRoot.isEmpty()) {
                String adjustedPath = fallbackPaths.get(image.sourcePath().toString());
                if (adjustedPath != null) {
                    sourceRoot = inputRoot.stream()
                            .filter(x -> x.resolve(adjustedPath).toFile().exists())
                            .findFirst();
                    relativeImagePath = Path.of(adjustedPath);
                }

                // If the file is still not found, bail..
                if (sourceRoot.isEmpty()) {
                    errorf("Unable to find image for %s", image.sourcePath());
                    continue;
                }
            }

            // Resolve the image path from data against the correct parent path
            Path sourcePath = sourceRoot.get().resolve(relativeImagePath);

            // target path must be pre-resolved to compendium or rules root
            // so just make sure the image dir exists
            targetPath.getParent().toFile().mkdirs();
            try {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                errorf(e, "Unable to copy image from %s to %s", image.sourcePath(), image.targetFilePath());
            }
        }
    }

    private void copyImageResource(ImageRef image) {
        String sourcePath = image.sourcePath().toString().replace("stream", "");
        Path targetPath = output.resolve(image.targetFilePath());
        targetPath.getParent().toFile().mkdirs();

        try {
            InputStream in = TtrpgConfig.class.getResourceAsStream(sourcePath);
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            errorf(e, "Unable to copy resource from %s to %s", sourcePath, image.targetFilePath());
        }
    }

    public boolean readFile(Path p, List<Fix> fixes, BiConsumer<String, JsonNode> callback) {
        inputRoot.add(p.getParent().toAbsolutePath());
        try {
            File f = p.toFile();
            String contents = Files.readString(p);
            for (Fix fix : fixes) {
                contents = contents.replaceAll(fix.match, fix.replace);
            }
            JsonNode node = MAPPER.readTree(contents);
            callback.accept(f.getName(), node);
            verbosef("🔖 Finished reading %s", p);
        } catch (IOException e) {
            errorf(e, "Unable to read source file at path %s (%s)", p, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean readDirectory(String relative, Path dir, BiConsumer<String, JsonNode> callback) {
        debugf("📁 %s", dir);

        inputRoot.add(dir.toAbsolutePath());

        boolean result = true;
        String basename = dir.getFileName().toString();
        if ("ancestries".equals(basename)) {
            basename = "ancestry";
        } else if (TtrpgConfig.getConfig().datasource() == Datasource.toolsPf2e && "bestiary".equals(basename)) {
            basename = "creature";
        }
        try (Stream<Path> stream = Files.list(dir)) {
            Iterator<Path> i = stream.iterator();
            while (i.hasNext()) {
                Path p = i.next();
                File f = p.toFile();
                String name = p.getFileName().toString();
                if (f.isDirectory()) {
                    result &= readDirectory(relative + p.getFileName() + '/', p, callback);
                } else if ((name.startsWith("fluff") || name.startsWith(basename)) && name.endsWith(".json")) {
                    result &= readFile(p, TtrpgConfig.getFixes(relative + name), callback);
                }
            }
        } catch (Exception e) {
            errorf(e, "Error reading %s (%s)", dir.toString(), e.getMessage());
            return false;
        }
        return result;
    }

    public boolean readToolsDir(Path toolsBase, BiConsumer<String, JsonNode> callback) {
        List<String> inputs = TtrpgConfig.getFileSources();
        List<String> markers = TtrpgConfig.getMarkerFiles();

        if (!markers.stream().allMatch(f -> toolsBase.resolve(f).toFile().exists())) {
            debugf("Unable to find tools data in %s", toolsBase.toString());
            return false;
        }

        inputRoot.add(toolsBase.getParent());

        boolean result = true;
        for (String input : inputs) {
            Path p = toolsBase.resolve(input);
            if (p.toFile().isFile()) {
                result &= readFile(p, TtrpgConfig.getFixes(input), callback);
            } else {
                result &= readDirectory(input + "/", p, callback);
            }
        }
        return result;
    }

    public void writeJsonFile(Path outputFile, Map<String, Object> values) throws IOException {
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
        pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        MAPPER.writer()
                .with(pp)
                .writeValue(outputFile.toFile(), values);
    }

    public void writeYamlFile(Path outputFile, Map<String, Object> values) throws IOException {
        yamlMapper().writer().writeValue(outputFile.toFile(), values);
    }

    public void writeJsonFile(Path outputFile, Object obj) throws IOException {
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
        pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        MAPPER.writer()
                .with(pp)
                .writeValue(outputFile.toFile(), obj);
    }

    public void writeYamlFile(Path outputFile, Object obj) throws IOException {
        yamlMapper().writer().writeValue(outputFile.toFile(), obj);
    }

    public String renderEmbedded(QuteBase resource) {
        return templates.render(resource);
    }

    public String renderEmbedded(QuteNote note) {
        return templates.render(note);
    }

    public <T> T readJsonValue(JsonNode node, TypeReference<T> targetRef) {
        if (node != null) {
            try {
                return Tui.MAPPER.convertValue(node, targetRef);
            } catch (RuntimeException e) {
                errorf(e, "Unable to convert %s", node.toString());
            }
        }
        return null;
    }

    public <T> T readJsonValue(JsonNode node, Class<T> classTarget) {
        if (node != null) {
            try {
                return Tui.MAPPER.convertValue(node, classTarget);
            } catch (RuntimeException e) {
                errorf(e, "Unable to convert %s", node.toString());
            }
        }
        return null;
    }

    public static JsonNode readTreeFromResource(String resource) {
        try {
            return Tui.MAPPER.readTree(TtrpgConfig.class.getResourceAsStream(resource));
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to read or parse required resource (" + resource + "): " + e, e);
        }
    }
}
