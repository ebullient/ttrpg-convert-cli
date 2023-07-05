package dev.ebullient.convert.tools.dnd5e;

import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteNote extends Json2QuteCommon {

    String title;
    boolean useSuffix;

    Json2QuteNote(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        title = index.replaceText(jsonNode.get("name").asText());
    }

    Json2QuteNote useSuffix(boolean useSuffix) {
        this.useSuffix = useSuffix;
        return this;
    }

    @Override
    String getName() {
        return title;
    }

    @Override
    protected Tools5eQuteNote buildQuteNote() {
        Set<String> tags = new TreeSet<>(sources.getSourceTags());
        String targetFile = useSuffix
                ? slugify(getName()) + Tools5eQuteBase.sourceIfNotCore(getSources().primarySource())
                : null;

        return new Tools5eQuteNote(title,
                sources.getSourceText(index.srdOnly()),
                getText("##"),
                tags)
                .withTargeFile(targetFile);
    }
}
