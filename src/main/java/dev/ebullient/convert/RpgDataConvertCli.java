package dev.ebullient.convert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TemplatePaths;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Templates;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.ToolsIndex;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;
import picocli.CommandLine.UnmatchedArgumentException;

@SuppressWarnings("CanBeFinal")
@QuarkusMain
@Command(name = "ttrpg-convert", header = "Convert TTRPG JSON data to markdown", subcommands = {
        Completion.class,
}, description = {
        "%n%nThis will read from a collection of individual JSON files or a directory containing JSON files and will produce Obsidian markdown documents.",
}, footer = {
        "",
        "Configuration.",
        "",
        "Sources, templates, and other settings should be specified in a config file. This file can be in either JSON or YAML. If no config file is specified (using -c or --config), this tool will look for config.json, and then config.yaml in the current directory.",
        "",
        "Use the 'from' option in the config file to filter materials by source. Only include materials from sources you own. There may be a default set of materials produced when no source is specified (e.g. those in the SRD)",
        "",
        "Identifiers for include/exclude rules and patters are listed in the generated index file.",
        "",
        "Here is a brief example (JSON). See the project README.md for details.",
        "",
        "{",
        "  \"from\" : [",
        "    \"PHB\",",
        "    \"DMG\",",
        "    \"SCAG\",",
        "  ]",
        "  \"exclude\" : [",
        "    \"background|sage|phb\",",
        "  ]",
        "  \"excludePattern\" : [",
        "    \"race|.*|dmg\",",
        "  ]",
        "}",
        "",
}, mixinStandardHelpOptions = true, versionProvider = VersionProvider.class, showDefaultValues = true)
public class RpgDataConvertCli implements Callable<Integer>, QuarkusApplication {
    static final Path DEFAULT_PATH = Path.of("config.json");

    List<Path> input;
    Path output;

    @Inject
    IFactory factory;

    @Inject
    Tui tui;

    @Inject
    Templates tpl;

    @Spec
    private CommandSpec spec;

    @Option(names = { "-d", "--debug" }, description = "Enable debug output", defaultValue = "false", scope = ScopeType.INHERIT)
    boolean debug;

    @Option(names = { "-v", "--verbose" }, description = "Verbose output", defaultValue = "false", scope = ScopeType.INHERIT)
    boolean verbose;

    Datasource game;

    @Option(names = { "-g",
            "--game" }, description = "Game data source.%n  Candidates: ${COMPLETION-CANDIDATES}", defaultValue = "5e", completionCandidates = Datasource.DatasourceCandidates.class)
    void setDatasource(String datasource) {
        try {
            game = Datasource.matchDatasource(datasource);
        } catch (IllegalStateException e) {
            tui.errorf("Unknown game data: %s", datasource);
        }
    }

    @Option(names = { "-c", "--config" }, description = "Config file")
    Path configPath;

    @Option(names = "-s", hidden = true, description = "Source Books%n  " +
            "Comma-separated list or multiple declarations (PHB,DMG,...); use ALL for all sources")
    List<String> source = Collections.emptyList();

    @Option(names = "--index", description = "Create index of keys that can be used to exclude entries")
    boolean writeIndex;

    @ArgGroup(exclusive = false)
    TemplatePaths templatePaths = new TemplatePaths();

    @Option(names = "-o", description = "Output directory", required = true)
    void setOutputPath(File outputDir) {
        output = outputDir.toPath().toAbsolutePath().normalize();
        if (output.toFile().exists() && output.toFile().isFile()) {
            throw new ParameterException(spec.commandLine(),
                    "Specified output path exists and is a file: " + output.toString());
        }
    }

    @Parameters(description = "Source file(s)")
    void setInput(List<File> inputFile) {
        input = new ArrayList<>(inputFile.size());
        for (File f : inputFile) {
            input.add(f.toPath().toAbsolutePath().normalize());
        }
    }

