package dev.ebullient.convert.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.qute.Reprinted;
import dev.ebullient.convert.qute.SourceAndPage;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import io.quarkus.qute.TemplateData;

@TemplateData
public abstract class CompendiumSources {
    protected final IndexType type;
    protected final String key;
    protected final String name;

    // sources will only appear once, iterate by insertion order
    protected final Set<String> sources = new LinkedHashSet<>();
    protected final Set<SourceAndPage> bookRef = new LinkedHashSet<>();
    protected String sourceText;

    // Provides a list of sources that this is a reprint of
    protected final Set<CompendiumSources> reprintOf = new HashSet<>();
    // Source that this is a copy of
    protected final JsonNode copyElement;

    public CompendiumSources(IndexType type, String key, JsonNode jsonElement) {
        this.type = type;
        this.key = key;
        this.name = findName(type, jsonElement);
        // remember this: handling copies will remove the copy field from the element
        // to avoid repeated processing
        this.copyElement = Fields._copy.getFrom(jsonElement);
        initSources(jsonElement);
    }

    public String getSourceText() {
        if (sourceText == null) {
            sourceText = findSourceText(type, findNode());
        }
        return sourceText;
    }

    public Collection<String> getSources() {
        return sources;
    }

    /** Protected: used by Tags.addSourceTags(sources) */
    String primarySourceTag() {
        return String.format("compendium/src/%s/%s",
                TtrpgConfig.getConfig().datasource().shortName(),
                isSynthetic() ? "" : primarySource().toLowerCase());
    }

    public abstract JsonNode findNode();

    protected abstract String findName(IndexType type, JsonNode jsonElement);

    protected void initSources(JsonNode jsonElement) {
        // add the primary source...
        SourceAndPage primary = new SourceAndPage(jsonElement);
        if (primary.source != null) {
            this.sources.add(primary.source);
            this.bookRef.add(primary);
        } else if (type.defaultSourceString() != null) {
            // synthetic groups don't have a default source
            String source = type.defaultSourceString();
            this.sources.add(source);
            this.bookRef.add(new SourceAndPage(source, null));
        }

        String copySrc = SourceField.source.getTextOrNull(copyElement);

        if (Fields.additionalSources.existsIn(jsonElement)) {
            // Additional information from...
            Fields.additionalSources.streamFrom(jsonElement)
                    .map(SourceAndPage::new)
                    .filter(sp -> sp.source != null)
                    .filter(sp -> !sp.source.equals(copySrc))
                    .forEach(sp -> {
                        this.bookRef.add(sp);
                        this.sources.add(sp.source);
                    });
        }

        if (Fields.otherSources.existsIn(jsonElement)) {
            // Also found in...
            // This can be overly generous.. only add other sources that
            // are explicitly included in the configuration
            Fields.otherSources.streamFrom(jsonElement)
                    .map(SourceAndPage::new)
                    .filter(sp -> sp.source != null)
                    .filter(sp -> !sp.source.equals(copySrc))
                    .filter(sp -> TtrpgConfig.getConfig().sourceIncluded(sp.source))
                    .forEach(sp -> {
                        this.bookRef.add(sp);
                        this.sources.add(sp.source);
                    });
        }
    }

    protected String findSourceText(IndexType type, JsonNode jsonElement) {
        List<String> srcText = new ArrayList<>();

        final SourceAndPage primary = bookRef.iterator().next();
        List<SourceAndPage> consolidated = bookRef.stream()
                .reduce(new ArrayList<>(), (list, sp) -> {
                    if (list.isEmpty()) {
                        list.add(sp);
                    } else {
                        SourceAndPage existing = list.stream()
                                .filter(x -> x.source.equals(sp.source))
                                .findFirst()
                                .orElse(null);
                        if (existing == null) {
                            list.add(sp);
                        } else if (existing.page != null) {
                            SourceAndPage replace = new SourceAndPage(existing.source, null);
                            list.remove(existing);
                            if (existing == primary) {
                                list.add(0, replace);
                            } else {
                                list.add(replace);
                            }
                        }
                    }
                    return list;
                }, (a, b) -> {
                    a.addAll(b);
                    return a;
                });

        final SourceAndPage first = consolidated.iterator().next();
        if (first.source != null) {
            srcText.add(first.toString());
        }

        String copyOf = SourceField.name.getTextOrNull(copyElement);
        String copySrc = SourceField.source.getTextOrNull(copyElement);
        String copiedFrom = Fields._copiedFrom.getTextOrNull(copyElement);

        if (copyOf != null) {
            srcText.add(String.format("Derived from %s (%s)", copyOf, copySrc));
        } else if (copiedFrom != null) {
            srcText.add(String.format("Derived from %s", copiedFrom));
        }

        // find/add additional sources
        consolidated.stream()
                .filter(sp -> sp != first && sp.source != null)
                .filter(sp -> !sp.source.equals(copySrc))
                .forEach(sp -> srcText.add(sp.toString()));

        return String.join(", ", srcText);
    }

    public boolean isPrimarySource(String source) {
        return source.equalsIgnoreCase(primarySource());
    }

    public String primarySource() {
        if (sources.isEmpty()) {
            return type.defaultSourceString();
        }
        return sources.iterator().next();
    }

    public String mapPrimarySource() {
        String primary = primarySource();
        return TtrpgConfig.sourceToAbbreviation(primary);
    }

    public boolean includedBy(Set<String> sources) {
        return TtrpgConfig.getConfig().allSources()
                || this.sources.stream().anyMatch(x -> sources.contains(x.toLowerCase()));
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

    public Collection<SourceAndPage> getSourceAndPage() {
        return bookRef;
    }

    public Collection<Reprinted> getReprints() {
        return reprintOf.stream()
                .map(s -> new Reprinted(s.getName(), s.primarySource()))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "sources[" + key + ']';
    }

    public void checkKnown() {
        TtrpgConfig.checkKnown(this.sources);
    }

    public void addReprint(CompendiumSources reprint) {
        this.reprintOf.add(reprint);
    }

    /** Documents that have no primary source (compositions) */
    protected boolean isSynthetic() {
        return false;
    }

    protected enum Fields implements JsonNodeReader {
        _copy,
        _copiedFrom,
        additionalSources,
        otherSources,
    }
}
