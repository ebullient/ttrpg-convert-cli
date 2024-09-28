package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.OptionalFeatureIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteOptionalFeatureType extends Json2QuteCommon {

    final OptionalFeatureType optionalFeatures;
    final String title;

    Json2QuteOptionalFeatureType(Tools5eIndex index, JsonNode node, OptionalFeatureType optionalFeatures) {
        super(index, Tools5eIndexType.optionalFeatureTypes, node);
        this.optionalFeatures = optionalFeatures;
        this.title = optionalFeatures.title;
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    protected Tools5eQuteNote buildQuteNote() {
        List<String> featureKeys = optionalFeatures.features;
        List<JsonNode> nodes = featureKeys.stream()
                .map(index::getAliasOrDefault)
                .map(index::getNode)
                .filter(x -> x != null)
                .sorted(Comparator.comparing(SourceField.name::getTextOrEmpty))
                .toList();

        String key = super.sources.getKey();
        if (nodes.isEmpty() || index().isExcluded(key)) {
            return null;
        }

        Tags tags = new Tags(getSources());
        List<String> text = new ArrayList<>();

        for (JsonNode entry : nodes) {
            text.add("- " + linkify(Tools5eIndexType.optfeature, Tools5eIndexType.optfeature.toTagReference(entry)));
        }
        if (text.isEmpty()) {
            return null;
        }

        String sourceText = super.sources.getSourceText(index().srdOnly());
        return new Tools5eQuteNote(title, sourceText, text, tags)
                .withTargetFile(optionalFeatures.getFilename())
                .withTargetPath(Tools5eIndexType.optionalFeatureTypes.getRelativePath());
    }
}
