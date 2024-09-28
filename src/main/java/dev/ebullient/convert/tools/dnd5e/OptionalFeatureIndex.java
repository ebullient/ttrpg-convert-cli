package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.Json2QuteClass.ClassFields;
import dev.ebullient.convert.tools.dnd5e.Tools5eHomebrewIndex.HomebrewMetaTypes;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class OptionalFeatureIndex implements JsonSource {
    private final Map<String, OptionalFeatureType> optFeatureIndex = new HashMap<>();
    private final Tools5eIndex index;

    OptionalFeatureIndex(Tools5eIndex index) {
        this.index = index;
    }

    public void addOptionalFeature(String finalKey, JsonNode optFeatureNode, HomebrewMetaTypes homebrew) {
        String lookup = null;
        for (String ft : toListOfStrings(optFeatureNode.get("featureType"))) {
            try {
                boolean homebrewType = homebrew != null && homebrew.getOptionalFeatureType(ft) != null;
                // scope the optional feature key (homebrew may conflict)
                String featKey = (homebrewType ? ft + "-" + homebrew.jsonKey : ft).toLowerCase();

                optFeatureIndex.computeIfAbsent(featKey, k -> new OptionalFeatureType(ft, k, homebrew, index())).add(finalKey);
                lookup = lookup == null ? featKey : lookup;
            } catch (IllegalArgumentException e) {
                tui().errorf(e, "Unable to define optional feature");
            }
        }
        if (lookup != null) {
            OftFields.oftLookup.setIn(optFeatureNode, lookup);
            OftFields.oftIndexKey.setIn(optFeatureNode, optFeatureIndex.get(lookup).getKey());
        }
    }

    public void amendSources(String key, JsonNode jsonSource, Tools5eHomebrewIndex homebrewIndex) {
        Tools5eSources sources = Tools5eSources.findSources(key);
        if (sources.getType() == Tools5eIndexType.optfeature) {
            OptionalFeatureType oft = get(jsonSource);
            if (oft == null) {
                tui().warnf(Msg.UNRESOLVED, "OptionalFeatureType %s not found for %s", jsonSource, key);
            } else {
                oft.amendSources(sources);
            }
        } else {
            for (JsonNode ofp : ClassFields.optionalfeatureProgression.iterateArrayFrom(jsonSource)) {
                for (String featureType : Tools5eFields.featureType.getListOfStrings(ofp, tui())) {
                    // class/subclass source matters for homebrew scope (if necessary)
                    OptionalFeatureType oft = get(featureType, sources.primarySource(), homebrewIndex);
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

    public OptionalFeatureType get(Tools5eIndexType type, String key) {
        return switch (type) {
            case optfeature -> {
                JsonNode ofNode = index().getOrigin(key);
                String oftKey = OftFields.oftIndexKey.getTextOrNull(ofNode);
                JsonNode oftNode = index().getOrigin(oftKey);
                yield get(oftNode);
            }
            case optionalFeatureTypes -> {
                JsonNode node = index().getOrigin(key);
                yield get(node);
            }
            default -> null;
        };
    }

    public OptionalFeatureType get(JsonNode node) {
        if (node == null) {
            return null;
        }
        String lookup = OftFields.oftLookup.getTextOrNull(node);
        return lookup == null ? null : optFeatureIndex.get(lookup);
    }

    public OptionalFeatureType get(String ft, String source, Tools5eHomebrewIndex homebrewIndex) {
        HomebrewMetaTypes metaTypes = homebrewIndex.getHomebrewMetaTypes(source);
        String homebrewType = metaTypes == null
                ? null
                : metaTypes.getOptionalFeatureType(ft);

        OptionalFeatureType oft = optFeatureIndex.get(ft.toLowerCase());
        if (homebrewType != null) {
            String homebrewScoped = ft + "-" + metaTypes.jsonKey;
            OptionalFeatureType homebrewOft = optFeatureIndex.get(homebrewScoped.toLowerCase());
            return homebrewOft == null
                    ? oft
                    : homebrewOft;
        }
        return oft;
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
        final String lookupKey;
        final String featureTypeKey;
        final String abbreviation;
        final HomebrewMetaTypes homebrewMeta;
        final String title;
        final String source;
        final List<String> features = new ArrayList<>();
        final List<String> consumers = new ArrayList<>();

        @JsonIgnore
        final ObjectNode featureTypeNode;

        OptionalFeatureType(String abbreviation, String scopedAbv, HomebrewMetaTypes homebrewMeta, Tools5eIndex index) {
            this.abbreviation = abbreviation;
            this.lookupKey = scopedAbv;
            this.homebrewMeta = homebrewMeta;
            String tmpTitle = null;
            if (homebrewMeta != null) {
                tmpTitle = homebrewMeta.getOptionalFeatureType(abbreviation);
            }
            if (tmpTitle == null) {
                tmpTitle = switch (abbreviation) {
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
                    default -> null;
                };
            }
            if (tmpTitle == null) {
                index.tui().warnf(Msg.NOT_SET.wrap("Missing title for OptionalFeatureType in %s from %s"),
                        abbreviation,
                        homebrewMeta == null ? "unknown/core" : homebrewMeta.filename);
                tmpTitle = abbreviation;
            }
            title = tmpTitle;
            source = getSource(homebrewMeta);

            featureTypeNode = Tui.MAPPER.createObjectNode();
            featureTypeNode.put("name", scopedAbv);
            featureTypeNode.put("source", source);
            OftFields.oftLookup.setIn(featureTypeNode, lookupKey);

            if (inSRD(abbreviation)) {
                featureTypeNode.put("srd", true);
            }
            // KNOCK-ON: Add to index
            index.addToIndex(Tools5eIndexType.optionalFeatureTypes, featureTypeNode);
            featureTypeKey = TtrpgValue.indexKey.getTextOrThrow(featureTypeNode);
            Tools5eSources.constructSources(featureTypeKey, featureTypeNode);
        }

        public void amendSources(Tools5eSources otherSources) {
            // Update sources from those of a consuming/using class or subclass
            // Optional features will always add to sources of types
            Tools5eSources mySources = Tools5eSources.findSources(featureTypeNode);
            if (otherSources.getType() == Tools5eIndexType.optfeature
                    || otherSources.contains(mySources)) {
                mySources.amendSources(otherSources);
            }
        }

        public void addConsumer(String key) {
            consumers.add(key);
        }

        public void add(String key) {
            features.add(key);
        }

        public String getFilename() {
            return "list-" + Tools5eQuteBase.fixFileName(title, source, Tools5eIndexType.optionalFeatureTypes);
        }

        public Tools5eSources getSources() {
            return Tools5eSources.findSources(featureTypeKey);
        }

        public boolean inUse() {
            return features.stream()
                    .map(k -> Tools5eSources.includedByConfig(k))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);
        }

        private String getSource(HomebrewMetaTypes homebrewMeta) {
            if (homebrewMeta != null) {
                return homebrewMeta.jsonKey;
            }
            return switch (abbreviation) {
                case "AF" -> "UAA";
                case "AI", "RN" -> "TCE";
                case "AS", "FS:B" -> "XGE";
                case "AS:V1-UA" -> "UAF";
                case "AS:V2-UA" -> "UARSC";
                case "MV:C2-UA" -> "UARCO";
                case "OR" -> "UACDW";
                default -> "PHB";
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
        oftLookup,
        oftIndexKey,
    }
}
