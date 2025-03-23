package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class OptionalFeatureIndex implements JsonSource {
    private final Map<String, OptionalFeatureType> optFeatureIndex = new HashMap<>();
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
        return optFeatureIndex.get(featureType.toLowerCase());
    }

    public void clear() {
        optFeatureIndex.clear();
    }

    public Map<String, OptionalFeatureType> getMap() {
        return optFeatureIndex;
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
            return Tools5eQuteBase.getOptionalFeatureTypeResource(abbreviation);
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
            return switch (abbreviation) {
                case "AI" -> "Artificer Infusion";
                case "ED" -> "Elemental Discipline";
                case "EI" -> "Eldritch Invocation";
                case "MM" -> "Metamagic";
                case "MV" -> "Maneuver";
                case "MV:B" -> "Maneuver, Battle Master";
                case "MV:C2-UA" -> "Maneuver, Cavalier V2 (UA)";
                case "AS:V1-UA" -> "Arcane Shot, V1 (UA)";
                case "AS:V2-UA" -> "Arcane Shot, V2 (UA)";
                case "AS" -> "Arcane Shot";
                case "OTH" -> "Other";
                case "FS:F" -> "Fighting Style, Fighter";
                case "FS:B" -> "Fighting Style, Bard";
                case "FS:P" -> "Fighting Style, Paladin";
                case "FS:R" -> "Fighting Style, Ranger";
                case "PB" -> "Pact Boon";
                case "OR" -> "Onomancy Resonant";
                case "RN" -> "Rune Knight Rune";
                case "AF" -> "Alchemical Formula";
                case "TT" -> "Traveler's Trick";
                default -> {
                    if (!homebrewMeta.isEmpty()) {
                        yield homebrewMeta.values().stream()
                                .map(hb -> hb.getOptionalFeatureType(abbreviation))
                                .distinct()
                                .collect(Collectors.joining("; "));
                    }
                    Tui.instance().warnf(Msg.NOT_SET, "Missing title for OptionalFeatureType in %s",
                            abbreviation);
                    yield abbreviation;
                }
            };
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
