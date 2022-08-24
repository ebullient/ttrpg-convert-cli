package dev.ebullient.json5e.io;

import java.io.PrintWriter;

import javax.enterprise.context.ApplicationScoped;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.github.slugify.Slugify;

import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;

@ApplicationScoped
public class Json5eTui {
    public final static ObjectMapper MAPPER = new ObjectMapper()
            .setVisibility(VisibilityChecker.Std.defaultInstance().with(JsonAutoDetect.Visibility.ANY));

    private static Slugify slugify;

    private static Slugify slugifier() {
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

    private static Yaml yaml;

    private static Yaml getYaml() {
        Yaml y = yaml;
        if (y == null) {
            DumperOptions options = new DumperOptions();
            options.setDefaultScalarStyle(ScalarStyle.DOUBLE_QUOTED);
            options.setPrettyFlow(true);
            y = yaml = new Yaml(options);
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
            out.println(ansi.new Text("@|faint " + output + "|@", colors));
        }
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void verbosef(String format, Object... params) {
        if (isVerbose()) {
            outPrintf(format, params);
        }
    }

    public void verbose(String output) {
        if (isVerbose()) {
            outPrintln(output.trim());
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
}
