package dev.ebullient.convert.io;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.json.JsonMapper;
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

    public final static PrintWriter streamToWriter(PrintStream stream) {
        return new PrintWriter(stream, true, StandardCharsets.UTF_8);
    }

    public final static ObjectMapper MAPPER = initMapper(JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build());

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
            options.setIndent(4);
            options.setIndicatorIndent(4);
            options.setIndentWithIndicator(true);

            yamlMapper = initMapper(new ObjectMapper(new YAMLFactoryBuilder(new YAMLFactory())
                    .dumperOptions(options).build()))
                    .setSerializationInclusion(Include.NON_DEFAULT);
        }
        return yamlMapper;
    }

    private static ObjectMapper initMapper(ObjectMapper mapper) {
        mapper.setSerializationInclusion(Include.NON_DEFAULT)
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
            options.setSplitLines(true);
            options.setIndent(2);
            options.setIndicatorIndent(2);
            options.setIndentWithIndicator(true);

            Representer representer = new Representer(options);
            representer.addClassTag(dev.ebullient.convert.qute.NamedText.class, Tag.MAP); //

            y = quotedYaml = new Yaml(representer, options);
        }
        return y;
    }

    public static String slugify(String s) {
        return slugifier().slugify(s);
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
    private boolean debugOrLog;
    private boolean verbose;
    private boolean verboseOrLog;
    private Path output = Paths.get("");
    private final Set<Path> inputRoot = new TreeSet<>();

    public Tui() {
        this.ansi = Help.Ansi.OFF;
        this.colors = Help.defaultColorScheme(ansi);

        this.out = streamToWriter(System.out);
        this.err = streamToWriter(System.err);
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

        this.debug = debug || picocliDebugEnabled;
        this.debugOrLog = this.debug || log;
        this.verbose = verbose || debug;
        this.verboseOrLog = this.verbose || log;

        if (log) {
            Path p = Path.of("ttrpg-convert.out.txt");
            try {
                BufferedWriter writer = Files.newBufferedWriter(p, StandardCharsets.UTF_8,
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                this.log = new PrintWriter(writer, true);

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

    private void outLine(String text, Text line) {
        out.println(line);
        if (log != null) {
            log.println(text);
        }
    }

    private void errLine(String text, Text line) {
        err.println(line);
        if (log != null) {
            log.println(text);
        }
    }

    public boolean isDebug() {
        return debugOrLog;
    }

    public boolean isVerbose() {
        return verboseOrLog;
    }

    public void debugf(String output, Object... params) {
        debugf(Msg.NOOP, output, params);
    }

    public void debugf(Msg msg, String output, Object... params) {
        if (debugOrLog) {
            output = format(msg.wrap(output), params);
            if (debug) {
                out.println(ansi.new Text(Msg.DEBUG.color(output), colors));
            }
            if (log != null) {
                log.println(Msg.DEBUG.wrap(output));
            }
        }
    }

    public void verbosef(String output, Object... params) {
        verbosef(Msg.NOOP, output, params);
    }

    public void verbosef(Msg msg, String output, Object... params) {
        if (verboseOrLog) {
            output = format(Msg.VERBOSE.wrap(msg.wrap(output)), params);
            if (verbose) {
                out.println(ansi.new Text(output));
            }
            if (log != null) {
                log.println(output);
            }
        }
    }

    public void log(Throwable t, boolean keepException) {
        if (log != null) {
            log.println(captureStackTrace(t, keepException));
        }
    }

    public void logf(String output, Object... params) {
        logf(Msg.NOOP, output, params);
    }

    public void logf(Msg msg, String output, Object... params) {
        if (log != null) {
            output = format(msg.wrap(output), params);
            log.println(output);
            if (msg == Msg.UNKNOWN || msg == Msg.UNRESOLVED) {
                log(new Exception(output), false);
            }
        }
    }

    public void progressf(String output, Object... params) {
        infof(Msg.PROGRESS, output, params);
    }

    public void infof(Msg msg, String output, Object... params) {
        infof(msg.wrap(output), params);
    }

    public void infof(String output, Object... params) {
        output = format(Msg.INFO.wrap(output), params);
        outLine(output, ansi.new Text(output));
    }

    public void warnf(Msg msg, String output, Object... params) {
        warnf(msg.wrap(output), params);
    }

    public void warnf(String output, Object... params) {
        output = format(Msg.WARN.wrap(output), params);
        outLine(output, ansi.new Text(output));
    }

    public void printlnf(Msg msgType, String output, Object... params) {
        output = format(msgType.wrap(output), params);
        outLine(output, ansi.new Text(output));
    }

    public void errorf(String output, Object... params) {
        errorf(null, Msg.NOOP, output, params);
    }

    public void errorf(Msg msgType, String output, Object... params) {
        errorf(null, msgType, output, params);
    }

    public void errorf(Throwable th, String output, Object... params) {
        errorf(th, Msg.NOOP, output, params);
    }

    public void errorf(Throwable th, Msg msgType, String output, Object... params) {
        output = format(msgType.wrap(output), params);
        error(th, output);
    }

    private void error(Throwable ex, String errorMsg) {
        String message = Msg.ERR.wrap(errorMsg
                .replace("java.nio.file.NoSuchFileException: ", "File not found: "));
        errLine(message, colors.errorText(message));
        if (ex != null && log != null) {
            log.println(captureStackTrace(ex, true));
        }
    }

    private String format(String output, Object... params) {
        if (params != null && params.length > 0) {
            return String.format(output, params);
        }
        return output;
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

            verbosef(Msg.WRITING, "Generating CSS snippet for %s", fontRef.sourcePath);
            if (fontRef.sourcePath.startsWith("http")) {
                try (InputStream is = URI.create(fontRef.sourcePath.replace(" ", "%20")).toURL().openStream()) {
                    Files.writeString(targetPath, templates.renderCss(fontRef, is));
                } catch (IOException e) {
                    errorf("Unable to copy font. %s", e);
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
                    errorf("Unable to copy font. %s", e);
                }
            }
        }
    }

    public void copyImages(Collection<ImageRef> images) {
        verbosef(Msg.PROGRESS, "Processing images");

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
                errorf("Unable to copy image. %s", e);
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
            errorf("Unable to copy resource. %s", e);
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
            errorf("Unable to copy remote image (%s). ", url, e);
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
        } catch (IOException e) {
            errorf(e, "Unable to read source file at path %s (%s)", p, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean readDirectory(String relative, Path dir, BiConsumer<String, JsonNode> callback) {
        debugf(Msg.FOLDER.wrap(dir.toString()));

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
        Collection<String> sourceFiles = TtrpgConfig.getFileSources();

        if (!sourceFiles.stream().allMatch(f -> toolsBase.resolve(f).toFile().exists())) {
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
            return resource.endsWith(".yaml")
                    ? Tui.yamlMapper().readTree(TtrpgConfig.class.getResourceAsStream(resource))
                    : Tui.MAPPER.readTree(TtrpgConfig.class.getResourceAsStream(resource));
        } catch (IOException | IllegalArgumentException e) {
            Tui.instance.errorf(e, "Unable to read or parse required resource (%s): %s", resource, e.toString());
            return null;
        }
    }

    public static String jsonStringify(Object o) {
        return Tui.MAPPER.valueToTree(o).toPrettyString();
    }

    public static String captureStackTrace(Throwable t, boolean keepException) {
        var stackTrace = t.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return keepException ? t.toString() : "";
        }
        StringBuilder sb = new StringBuilder();
        if (keepException) {
            sb.append(Msg.DEBUG.wrap(t.toString())).append("\n");
        } else {
            sb.append(Msg.DEBUG.wrap(t.getMessage())).append("\n");
        }
        for (StackTraceElement e : stackTrace) {
            if (e.getClassName().startsWith("picocli")) {
                break;
            }
            if (e.getClassName().matches(".*(Tui|RpgDataConvertCli|writeAll).*")) {
                continue;
            }
            sb.append("\tat ").append(e.toString()).append("\n");
        }
        return sb.toString();
    }
}
