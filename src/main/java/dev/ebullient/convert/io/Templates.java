package dev.ebullient.convert.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.MarkdownWriter.FileMap;
import dev.ebullient.convert.qute.QuteBase;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Templates {

    CompendiumConfig config = null;

    @Inject
    Tui tui;

    @Inject
    Engine engine;

    public void setCustomTemplates(CompendiumConfig config) {
        this.config = config;
        engine.clearTemplates();
    }

    private Template customTemplateOrDefault(String id) throws RuntimeException {
        if (config == null) {
            throw new IllegalStateException("Config not set");
        }

        String key = config.datasource() + "/" + id;

        if (!engine.isTemplateLoaded(key)) {
            Path customPath = config.getCustomTemplate(id);
            if (customPath != null) {
                tui.verbosef("📝 %s template: %s", id, customPath);
                try {
                    Template template = engine.parse(Files.readString(customPath));
                    engine.putTemplate(id, template);
                    return template;
                } catch (IOException e) {
                    tui.errorf(e, "Failed reading template for %s from %s", id, customPath);
                }
            }
            Template tpl = engine.getTemplate(key);
            if (tpl == null) {
                tui.errorf("Unable to find template for for %s", key);
                throw new RuntimeException("Unable to render content");
            }
        }
        return engine.getTemplate(key);
    }

    public String render(QuteBase resource) {
        Template tpl = customTemplateOrDefault(resource.template());
        return tpl
                .data("resource", resource)
                .render()
                .replaceAll("%%-- .*? --%%\\n", "")
                .trim();
    }

    public String renderInlineEmbedded(QuteBase resource) {
        Template tpl = customTemplateOrDefault(resource.template());
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    public String renderIndex(String name, Collection<FileMap> resources) {
        Template tpl = customTemplateOrDefault("index.txt");
        return tpl
                .data("name", name)
                .data("resources", resources)
                .render();
    }
}
