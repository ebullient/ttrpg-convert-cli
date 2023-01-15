package dev.ebullient.convert.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.io.MarkdownWriter.FileMap;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.dnd5e.qute.QuteName;
import dev.ebullient.convert.tools.pf2e.ToolsPf2eIndexType;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

@ApplicationScoped
public class Templates {

    final Map<String, Template> templates = new HashMap<>();
    CompendiumConfig config = null;

    @Inject
    Tui tui;

    @Inject
    Engine engine;

    @Inject
    public Template index;

    @Inject
    public Template background2md;

    @Inject
    public Template class2md;

    @Inject
    public Template deity2md;

    @Inject
    public Template feat2md;

    @Inject
    public Template item2md;

    @Inject
    public Template monster2md;

    @Inject
    public Template name2md;

    @Inject
    public Template note2md;

    @Inject
    public Template race2md;

    @Inject
    public Template spell2md;

    @Inject
    public Template subclass2md;

    public void setCustomTemplates(CompendiumConfig config) {
        this.config = config;
        this.templates.clear();
    }

    private Template customTemplateOrDefault(String id, Template defaultTemplate) {
        if (config == null) {
            throw new IllegalStateException("Config not set");
        }
        Path customPath = config.getCustomTemplate(id);
        if (customPath != null) {
            tui.verbosef("📝 %s template: %s", id, customPath);
            try {
                return engine.parse(Files.readString(customPath));
            } catch (IOException e) {
                tui.errorf(e, "Failed reading template for %s from %s", id, customPath);
            }
        }
        return defaultTemplate;
    }

    public String render(QuteBase resource) {
        IndexType type = resource.type();
        String key = String.format("%s2md.txt", type.templateName());

        Template tpl = templates.computeIfAbsent(key, k -> {
            if (config == null) {
                throw new IllegalStateException("Config not set");
            }
            Path customPath = config.getCustomTemplate(key);
            if (customPath != null) {
                tui.verbosef("📝 %s template: %s", key, customPath);
                try {
                    return engine.parse(Files.readString(customPath));
                } catch (IOException e) {
                    tui.errorf(e, "Failed reading template for %s from %s", key, customPath);
                }
            }
            return getTemplateForType(config.datasource(), type);
        });

        return tpl
                .data("resource", resource)
                .render().trim();
    }

    public String renderIndex(String name, Collection<FileMap> resources) {
        return index
                .data("name", name)
                .data("resources", resources)
                .render();
    }

    public String renderName(QuteName resource) {
        Template tpl = templates.computeIfAbsent("name2md.txt", k -> customTemplateOrDefault(k, name2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    public String renderNote(QuteNote resource) {
        Template tpl = templates.computeIfAbsent("note2md.txt", k -> customTemplateOrDefault(k, note2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    Template getTemplateForType(Datasource datasource, IndexType type) {
        switch (datasource) {
            case toolsPf2e:
                return getPf2eTemplates((ToolsPf2eIndexType) type);
            default:
                return get5eToolsTemplates((Tools5eIndexType) type);
        }
    }

    private Template get5eToolsTemplates(Tools5eIndexType type) {
        switch (type) {
            case background:
                return background2md;
            case classtype:
                return class2md;
            case subclass:
                return subclass2md;
            case deity:
                return deity2md;
            case feat:
                return feat2md;
            case item:
                return item2md;
            case monster:
                return monster2md;
            case race:
            case subrace:
                return race2md;
            case spell:
                return spell2md;
            default:
                throw new IllegalArgumentException("No template for type:" + type);
        }
    }

    private Template getPf2eTemplates(ToolsPf2eIndexType type) {
        throw new IllegalArgumentException("No template for type:" + type);
    }

}
