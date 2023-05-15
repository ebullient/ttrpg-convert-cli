package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;

public class Json2QuteCompose extends Json2QuteBase {
    List<JsonNode> nodes = new ArrayList<>();
    Pf2eIndexType type;
    Pf2eSources currentSources;
    String title;

    public Json2QuteCompose(Pf2eIndexType type, Pf2eIndex index, String title) {
        super(index, type, null,
                Pf2eSources.constructSyntheticSource(title));
        currentSources = super.getSources();
        this.title = title;
        this.type = type;
    }

    public void add(JsonNode node) {
        nodes.add(node);
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
        Set<String> tags = new HashSet<>();
        List<String> text = new ArrayList<>();

        nodes.sort(Comparator.comparing(Field.name::getTextOrEmpty));
        nodes.forEach(x -> appendElement(x, text, tags));
        appendFootnotes(text, 0);

        return new Pf2eQuteNote(type,
                title,
                null,
                String.join("\n", text),
                tags);
    }

    private void appendElement(JsonNode entry, List<String> text, Set<String> tags) {
        String key = TtrpgValue.indexKey.getFromNode(entry);
        currentSources = Pf2eSources.findSources(key);
        String name = Field.name.getTextOrNull(entry);

        if (index.keyIsIncluded(key, entry)) {
            boolean pushed = parseState.push(entry);
            try {
                tags.addAll(currentSources.getSourceTags());
                maybeAddBlankLine(text);
                text.add("## " + replaceText(name));
                maybeAddBlankLine(text);
                appendEntryToText(text, Field.entries.getFrom(entry), "###");
                appendEntryToText(text, Field.entry.getFrom(entry), "###");

                // Special content for some types (added to text)
                addDomainSpells(name, text);

                maybeAddBlankLine(text);
                text.add(String.format("_Source: %s_", currentSources.getSourceText()));
            } finally {
                parseState.pop(pushed);
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
                .sorted(Comparator.comparingInt(n -> Pf2eSpell.level.intOrDefault(n, 1)))
                .map(n -> Pf2eSources.findSources(n))
                .map(s -> linkify(Pf2eIndexType.spell, s.getName() + "|" + s.primarySource()))
                .collect(Collectors.joining(", ")));
    }
}
