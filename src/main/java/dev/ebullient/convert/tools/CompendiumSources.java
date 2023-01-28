package dev.ebullient.convert.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
                String.format("compendium/src/%s/%s",
                        TtrpgConfig.getConfig().datasource().shortName(),
                        isSynthetic() ? "" : primarySource().toLowerCase()));
    }

    protected abstract String findName(IndexType type, JsonNode jsonElement);

    protected String findSourceText(IndexType type, JsonNode jsonElement) {
        // add the primary source...
        String primarySource = Fields.source.getTextOrEmpty(jsonElement);
        if (primarySource != null) {
            this.bookSources.add(primarySource);
        }

        List<String> srcText = new ArrayList<>();
        String sPage = new SourceAndPage(jsonElement).toString();
        if (sPage != null) {
            srcText.add(sPage);
        }

        JsonNode copyElement = Fields._copy.getFrom(jsonElement);
        String copyOf = Fields.name.getTextOrNull(copyElement);
        String copySrc = Fields.source.getTextOrNull(copyElement);

        if (copyOf != null) {
            srcText.add(String.format("Derived from %s (%s)", copyOf, copySrc));
        }

        // find/add additional sources
        if (Fields.additionalSources.existsIn(jsonElement)) {
            srcText.addAll(Fields.additionalSources.streamOf(jsonElement)
                    .map(e -> new SourceAndPage(e))
                    .filter(sp -> sp.source != null)
                    .filter(sp -> !sp.source.equals(copySrc))
                    .filter(sp -> datasourceFilter(sp.source))
                    .peek(sp -> this.bookSources.add(sp.source))
                    .map(sp -> sp.toString())
                    .collect(Collectors.toList()));
        }
        if (Fields.otherSources.existsIn(jsonElement)) {
            srcText.addAll(Fields.otherSources.streamOf(jsonElement)
                    .map(e -> new SourceAndPage(e))
                    .filter(sp -> sp.source != null)
                    .filter(sp -> !sp.source.equals(copySrc))
                    .filter(sp -> datasourceFilter(sp.source))
                    .peek(sp -> this.bookSources.add(sp.source))
                    .map(sp -> sp.toString())
                    .collect(Collectors.toList()));
        }

        return String.join(", ", srcText);
    }

    protected boolean datasourceFilter(String source) {
        return true;
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

    protected enum Fields implements NodeReader {
        abbreviation,
        additionalSources,
        _copy,
        name,
        otherSources,
        page,
        source;
    }

    public static class SourceAndPage {
        String source;
        String page;

        SourceAndPage(JsonNode jsonElement) {
            source = Fields.source.getTextOrNull(jsonElement);
            page = Fields.page.getTextOrNull(jsonElement);
        }

        public String toString() {
            if (source != null) {
                String book = TtrpgConfig.sourceToLongName(source);
                if (page != null) {
                    return String.format("%s p. %s", book, page);
                }
                return book;
            }
            return null;
        }
    }
}
