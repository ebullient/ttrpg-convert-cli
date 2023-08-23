package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.OptionalFeatureType;
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
        List<JsonNode> nodes = optionalFeatures.nodes;
        String key = super.sources.getKey();
        if (nodes.isEmpty() || index().isExcluded(key)) {
            return null;
        }
        nodes.sort(Comparator.comparing(SourceField.name::getTextOrEmpty));

        Tags tags = new Tags(getSources());
        List<String> text = new ArrayList<>();

        for (JsonNode entry : nodes) {
            Tools5eSources featureSource = Tools5eSources.findSources(entry);
            if (index().isExcluded(featureSource.getKey())) {
                continue;
            }
            text.add("- " + linkify(Tools5eIndexType.optionalfeature,
                    featureSource.getName()
                            + "|" + featureSource.primarySource()
                            + "|" + decoratedUaName(featureSource.getName(), featureSource)));
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
