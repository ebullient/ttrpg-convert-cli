package dev.ebullient.convert.config;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import dev.ebullient.convert.io.Tui;

@ApplicationScoped
public class TtrpgConfig {
    final Map<Datasource, CompendiumConfig> sourceConfig = new HashMap<Datasource, CompendiumConfig>();
    final Map<String, String> fallbackImagePaths = new HashMap<>();

    Datasource game;

    @Inject
    Tui tui;

    public CompendiumConfig getConfig() {
        return getConfig(game == null ? Datasource.tools5e : game);
    }

    public CompendiumConfig getConfig(Datasource ttrpg) {
        return sourceConfig.computeIfAbsent(ttrpg, (k) -> new CompendiumConfig(this, ttrpg, tui));
    }

    public Map<String, String> imageFallbackPaths() {
        return fallbackImagePaths;
    }
}
