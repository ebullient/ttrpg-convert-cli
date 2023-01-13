package dev.ebullient.convert.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import dev.ebullient.convert.io.MarkdownWriter.FileMap;
import dev.ebullient.convert.qute.QuteBackground;
import dev.ebullient.convert.qute.QuteClass;
import dev.ebullient.convert.qute.QuteDeity;
import dev.ebullient.convert.qute.QuteFeat;
import dev.ebullient.convert.qute.QuteItem;
import dev.ebullient.convert.qute.QuteMonster;
import dev.ebullient.convert.qute.QuteName;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.qute.QuteRace;
import dev.ebullient.convert.qute.QuteSpell;
import dev.ebullient.convert.qute.QuteSubclass;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

@ApplicationScoped
public class Templates {

    final Map<String, Template> templates = new HashMap<>();
    TemplatePaths templatePaths = null;

    @Inject
    Tui tui;

    @Inject
    Engine engine;

    public void setCustomTemplates(TemplatePaths templatePaths) {
        if (templatePaths != null) {
            templatePaths.verify(tui);
        }

        this.templatePaths = templatePaths;
        this.templates.clear();
    }

    private Template customTemplateOrDefault(String id, Template defaultTemplate) {
        Path customPath = templatePaths == null ? null : templatePaths.get(id);
        if (customPath != null) {
            try {
                return engine.parse(Files.readString(customPath));
            } catch (IOException e) {
                tui.errorf(e, "Failed reading template for %s from %s", id, customPath);
            }
        }
        return defaultTemplate;
    }

    @Inject
    public Template index;

    public String renderIndex(String name, Collection<FileMap> resources) {
        return index
                .data("name", name)
                .data("resources", resources)
                .render();
    }

    @Inject
    public Template background2md;

    public String renderBackground(QuteBackground resource) {
        Template tpl = templates.computeIfAbsent("background2md.txt", k -> customTemplateOrDefault(k, background2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template class2md;

    public String renderClass(QuteClass resource) {
        Template tpl = templates.computeIfAbsent("class2md.txt", k -> customTemplateOrDefault(k, class2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template deity2md;

    public String renderDeity(QuteDeity resource) {
        Template tpl = templates.computeIfAbsent("deity2md.txt", k -> customTemplateOrDefault(k, deity2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template feat2md;

    public String renderFeat(QuteFeat resource) {
        Template tpl = templates.computeIfAbsent("feat2md.txt", k -> customTemplateOrDefault(k, feat2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template item2md;

    public String renderItem(QuteItem resource) {
        Template tpl = templates.computeIfAbsent("item2md.txt", k -> customTemplateOrDefault(k, item2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template monster2md;

    public String renderMonster(QuteMonster resource) {
        Template tpl = templates.computeIfAbsent("monster2md.txt", k -> customTemplateOrDefault(k, monster2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template name2md;

    public String renderName(QuteName resource) {
        Template tpl = templates.computeIfAbsent("name2md.txt", k -> customTemplateOrDefault(k, name2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template note2md;

    public String renderNote(QuteNote resource) {
        Template tpl = templates.computeIfAbsent("note2md.txt", k -> customTemplateOrDefault(k, note2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template race2md;

    public String renderRace(QuteRace resource) {
        Template tpl = templates.computeIfAbsent("race2md.txt", k -> customTemplateOrDefault(k, race2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template spell2md;

    public String renderSpell(QuteSpell resource) {
        Template tpl = templates.computeIfAbsent("spell2md.txt", k -> customTemplateOrDefault(k, spell2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Inject
    public Template subclass2md;

    public String renderSubclass(QuteSubclass resource) {
        Template tpl = templates.computeIfAbsent("subclass2md.txt", k -> customTemplateOrDefault(k, subclass2md));
        return tpl
                .data("resource", resource)
                .render().trim();
    }

    @Override
    public String toString() {
        return "Templates{" +
                "templates=" + templates +
                ", templatePaths=" + templatePaths +
                ", tui=" + tui +
                ", engine=" + engine +
                ", index=" + index +
                ", background2md=" + background2md +
                ", class2md=" + class2md +
                ", feat2md=" + feat2md +
                ", item2md=" + item2md +
                ", monster2md=" + monster2md +
                ", name2md=" + name2md +
                ", note2md=" + note2md +
                ", race2md=" + race2md +
                ", spell2md=" + spell2md +
                ", subclass2md=" + subclass2md +
                '}';
    }
}
