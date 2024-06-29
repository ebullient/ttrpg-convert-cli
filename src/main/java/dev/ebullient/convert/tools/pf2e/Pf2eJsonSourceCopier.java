package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ebullient.convert.tools.JsonSourceCopier;

import java.util.Set;

public class Pf2eJsonSourceCopier extends JsonSourceCopier<Pf2eIndexType> implements JsonSource {
    private static final Set<String> MERGE_PRESERVE_BASE = Set.of("page", "otherSources");
    private static final Set<String> MERGE_PRESERVE_CREATURE = Set.of(
        "page", "otherSources", "hasImages", "description");

    final Pf2eIndex index;

    Pf2eJsonSourceCopier(Pf2eIndex index) {
        this.index = index;
    }

    @Override
    public Pf2eIndex index() {
        return index;
    }

    @Override
    public Pf2eSources getSources() {
        throw new IllegalStateException("Should not call getSources while copying source");
    }

    @Override
    protected JsonNode getOriginNode(String key) {
        return index.getIncludedNode(key);
    }

    @Override
    protected boolean mergePreserveKey(Pf2eIndexType type, String key) {
        return switch (type) {
            case ritual, optfeature, spell, background,
                 deity, deityFluff, organization, organizationFluff,
                 creatureTemplate, creatureTemplateFluff -> MERGE_PRESERVE_BASE.contains(key);
            case creature -> MERGE_PRESERVE_CREATURE.contains(key);
            default -> false;
        };
    }

    @Override
    public JsonNode handleCopy(Pf2eIndexType type, JsonNode jsonSource) {

        return jsonSource;
    }
}
