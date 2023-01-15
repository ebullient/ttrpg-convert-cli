package dev.ebullient.convert.config;

import java.nio.file.Path;

import dev.ebullient.convert.io.Tui;

public class ConfiguratorUtil {

    public static CompendiumConfig testCustomTemplate(TtrpgConfig ttrpgConfig, String key, Path p) {
        CompendiumConfig base = ttrpgConfig.getConfig();
        TemplatePaths templatePaths = new TemplatePaths();
        templatePaths.setCustomTemplate(key, p);

        return ConfiguratorUtil.copy(base, templatePaths);
    }

    public static CompendiumConfig testConfig(Tui tui) {
        TtrpgConfig ttrpgConfig = new TtrpgConfig();
        ttrpgConfig.tui = tui;
        return ttrpgConfig.getConfig(Datasource.tools5e);
    }

    public static CompendiumConfig copy(CompendiumConfig base, TemplatePaths newTemplates) {
        CompendiumConfig copy = new CompendiumConfig(base.ttrpgConfig, base.datasource, base.tui);

        copy.allSources = base.allSources;
        copy.paths = base.paths;

        copy.adventures.addAll(base.adventures);
        copy.books.addAll(base.books);

        copy.allowedSources.addAll(base.allowedSources);
        copy.customTemplates.putAll(newTemplates.customTemplates);
        copy.excludedKeys.addAll(base.excludedKeys);
        copy.excludedPatterns.addAll(base.excludedPatterns);
        copy.includeGroups.addAll(base.includeGroups);
        copy.includedKeys.addAll(base.includedKeys);

        return copy;
    }
}
