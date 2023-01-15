package dev.ebullient.convert.config;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import dev.ebullient.convert.io.Templates;
import dev.ebullient.convert.io.Tui;

@ApplicationScoped
public class TtrpgConfig {
    final Map<Datasource, CompendiumConfig> sourceConfig = new HashMap<Datasource, CompendiumConfig>();
    final Map<String, String> fallbackImagePaths = new HashMap<>();

    Datasource datasource = Datasource.tools5e;

    @Inject
    Tui tui;

    @Inject
    Templates templates;

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }

    public CompendiumConfig getConfig() {
        return getConfig(datasource == null ? Datasource.tools5e : datasource);
    }

    public CompendiumConfig getConfig(Datasource datasource) {
        return sourceConfig.computeIfAbsent(datasource, (k) -> new CompendiumConfig(this, datasource, tui));
    }

    public Map<String, String> imageFallbackPaths() {
        return fallbackImagePaths;
    }
}
