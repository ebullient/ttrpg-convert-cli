package dev.ebullient.convert.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import io.quarkus.qute.TemplateData;

@TemplateData
public class CompendiumSources {
    protected final IndexType type;
    protected final String key;
    protected final String name;
    protected final Set<String> bookSources = new LinkedHashSet<>();
    protected final String sourceText;

    public CompendiumSources(IndexType type, String key, JsonNode jsonElement) {
        this.type = type;
        this.key = key;
        this.name = (jsonElement.has("name")
                ? jsonElement.get("name").asText()
                : jsonElement.get("abbreviation").asText()).trim();
        this.sourceText = findSourceText(jsonElement);
    }

    public String getSourceText(boolean useSrd) {
        return sourceText;
    }

    public Collection<String> getBookSources() {
        return bookSources;
    }

    public List<String> getSourceTags() {
        return List.of("compendium/src/" + primarySource().toLowerCase());
    }

    protected String findSourceText(JsonNode jsonElement) {
        this.bookSources.add(jsonElement.get("source").asText());

        List<String> srcText = new ArrayList<>();
        srcText.add(sourceAndPage(jsonElement));

        String copyOf = jsonElement.has("_copy")
                ? jsonElement.get("_copy").get("name").asText()
                : null;
        String copySrc = jsonElement.has("_copy")
                ? jsonElement.get("_copy").get("source").asText()
                : null;

        if (copyOf != null) {
            srcText.add(String.format("Derived from %s (%s)", copyOf, copySrc));
        }

        if (jsonElement.has("additionalSources")) {
            srcText.addAll(StreamSupport.stream(jsonElement.withArray("additionalSources").spliterator(), false)
                    .filter(x -> !x.get("source").asText().equals(copySrc))
                    .filter(x -> subclassFilter(x.get("source").asText()))
                    .peek(x -> this.bookSources.add(x.get("source").asText()))
                    .map(this::sourceAndPage)
                    .collect(Collectors.toList()));
        }

        if (jsonElement.has("otherSources")) {
            srcText.addAll(StreamSupport.stream(jsonElement.withArray("otherSources").spliterator(), false)
                    .filter(x -> !x.get("source").asText().equals(copySrc))
                    .filter(x -> subclassFilter(x.get("source").asText()))
                    .peek(x -> this.bookSources.add(x.get("source").asText()))
                    .map(this::sourceAndPage)
                    .collect(Collectors.toList()));
        }

        return sourceText;
    }

    protected boolean subclassFilter(String source) {
        return !List.of("phb", "mm", "dmg").contains(source.toLowerCase());
    }

    private String sourceAndPage(JsonNode source) {
        String src = source.get("source").asText();
        String book = abvToName.getOrDefault(src, src);
        if (source.has("page")) {
            return String.format("%s p. %s", book, source.get("page").asText());
        }
        return book;
    }

    public boolean isPrimarySource(String source) {
        return bookSources.iterator().next().equals(source);
    }

    public String primarySource() {
        return bookSources.iterator().next();
    }

    public String mapPrimarySource() {
        String primary = primarySource();
        return sourceToAbv.getOrDefault(primary, primary);
    }

    public String alternateSource() {
        Iterator<String> i = bookSources.iterator();
        if (bookSources.size() > 1) {
            i.next();
        }
        return i.next();
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public IndexType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "sources[" + key + ']';
    }

    protected final static Map<String, String> abvToName = new HashMap<>();
    protected final static Map<String, String> sourceToAbv = new HashMap<>();

    public static String sourceToLongName(String src) {
        return abvToName.getOrDefault(sourceToAbbreviation(src), src);
    }

    public static String sourceToAbbreviation(String src) {
        return sourceToAbv.getOrDefault(src, src);
    }

    public void checkKnown(Tui tui, Set<String> missing) {
        bookSources.forEach(s -> {
            if (abvToName.containsKey(s)) {
                return;
            }
            String alternate = sourceToAbv.get(s);
            if (alternate != null) {
                return;
            }
            if (missing.add(s)) {
                tui.warnf("Source %s is unknown", s);
            }
        });
    }
}
