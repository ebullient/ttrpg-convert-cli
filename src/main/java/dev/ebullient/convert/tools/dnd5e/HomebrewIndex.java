package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.PsionicType.CustomPsionicType;
import dev.ebullient.convert.tools.dnd5e.SkillOrAbility.CustomSkillOrAbility;
import dev.ebullient.convert.tools.dnd5e.SpellSchool.CustomSpellSchool;

public class HomebrewIndex implements JsonSource {

    private final Map<String, HomebrewMetaTypes> homebrewMetaTypes = new HashMap<>();
    private final Tools5eIndex index;

    HomebrewIndex(Tools5eIndex index) {
        this.index = index;
    }

    public void importBrew(Consumer<HomebrewMetaTypes> processHomebrewTree) {
        for (HomebrewMetaTypes homebrew : homebrewMetaTypes.values()) {
            processHomebrewTree.accept(homebrew);
            for (var featureType : homebrew.optionalFeatureTypes.keySet()) {
                index.optFeatureIndex.addOptionalFeatureType(featureType, homebrew);
            }
        }
    }

    public Collection<HomebrewMetaTypes> getHomebrewMetaTypes(Tools5eSources sources) {
        Map<String, HomebrewMetaTypes> metaTypes = new HashMap<>();
        for (String src : sources.getSources()) {
            HomebrewMetaTypes meta = homebrewMetaTypes.get(src);
            if (meta != null) {
                metaTypes.put(meta.primary, meta);
            }
        }
        return metaTypes.values();
    }

    public HomebrewMetaTypes getHomebrewMetaTypes(String source) {
        return homebrewMetaTypes.get(source);
    }

    public SkillOrAbility findHomebrewSkillOrAbility(String key, Tools5eSources sources) {
        Collection<HomebrewMetaTypes> metaTypes = getHomebrewMetaTypes(sources);
        for (HomebrewMetaTypes meta : metaTypes) {
            SkillOrAbility skill = meta.getSkillType(key);
            if (skill != null) {
                return skill;
            }
        }
        return null;
    }

    public SpellSchool findHomebrewSpellSchool(String code, Tools5eSources sources) {
        Collection<HomebrewMetaTypes> metaTypes = getHomebrewMetaTypes(sources);
        for (HomebrewMetaTypes meta : metaTypes) {
            SpellSchool school = meta.getSpellSchool(code);
            if (school != null) {
                return school;
            }
        }
        return SpellSchool.SchoolEnum.None;
    }

    public ItemType findHomebrewType(String abbreviation, Tools5eSources sources) {
        Collection<HomebrewMetaTypes> metaTypes = getHomebrewMetaTypes(sources);
        for (HomebrewMetaTypes meta : metaTypes) {
            // key is lowercase abbreviation
            JsonNode node = meta.getItemType(abbreviation);
            if (node != null) {
                return ItemType.fromNode(node);
            }
        }
        return null;
    }

    public ItemMastery findHomebrewMastery(String name, Tools5eSources sources) {
        Collection<HomebrewMetaTypes> metaTypes = getHomebrewMetaTypes(sources);
        for (HomebrewMetaTypes meta : metaTypes) {
            // key is lowercase name
            JsonNode node = meta.getItemMastery(name);
            if (node != null) {
                return ItemMastery.fromNode(node);
            }
        }
        return null;
    }

    public ItemProperty findHomebrewProperty(String code, Tools5eSources sources) {
        Collection<HomebrewMetaTypes> metaTypes = getHomebrewMetaTypes(sources);
        for (HomebrewMetaTypes meta : metaTypes) {
            JsonNode node = meta.getItemProperty(code);
            if (node != null) {
                return ItemProperty.fromNode(node);
            }
        }
        return null;
    }

    public void clear() {
        homebrewMetaTypes.clear();
    }

    public boolean addHomebrewSourcesIfPresent(String filename, JsonNode brewNode) {
        JsonNode meta = SourceField._meta.getFrom(brewNode);
        JsonNode sources = HomebrewFields.sources.getFrom(meta);
        if (sources == null || sources.size() == 0) {
            return false;
        }
        Set<String> definedSources = new HashSet<>();

        for (JsonNode s : iterableElements(sources)) {
            String json = HomebrewFields.json.getTextOrNull(s);
            if (json == null) {
                tui().errorf(Msg.BREW, "Source does not define json id: %s", s);
                continue;
            }
            String fullName = HomebrewFields.full.getTextOrEmpty(s);
            String abbreviation = HomebrewFields.abbreviation.getTextOrEmpty(s);
            if (fullName == null) {
                tui().warnf(Msg.BREW, "Homebrew source %s missing full name: %s", json, fullName);
            }
            TtrpgConfig.addHomebrewSource(fullName, json, abbreviation); // define source
            TtrpgConfig.includeAdditionalSource(json); // include source
            definedSources.add(json);
        }

        HomebrewMetaTypes metaTypes = new HomebrewMetaTypes(definedSources,
                filename, brewNode, Tools5eFields.edition.getTextOrDefault(meta, "classic"));

        for (var src : definedSources) {
            homebrewMetaTypes.compute(src, (k, v) -> {
                if (v == null) {
                    return metaTypes;
                }
                tui().errorf(Msg.BREW, "Shared homebrew id %s: %s and %s; ignoring definition in %s",
                        src, v.filename, v.filename);
                return v;
            });
        }

        // --- From meta of homebrew ---

        for (Entry<String, JsonNode> entry : HomebrewFields.optionalFeatureTypes.iterateFieldsFrom(meta)) {
            metaTypes.setOptionalFeatureType(entry.getKey(), entry.getValue().asText());
        }

        // ignoring short names for spell schools and psionic types
        for (Entry<String, JsonNode> entry : HomebrewFields.spellSchools.iterateFieldsFrom(meta)) {
            metaTypes.setSpellSchool(entry.getKey(), entry.getValue());
        }

        for (Entry<String, JsonNode> entry : HomebrewFields.psionicTypes.iterateFieldsFrom(meta)) {
            metaTypes.setPsionicType(entry.getKey(), entry.getValue());
        }

        Tools5eSources.addFonts(meta, HomebrewFields.fonts);

        return true;
    }