    @Override
    public Integer call() {
        if (input == null || input.isEmpty()) {
            throw new CommandLine.MissingParameterException(spec.commandLine(), spec.args(),
                    "Must specify an input file");
        }
        if (!output.toFile().exists() && !output.toFile().mkdirs()) {
            tui.errorf("Unable to create output directory: %s", output);
            return ExitCode.USAGE;
        }

        boolean allOk = true;
        tui.setTemplates(tpl);
        tui.setOutputPath(output);

        TtrpgConfig.init(tui, game);
        Configurator configurator = new Configurator(tui);

        if (source.size() == 1 && source.get(0).contains(",")) {
            String tmp = source.remove(0);
            source = List.of(tmp.split(","));
        }
        if (source.contains("ALL")) {
            source = List.of("*");
        }
        configurator.addSources(source);
        configurator.setTemplatePaths(templatePaths);

        if (configPath != null) {
            if (configPath.toFile().exists()) {
                // Read configuration
                allOk = configurator.readConfiguration(configPath);
            } else {
                tui.errorf("Specified config file does not exist: %s", configPath);
                allOk = false;
            }
        }

        if (!allOk) {
            return ExitCode.USAGE;
        }

        CompendiumConfig config = TtrpgConfig.getConfig();

        tui.done("Finished reading config.");
        tui.verbosef("Writing markdown to %s.\n", output);

        ToolsIndex index = ToolsIndex.createIndex();
        Path toolsPath = null;

        // Read provided input files
        // Note: could test for selected game system and read paths differently
        // ATM, both 5e and pf2e use the same general structure.
        // Marker files are in configData
        for (Path inputPath : input) {
            tui.printlnf("⏱️ Reading %s", inputPath);
            Path input = inputPath.toAbsolutePath();
            if (input.toFile().isDirectory()) {
                boolean isTools = tui.readToolsDir(input, index::importTree);
                if (isTools) { // we found the tools directory
                    toolsPath = input;
                } else {
                    // this is some other directory full of json
                    allOk &= tui.readDirectory("", input, index::importTree);
                }
            } else {
                allOk &= tui.readFile(input, TtrpgConfig.getFixes(inputPath.toString()), index::importTree);
            }
        }

        // Include extra books and adventures from config (relative to toolsPath)
        if (allOk && toolsPath != null) {
            for (String adventure : config.getAdventures()) {
                allOk &= tui.readFile(toolsPath.resolve(adventure), TtrpgConfig.getFixes(adventure), index::importTree);
            }
            for (String book : config.getBooks()) {
                allOk &= tui.readFile(toolsPath.resolve(book), TtrpgConfig.getFixes(book), index::importTree);
            }
        }

        // Include additional standalone files from config (relative to current directory)
        for (String brew : config.getHomebrew()) {
            allOk &= tui.readFile(Path.of(brew), TtrpgConfig.getFixes(brew), index::importTree);
        }

        if (!allOk) {
            tui.println("❌ errors reading data. Check the following: ",
                    "- Are you specifying the right game? (-g 5e OR -g pf2e),",
                    "    Using " + TtrpgConfig.getConfig().datasource().shortName(),
                    "- Check error messages to see what files couldn't be read",
                    "   For bulk conversion, specify the the <tools>/data directory");
            return ExitCode.USAGE;
        }
        tui.done("Finished reading data.");
        try {
            index.prepare();

            if (writeIndex) {
                try {
                    index.writeFullIndex(output.resolve("all-index.json"));
                    index.writeFilteredIndex(output.resolve("src-index.json"));
                } catch (IOException e) {
                    tui.error(e, "  Exception: " + e.getMessage());
                    allOk = false;
                }
            }

            tui.println("💡 Writing files to " + output);
            tpl.setCustomTemplates(config);

            MarkdownWriter writer = new MarkdownWriter(output, tpl, tui);
            index.markdownConverter(writer)
                    .writeAll()
                    .writeNotesAndTables()
                    .writeImages();
        } catch (Throwable e) {
            String message = e.getMessage();
            if (message == null) {
                message = e.getClass().getSimpleName();
            }
            tui.errorf(e, "An error occurred: %s.%n%nRun with --debug for details.", message);
            allOk = false;
        }

        return allOk ? ExitCode.OK : ExitCode.SOFTWARE;
    }

    private int executionStrategy(ParseResult parseResult) {
        try {
            tui.init(spec, debug, verbose);
            return new CommandLine.RunLast().execute(parseResult);
        } finally {
            tui.close();
        }
    }

    @Override
    public int run(String... args) {
        return new CommandLine(this, factory)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setExecutionStrategy(this::executionStrategy)
                .setParameterExceptionHandler(new ShortErrorMessageHandler())
                .execute(args);
    }

    class ShortErrorMessageHandler implements IParameterExceptionHandler {
        public int handleParseException(ParameterException ex, String[] args) {
            CommandLine cmd = ex.getCommandLine();
            CommandSpec spec = cmd.getCommandSpec();
            tui.init(spec, debug, verbose);

            tui.error(ex, ex.getMessage());
            UnmatchedArgumentException.printSuggestions(ex, cmd.getErr());

            cmd.getErr().println(cmd.getHelp().fullSynopsis()); // normal text to error stream

            if (spec.equals(spec.root())) {
                cmd.getErr().println(cmd.getHelp().commandList()); // normal text to error stream
            }
            cmd.getErr().printf("See '%s --help' for more information.%n", spec.qualifiedName());
            cmd.getErr().flush();

            return cmd.getExitCodeExceptionMapper() != null
                    ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                    : spec.exitCodeOnInvalidInput();
        }
    }
}
