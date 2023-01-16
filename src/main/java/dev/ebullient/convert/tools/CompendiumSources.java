package dev.ebullient.convert.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import io.quarkus.qute.TemplateData;

@TemplateData
public abstract class CompendiumSources {
    protected final IndexType type;
    protected final String key;
    protected final String name;
    protected final Set<String> bookSources = new LinkedHashSet<>();
    protected final String sourceText;

    public CompendiumSources(IndexType type, String key, JsonNode jsonElement) {
        this.type = type;
        this.key = key;
        this.name = findName(type, jsonElement);
        this.sourceText = findSourceText(type, jsonElement);
    }

    public String getSourceText() {
        return sourceText;
    }

    public Collection<String> getBookSources() {
        return bookSources;
    }

    public List<String> getSourceTags() {
        return List.of(
                String.format("compendium/%s/src/%s",
                        TtrpgConfig.getConfig().datasource().shortName(),
                        isSynthetic() ? "" : primarySource().toLowerCase()));
    }

    protected abstract String findName(IndexType type, JsonNode jsonElement);

    protected String findSourceText(IndexType type, JsonNode jsonElement) {
        // add the primary source...
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

        // find/add additional sources
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

        return String.join(", ", srcText);
    }

    protected boolean subclassFilter(String source) {
        return !List.of("phb", "mm", "dmg").contains(source.toLowerCase());
    }

    private String sourceAndPage(JsonNode source) {
        String src = source.get("source").asText();
        String book = TtrpgConfig.sourceToLongName(src);
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
        return TtrpgConfig.sourceToAbbreviation(primary);
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

    public void checkKnown() {
        TtrpgConfig.checkKnown(this.bookSources);
    }

    boolean isSynthetic() {
        return type == Pf2eIndexType.syntheticGroup;
    }
}
