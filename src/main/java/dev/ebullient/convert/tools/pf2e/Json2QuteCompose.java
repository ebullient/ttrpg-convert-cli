package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.toAnchorTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;

public class Json2QuteCompose extends Json2QuteBase {
    final List<JsonNode> nodes = new ArrayList<>();
    Pf2eSources currentSources;
    final String title;

    public Json2QuteCompose(Pf2eIndexType type, Pf2eIndex index, String title) {
        super(index, type, null,
                Pf2eSources.constructSyntheticSource(title));
        currentSources = super.getSources();
        this.title = title;
    }

    public void add(JsonNode node) {
        nodes.add(node);
        if (ComposeFields.alias.existsIn(node)) {
            String name = SourceField.name.getTextOrEmpty(node);
            JsonNode entries = ((ObjectNode) node).arrayNode()
                    .add(String.format("See [%s](#%s)", name, toAnchorTag(name)));

            for (String alias : ComposeFields.alias.getListOfStrings(node, tui())) {
                ObjectNode copy = (ObjectNode) copyNode(node);
                copy.put("name", alias);
                copy.remove("alias");
                copy.set("entries", entries);
                nodes.add(copy);
            }
        }
    }

    @Override
    public Pf2eSources getSources() {
        return currentSources;
    }

    @Override
    public Pf2eQuteNote buildNote() {
        // Override because we don't have global or even current sources here
        // We have to push/pop source-related state as we work through
        // contents (appendElement)
        Tags tags = new Tags();
        List<String> text = new ArrayList<>();

        nodes.sort(Comparator.comparing(SourceField.name::getTextOrEmpty));
        for (JsonNode entry : nodes) {
            appendElement(entry, text, tags);
        }

        return new Pf2eQuteNote(type,
                title,
                null,
                String.join("\n", text),
                tags);
    }

    private void appendElement(JsonNode entry, List<String> text, Tags tags) {
        String key = TtrpgValue.indexKey.getTextOrEmpty(entry);
        currentSources = Pf2eSources.findSources(key);
        String name = SourceField.name.getTextOrEmpty(entry);

        if (index.keyIsIncluded(key, entry)) {
            boolean pushed = parseState().push(entry);
            try {
                tags.addSourceTags(currentSources);
                maybeAddBlankLine(text);
                text.add("## " + replaceText(name));
                text.add(String.format("_Source: %s_", currentSources.getSourceText()));
                maybeAddBlankLine(text);
                appendToText(text, SourceField.entries.getFrom(entry), "###");
                appendToText(text, SourceField.entry.getFrom(entry), "###");

                // Special content for some types (added to text)
                addDomainSpells(name, text);

                maybeAddBlankLine(text);
            } finally {
                parseState().pop(pushed);
            }
        }
    }

    void addDomainSpells(String name, List<String> text) {
        Collection<String> spells = index().domainSpells(name);
        if (type != Pf2eIndexType.domain || spells.isEmpty()) {
            return;
        }
        maybeAddBlankLine(text);
        text.add("**Spells** " + spells.stream()
                .map(s -> index().getIncludedNode(s))
                .sorted(Comparator.comparingInt(n -> Json2QuteSpell.Pf2eSpell.level.intOrDefault(n, 1)))
                .map(Pf2eSources::findSources)
                .map(s -> linkify(Pf2eIndexType.spell, s.getName() + "|" + s.primarySource()))
                .collect(Collectors.joining(", ")));
    }

    enum ComposeFields implements Pf2eJsonNodeReader {
        alias
    }
}
