package dev.ebullient.convert.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.MarkdownWriter.FileMap;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.dnd5e.qute.QuteName;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

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
                    return engine.parse(Files.readString(customPath));
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
        IndexType type = resource.type();
        String key = String.format("%s2md.txt", type.templateName());

        Template tpl = customTemplateOrDefault(key);
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

    public String renderName(QuteName resource) {
        Template tpl = customTemplateOrDefault("name2md.txt");
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    public String renderNote(QuteNote resource) {
        Template tpl = customTemplateOrDefault(resource.template());
        return tpl
                .data("resource", resource)
                .render().trim();
    }
}