    static class HomebrewMetaTypes {
        final String primary;
        final Set<String> sourceKeys;
        final String filename;
        final JsonNode homebrewNode;
        final String edition;

        // name, long name
        final Map<String, String> optionalFeatureTypes = new HashMap<>();
        final Map<String, PsionicType> psionicTypes = new HashMap<>();
        final Map<String, SkillOrAbility> skillOrAbility = new HashMap<>();
        final Map<String, CustomSpellSchool> spellSchoolTypes = new HashMap<>();
        final Map<String, JsonNode> itemTypes = new HashMap<>();
        final Map<String, JsonNode> itemProperties = new HashMap<>();
        final Map<String, JsonNode> itemMastery = new HashMap<>();

        HomebrewMetaTypes(Set<String> sourceKeys, String filename, JsonNode homebrewNode, String edition) {
            this.primary = sourceKeys.iterator().next();
            this.sourceKeys = sourceKeys;
            this.filename = filename;
            this.homebrewNode = homebrewNode;
            this.edition = edition;
        }

        public String getOptionalFeatureType(String key) {
            return optionalFeatureTypes.get(key.toLowerCase());
        }

        public void setOptionalFeatureType(String key, String value) {
            optionalFeatureTypes.put(key.toLowerCase(), value);
        }

        public PsionicType getPsionicType(String key) {
            return psionicTypes.get(key.toLowerCase());
        }

        public void setPsionicType(String key, JsonNode value) {
            try {
                CustomPsionicType psionicType = Tui.MAPPER.convertValue(value, CustomPsionicType.class);
                psionicTypes.put(key.toLowerCase(), psionicType);
            } catch (IllegalArgumentException e) {
                Tui.instance().errorf(Msg.BREW, "Error reading psionic type %s: %s", key, value);
            }
        }

        public SkillOrAbility getSkillType(String key) {
            return skillOrAbility.get(key.toLowerCase());
        }

        public void setSkillType(String key, JsonNode skillNode) {
            try {
                CustomSkillOrAbility skill = new CustomSkillOrAbility(skillNode);
                skillOrAbility.put(key.toLowerCase(), skill);
            } catch (IllegalArgumentException e) {
                Tui.instance().errorf(Msg.BREW, "Error reading skill %s: %s", key, skillNode);
            }
        }

        public SpellSchool getSpellSchool(String key) {
            return spellSchoolTypes.get(key.toLowerCase());
        }

        public void setSpellSchool(String key, JsonNode spellNode) {
            try {
                CustomSpellSchool school = new CustomSpellSchool(key,
                        HomebrewFields.full.getTextOrEmpty(spellNode));
                spellSchoolTypes.put(key.toLowerCase(), school);
            } catch (IllegalArgumentException e) {
                Tui.instance().errorf(Msg.BREW, "Error reading skill %s: %s", key, spellNode);
            }
        }

        public JsonNode getItemType(String abbreviation) {
            return itemTypes.get(abbreviation.toLowerCase());
        }

        public JsonNode getItemProperty(String abbreviation) {
            return itemProperties.get(abbreviation.toLowerCase());
        }

        public JsonNode getItemMastery(String name) {
            return itemMastery.get(name.toLowerCase());
        }

        public void addCrossReference(Tools5eIndexType type, String key, JsonNode value) {
            String name = SourceField.name.getTextOrNull(value);
            String abbreviation = Tools5eFields.abbreviation.getTextOrNull(value);

            // Done before copies & variants are made
            TtrpgValue.homebrewBaseSource.setIn(value, SourceField.source.getTextOrEmpty(value));
            TtrpgValue.homebrewSource.setIn(value, SourceField.source.getTextOrEmpty(value));

            if (isPresent(abbreviation)) {
                // Make sure the key and sources have been constructed/assigned
                if (type == Tools5eIndexType.itemType) {
                    itemTypes.put(abbreviation.toLowerCase(), value);
                } else if (type == Tools5eIndexType.itemProperty) {
                    itemProperties.put(abbreviation.toLowerCase(), value);
                }
            } else if (isPresent(name)) {
                if (type == Tools5eIndexType.itemMastery) {
                    itemMastery.put(name.toLowerCase(), value);
                } else if (type == Tools5eIndexType.skill) {
                    setSkillType(name.toLowerCase(), value);
                }
            }
        }
    }

    enum HomebrewFields implements JsonNodeReader {
        abbreviation,
        fonts,
        full,
        json,
        optionalFeatureTypes,
        psionicTypes,
        skill,
        sources,
        spellSchools,
        spellDistanceUnits
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
}
