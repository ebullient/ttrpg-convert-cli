package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joinConjunct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.dnd5e.HomebrewIndex.HomebrewMetaTypes;
import dev.ebullient.convert.tools.dnd5e.Json2QuteClass.ClassFields;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class OptionalFeatureIndex implements JsonSource {
    private final Map<String, OptionalFeatureType> optFeatureIndex = new HashMap<>();
    private final Set<String> unresolvedFeatureTypes = new HashSet<>();
    private final Tools5eIndex index;

    OptionalFeatureIndex(Tools5eIndex index) {
        this.index = index;
    }

    public OptionalFeatureType addOptionalFeatureType(String featureType, HomebrewMetaTypes homebrew) {
        // scope the optional feature key (homebrew may conflict)
        try {
            var oft = optFeatureIndex.computeIfAbsent(featureType.toLowerCase(),
                    k -> new OptionalFeatureType(featureType, homebrew, index()));
            oft.addHomebrewMeta(homebrew);
            return oft;
        } catch (IllegalArgumentException e) {
            tui().errorf(e, "Unable to define optional feature");
        }
        return null;
    }

    public void addOptionalFeature(String finalKey, JsonNode optFeatureNode, HomebrewMetaTypes homebrew) {
        for (String ft : OftFields.featureType.getListOfStrings(optFeatureNode, tui())) {
            var oft = addOptionalFeatureType(ft, homebrew);
            if (oft != null) {
                oft.addFeature(finalKey);
            }
        }
    }

    public void amendSources(String key, JsonNode jsonSource) {
        Tools5eSources sources = Tools5eSources.findSources(key);
        if (sources.getType() == Tools5eIndexType.optfeature) {
            for (String featureType : OftFields.featureType.getListOfStrings(jsonSource, tui())) {
                OptionalFeatureType oft = get(featureType);
                if (oft == null) {
                    tui().warnf(Msg.UNRESOLVED, "OptionalFeatureType %s not found for %s", jsonSource, key);
                } else {
                    oft.amendSources(sources);
                }
            }
        } else {
            for (JsonNode ofp : ClassFields.optionalfeatureProgression.iterateArrayFrom(jsonSource)) {
                for (String featureType : Tools5eFields.featureType.getListOfStrings(ofp, tui())) {
                    // class/subclass source matters for homebrew scope (if necessary)
                    OptionalFeatureType oft = get(featureType);
                    if (oft == null) {
                        tui().warnf(Msg.UNRESOLVED, "OptionalFeatureType %s not found for %s",
                                featureType, key);
                        continue;
                    }
                    oft.addConsumer(key);
                    oft.amendSources(sources);
                }
            }
        }
    }

    public void removeUnusedOptionalFeatures(
            Function<String, Boolean> testInUse,
            Consumer<String> keep,
            Consumer<String> remove) {
        for (var oft : optFeatureIndex.values()) {
            // Test to see if any of the features using this type are still active.
            if (oft.testFeaturesInUse(testInUse) || oft.testConsumersInUse(testInUse)) {
                keep.accept(oft.getKey());
                continue;
            }

            // Remove the feature type
            remove.accept(oft.getKey());
            // Remove all features associated with this type
            oft.features.forEach(remove);
        }
    }

    public OptionalFeatureType get(JsonNode node) {
        if (node == null) {
            return null;
        }
        String lookup = SourceField.name.getTextOrEmpty(node);
        return lookup == null ? null : optFeatureIndex.get(lookup.toLowerCase());
    }

    public OptionalFeatureType get(String featureType) {
        var lowerType = featureType.toLowerCase();
        OptionalFeatureType type = optFeatureIndex.get(lowerType);
        if (type == null && unresolvedFeatureTypes.add(lowerType)) {
            tui().logf(Msg.UNRESOLVED, "OptionalFeatureType %s not found", lowerType);
        }
        return type;
    }

    public void clear() {
        optFeatureIndex.clear();
    }

    public Map<String, OptionalFeatureType> getMap() {
        return optFeatureIndex;
    }

    public static class OptionalFeatureCondition {
        final int order;
        final String name;
        final List<String> includes = new ArrayList<>();
        final List<String> includeConditions = new ArrayList<>();

        final List<String> excludes = new ArrayList<>();
        final List<String> excludeConditions = new ArrayList<>();

        public OptionalFeatureCondition(int order, String name,
                List<String> conditions,
                Function<String, String> transform) {
            this.order = order;
            this.name = name;

            for (String condition : conditions) {
                if (condition.startsWith("!")) {
                    String exclude = condition.substring(1);
                    excludeConditions.add(exclude);
                    excludes.add(transform.apply(exclude));
                } else {
                    includeConditions.add(condition);
                    includes.add(transform.apply(condition));
                }
            }
        }

        public int order() {
            return order;
        }

        public boolean isEmpty() {
            return includes.isEmpty() && excludes.isEmpty();
        }

        @Override
        public String toString() {
            if (!includes.isEmpty() && !excludes.isEmpty()) {
                return String.format("%s %s, excluding %s", name,
                        joinConjunct(" or ", includes), join(" and ", excludes));
            } else if (!includes.isEmpty()) {
                return String.format("%s %s", name, joinConjunct(" or ", includes));
            } else if (!excludes.isEmpty()) {
                return String.format("%s excluding %s", name, joinConjunct(" and ", includes));
            }
            return "";
        }
    }

    /**
     * This is included in all-index.json
     */
    static class OptionalFeatureType {

        final String featureTypeKey;
        final String abbreviation;
        final List<String> features = new ArrayList<>();
        final List<String> consumers = new ArrayList<>();

        @JsonIgnore
        final ObjectNode featureTypeNode;

        @JsonIgnore
        final Map<String, HomebrewMetaTypes> homebrewMeta = new HashMap<>();

        Tools5eSources sources; // deferred initialization

        OptionalFeatureType(String abbreviation, HomebrewMetaTypes homebrewMeta, Tools5eIndex index) {
            this.abbreviation = abbreviation;
            String primarySource = getSource(homebrewMeta);

            featureTypeNode = Tui.MAPPER.createObjectNode();
            featureTypeNode.put("name", abbreviation);
            featureTypeNode.put("source", primarySource);

            if (inSRD(abbreviation)) {
                featureTypeNode.put("srd", true);
                featureTypeNode.put("srd52", true);
            }
            // KNOCK-ON: Add to index
            this.featureTypeKey = Tools5eIndexType.optionalFeatureTypes.createKey(featureTypeNode);
            index.addToIndex(Tools5eIndexType.optionalFeatureTypes, featureTypeNode);
            // wait to construct sources
        }

        public void amendSources(Tools5eSources otherSources) {
            var mySources = mySources();
            // Update sources from those of a consuming/using class or subclass
            // Optional features will always add to sources of types
            if (otherSources.getType() == Tools5eIndexType.optfeature
                    || otherSources.contains(mySources)) {
                mySources.amendSources(otherSources);
            }
        }

        public void addHomebrewMeta(HomebrewMetaTypes homebrew) {
            if (homebrew != null) {
                homebrewMeta.put(homebrew.primary, homebrew);
                mySources().amendSources(homebrew.sourceKeys);
            }
        }

        private Tools5eSources mySources() {
            if (this.sources == null) {
                this.sources = Tools5eSources.constructSources(featureTypeKey, featureTypeNode);
            }
            return this.sources;
        }

        public void addConsumer(String key) {
            consumers.add(key);
        }

        public void addFeature(String key) {
            features.add(key);
        }

        public String getFilename() {
            return linkifier().getOptionalFeatureTypeResource(abbreviation);
        }

        public Tools5eSources getSources() {
            return Tools5eSources.findSources(featureTypeKey);
        }

        public boolean testConsumersInUse(Function<String, Boolean> test) {
            return consumers.stream()
                    .map(k -> test.apply(k))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);
        }

        public boolean testFeaturesInUse(Function<String, Boolean> test) {
            return features.stream()
                    .map(k -> test.apply(k))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);
        }

        public String getTitle() {
            String title = JsonSource.featureTypeToString(abbreviation);
            if (title.equalsIgnoreCase(abbreviation)) {
                if (!homebrewMeta.isEmpty()) {
                    return homebrewMeta.values().stream()
                            .map(hb -> hb.getOptionalFeatureType(abbreviation))
                            .distinct()
                            .collect(Collectors.joining("; "));
                }
                Tui.instance().warnf(Msg.NOT_SET, "Missing title for OptionalFeatureType in %s",
                        abbreviation);
                return abbreviation;
            }
            return title;
        }

        private String getSource(HomebrewMetaTypes homebrewMeta) {
            return switch (abbreviation) {
                case "AF" -> "UAA";
                case "AI", "RN" -> "TCE";
                case "AS", "FS:B" -> "XGE";
                case "AS:V1-UA" -> "UAF";
                case "AS:V2-UA" -> "UARSC";
                case "MV:C2-UA" -> "UARCO";
                case "OR" -> "UACDW";
                case "TT" -> "HWCS";
                default -> {
                    if (homebrewMeta != null) {
                        yield homebrewMeta.primary;
                    }
                    yield "PHB";
                }
            };
        }

        private boolean inSRD(String abbreviation) {
            return switch (abbreviation) {
                case "EI", "FS:F", "FS:R", "FS:P", "MM", "PB" -> true;
                default -> false;
            };
        }

        @JsonIgnore
        String getKey() {
            return featureTypeKey;
        }

        Tools5eLinkifier linkifier() {
            return Tools5eLinkifier.instance();
        }
    }

    @Override
    public CompendiumConfig cfg() {
        return index.config;
    }

    @Override
    public Tools5eIndex index() {
        return index;
    }

    @Override
    public Tools5eSources getSources() {
        return null;
    }

    enum OftFields implements JsonNodeReader {
        featureType,
        optionalFeatureTypes,
    }
}
