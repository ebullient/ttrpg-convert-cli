package dev.ebullient.convert.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
import jakarta.enterprise.event.Observes;

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

import dev.ebullient.convert.VersionProvider;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.config.TtrpgConfig.Fix;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.runtime.ShutdownEvent;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Help.Ansi.Text;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@ApplicationScoped
public class Tui {
    static Tui instance;

    public static Tui instance() {
        return instance;
    }

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

    public static String slugify(String s) {
        return slugifier().slugify(s);
    }

    public static String toAnchorTag(String x) {
        return x.replace(" ", "%20")
                .replace(":", "")
                .replace(".", "")
                .replace('‑', '-');
    }

    static final boolean picocliDebugEnabled = "DEBUG".equalsIgnoreCase(System.getProperty("picocli.trace"));

    Ansi ansi;
    ColorScheme colors;

    PrintWriter log;
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

        Tui.instance = this;
    }

    public void init(CommandSpec spec, boolean debug, boolean verbose) {
        init(spec, debug, verbose, false);
    }

    public void init(CommandSpec spec, boolean debug, boolean verbose, boolean log) {
        if (spec != null) {
            this.ansi = spec.commandLine().getHelp().ansi();
            this.colors = spec.commandLine().getHelp().colorScheme();
            this.out = spec.commandLine().getOut();
            this.err = spec.commandLine().getErr();
            this.commandLine = spec.commandLine();
        }

        this.debug = debug || log;
        this.verbose = verbose;
        if (log) {
            Path p = Path.of("ttrpg-convert.out.txt");
            try {
                this.log = new PrintWriter(Files.newOutputStream(p));
                VersionProvider vp = new VersionProvider();
                List.of(vp.getVersion()).forEach(this.log::println);
            } catch (IOException e) {
                errorf(e, "Unable to open log file %s: %s", p.toAbsolutePath(), e.toString());
            }
        }
    }

    void onShutdown(@Observes ShutdownEvent event) {
        this.close();
    }

    public void setOutputPath(Path output) {
        this.output = output;
    }

    public void setTemplates(Templates templates) {
        this.templates = templates;
    }

    public void close() {
        flush();
        if (this.log != null) {
            log.close();
        }
    }

    public void flush() {
        out.flush();
        err.flush();
        if (log != null) {
            log.flush();
        }
    }

    private void outLine(Text line) {
        out.println(line);
        if (log != null) {
            log.println(line.plainString());
        }
    }

    private void errLine(Text line) {
        err.println(line);
        if (log != null) {
            log.println(line.plainString());
        }
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
            outLine(ansi.new Text("@|faint 🔧 " + output + "|@", colors));
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
            outLine(ansi.new Text("@|faint 🔹 " + output + "|@", colors));
        }
    }

    public void warnf(String format, Object... params) {
        warn(String.format(format, params));
    }

    public void warn(String output) {
        outLine(ansi.new Text("[🔸 WARN] " + output));
    }

    public void donef(String format, Object... params) {
        done(String.format(format, params));
    }

    public void done(String output) {
        outLine(ansi.new Text("[ ✅  OK] " + output));
    }

    public void printlnf(String format, Object... args) {
        println(String.format(format, args));
    }

    public void println(String output) {
        outLine(ansi.new Text(output, colors));
        flush();
    }

    public void println(String... output) {
        Arrays.stream(output).forEach(l -> outLine(ansi.new Text(l, colors)));
        flush();
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
        errLine(colors.errorText("[ 🛑 ERR] " + errorMsg));
        if (ex != null && isDebug()) {
            ex.printStackTrace(err);
            if (log != null) {
                ex.printStackTrace(log);
            }
        }
        flush();
    }

    public void throwInvalidArgumentException(String message) {
        if (commandLine != null) {
            throw new ParameterException(commandLine, message);
        } else {
            throw new IllegalArgumentException(message);
        }
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

    public void copyFonts(Collection<FontRef> fonts) {
        for (FontRef fontRef : fonts) {
            Path targetPath = output.resolve(Path.of("css-snippets", slugify(fontRef.fontFamily) + ".css"));
            targetPath.getParent().toFile().mkdirs();

            printlnf("⏱️ Generating CSS snippet for %s", fontRef.sourcePath);
            if (fontRef.sourcePath.startsWith("http")) {
                try (InputStream is = URI.create(fontRef.sourcePath.replace(" ", "%20")).toURL().openStream()) {
                    Files.writeString(targetPath, templates.renderCss(fontRef, is));
                } catch (IOException e) {
                    errorf(e, "Unable to copy font from %s to %s", fontRef.sourcePath, targetPath);
                }
            } else {
                Optional<Path> resolvedSource = resolvePath(Path.of(fontRef.sourcePath));
                if (resolvedSource.isEmpty()) {
                    errorf("Unable to find font '%s'", fontRef.sourcePath);
                    continue;
                }
                try (BufferedInputStream is = new BufferedInputStream(Files.newInputStream(resolvedSource.get()))) {
                    Files.writeString(targetPath, templates.renderCss(fontRef, is));
                } catch (IOException e) {
                    errorf(e, "Unable to copy font from %s to %s", fontRef.sourcePath, targetPath);
                }
            }
        }
    }

    public void copyImages(Collection<ImageRef> images) {
        for (ImageRef image : images) {
            Path targetPath = image.targetFilePath() == null
                    ? null
                    : output.resolve(image.targetFilePath());
            if (targetPath == null || targetPath.toFile().exists()) {
                // remote resources we are not copying, or a target path that already exists
                continue;
            }
            if (image.sourcePath() == null) {
                copyRemoteImage(image, targetPath);
                continue;
            }
            if (image.sourcePath().toString().startsWith("stream/")) {
                copyImageResource(image, targetPath);
                continue;
            }

            // target path must be pre-resolved to compendium or rules root
            // so just make sure the image dir exists
            targetPath.getParent().toFile().mkdirs();
            try {
                Files.copy(image.sourcePath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                errorf(e, "Unable to copy image from %s to %s (%s)", image.sourcePath(), image.targetFilePath(), e);
            }
        }
    }

    private void copyImageResource(ImageRef image, Path targetPath) {
        String sourcePath = image.sourcePath().toString().replace("stream", "");
        targetPath.getParent().toFile().mkdirs();

        try {
            InputStream in = TtrpgConfig.class.getResourceAsStream(sourcePath);
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            errorf(e, "Unable to copy resource from %s to %s (%s)", sourcePath, image.targetFilePath(), e);
        }
    }

    private void copyRemoteImage(ImageRef image, Path targetPath) {
        targetPath.getParent().toFile().mkdirs();

        String url = image.url();
        if (url == null) {
            errorf("ImageRef %s has no URL", image.targetFilePath());
            return;
        }
        if (!url.startsWith("http") && !url.startsWith("file")) {
            errorf("Remote ImageRef %s has invalid URL %s", image.targetFilePath(), url);
            return;
        }

        try {
            Tui.instance().debugf("copy image %s", url);

            ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());
            try (FileOutputStream fileOutputStream = new FileOutputStream(targetPath.toFile())) {
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
        } catch (IOException e) {
            errorf(e, "Unable to copy remote image from %s to %s (%s)", url, image.targetFilePath(), e);
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
        Collection<String> inputs = TtrpgConfig.getFileSources();
        Collection<String> markers = TtrpgConfig.getMarkerFiles();

        if (!markers.stream().allMatch(f -> toolsBase.resolve(f).toFile().exists())) {
            // Common mistake is to point to the tools directory instead of the data directory
            Path data = toolsBase.resolve("data");
            if (data.toFile().isDirectory()) {
                return readToolsDir(data, callback);
            } else {
                debugf("Unable to find tools data in %s", toolsBase.toString());
                return false;
            }
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

    public String renderEmbedded(QuteUtil resource) {
        return templates.renderInlineEmbedded(resource);
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
            Tui.instance.errorf(e, "Unable to read or parse required resource (%s): %s", resource, e.toString());
            return null;
        }
    }
}
