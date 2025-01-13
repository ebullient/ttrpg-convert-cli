package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.dnd5e.PsionicType.CustomPsionicType;
import dev.ebullient.convert.tools.dnd5e.SkillOrAbility.CustomSkillOrAbility;
import dev.ebullient.convert.tools.dnd5e.SpellSchool.CustomSpellSchool;

public class Tools5eHomebrewIndex implements JsonSource {

    private final Map<String, HomebrewMetaTypes> homebrewMetaTypes = new HashMap<>();
    private final Tools5eIndex index;

    Tools5eHomebrewIndex(Tools5eIndex index) {
        this.index = index;
    }

    public void importBrew(Consumer<HomebrewMetaTypes> processHomebrewTree) {
        for (HomebrewMetaTypes homebrew : homebrewMetaTypes.values()) {
            processHomebrewTree.accept(homebrew);
        }
    }

    public HomebrewMetaTypes getHomebrewMetaTypes(Tools5eSources sources) {
        return homebrewMetaTypes.get(sources.primarySource());
    }

    public HomebrewMetaTypes getHomebrewMetaTypes(String source) {
        return homebrewMetaTypes.get(source);
    }

    public SkillOrAbility findHomebrewSkillOrAbility(String key, Tools5eSources sources) {
        HomebrewMetaTypes meta = homebrewMetaTypes.get(sources.primarySource());
        if (meta != null) {
            return meta.getSkillType(key);
        }
        return null;
    }

    public SpellSchool findHomebrewSpellSchool(String abbreviation, Tools5eSources sources) {
        HomebrewMetaTypes meta = homebrewMetaTypes.get(sources.primarySource());
        if (meta != null) {
            return meta.getSpellSchool(abbreviation);
        }
        return null;
    }

    public ItemType findHomebrewType(String fragment, Tools5eSources sources) {
        HomebrewMetaTypes meta = homebrewMetaTypes.get(sources.primarySource());
        if (meta != null) {
            JsonNode homebrewNode = meta.getItemProperty(fragment);
            if (homebrewNode != null) {
                return ItemType.fromNode(homebrewNode);
            }
        }
        return null;
    }

    public ItemMastery findHomebrewMastery(String fragment, Tools5eSources sources) {
        HomebrewMetaTypes meta = homebrewMetaTypes.get(sources.primarySource());
        if (meta != null) {
            JsonNode homebrewNode = meta.getItemMastery(fragment);
            if (homebrewNode != null) {
                return ItemMastery.fromNode(homebrewNode);
            }
        }
        return null;
    }

    public ItemProperty findHomebrewProperty(String fragment, Tools5eSources sources) {
        HomebrewMetaTypes meta = homebrewMetaTypes.get(sources.primarySource());
        if (meta != null) {
            JsonNode homebrewNode = meta.getItemProperty(fragment);
            if (homebrewNode != null) {
                return ItemProperty.fromNode(homebrewNode);
            }
        }
        return null;
    }

    public void clear() {
        homebrewMetaTypes.clear();
    }

    public boolean addHomebrewSourcesIfPresent(String filename, JsonNode node) {
        JsonNode sources = SourceField._meta.getFieldFrom(node, HomebrewFields.sources);
        if (sources == null || sources.size() == 0) {
            return false;
        }
        // TODO include homebrew date
        String json = HomebrewFields.json.getTextOrNull(sources.get(0));
        if (json == null) {
            tui().errorf("Source does not define json id: %s", sources.get(0));
            return false;
        }
        TtrpgConfig.includeAdditionalSource(json);

        HomebrewMetaTypes metaTypes = new HomebrewMetaTypes(json, filename, node);
        for (JsonNode source : iterableElements(sources)) {
            String fullName = HomebrewFields.full.getTextOrEmpty(source);
            String abbreviation = HomebrewFields.abbreviation.getTextOrEmpty(source);
            json = HomebrewFields.json.getTextOrEmpty(source);
            if (fullName == null) {
                tui().warnf(Msg.BREW, "Homebrew source %s missing full name: %s", json, fullName);
            }
            // add homebrew to known sources
            if (TtrpgConfig.addHomebrewSource(fullName, json, abbreviation)) {
                // one homebrew file may include multiple sources, the same mapping applies to
                // all
                HomebrewMetaTypes old = homebrewMetaTypes.put(json, metaTypes);
                if (old != null) {
                    tui().errorf(Msg.BREW, "Shared homebrew id: %s and %s", old.filename, metaTypes.filename);
                }
            } else {
                tui().errorf(Msg.BREW, "Skipping homebrew id %s from %s; duplicate source id", json, metaTypes.filename);
            }
        }

        JsonNode featureTypes = SourceField._meta.getFieldFrom(node, HomebrewFields.optionalFeatureTypes);
        JsonNode spellSchools = SourceField._meta.getFieldFrom(node, HomebrewFields.spellSchools);
        JsonNode psionicTypes = SourceField._meta.getFieldFrom(node, HomebrewFields.psionicTypes);
        JsonNode skillTypes = HomebrewFields.skill.getFrom(node);
        if (featureTypes != null || spellSchools != null || psionicTypes != null || skillTypes != null) {
            for (Entry<String, JsonNode> entry : iterableFields(featureTypes)) {
                metaTypes.setOptionalFeatureType(entry.getKey(), entry.getValue().asText());
            }
            // ignoring short names for spell schools and psionic types
            for (Entry<String, JsonNode> entry : iterableFields(spellSchools)) {
                metaTypes.setSpellSchool(entry.getKey(),
                        new CustomSpellSchool(HomebrewFields.full.getTextOrEmpty(entry.getValue())));
            }
            for (Entry<String, JsonNode> entry : iterableFields(psionicTypes)) {
                metaTypes.setPsionicType(entry.getKey(),
                        tui().readJsonValue(entry.getValue(), CustomPsionicType.class));
            }
            for (JsonNode skill : iterableElements(skillTypes)) {
                String skillName = SourceField.name.getTextOrEmpty(skill);
                if (skillName == null) {
                    tui().warnf(Msg.BREW, "Homebrew skill type missing name: %s", skill);
                    continue;
                }
                metaTypes.setSkillType(skillName, skill);
            }
        }
        Tools5eSources.addFonts(SourceField._meta.getFrom(node), HomebrewFields.fonts);
        return true;
    }

    static class HomebrewMetaTypes {
        final String jsonKey;
        final String filename;
        final JsonNode homebrewNode;
        // name, long name
        final Map<String, String> optionalFeatureTypes = new HashMap<>();
        final Map<String, PsionicType> psionicTypes = new HashMap<>();
        final Map<String, SkillOrAbility> skillOrAbility = new HashMap<>();
        final Map<String, CustomSpellSchool> spellSchoolTypes = new HashMap<>();
        final Map<String, JsonNode> itemTypes = new HashMap<>();
        final Map<String, JsonNode> itemProperties = new HashMap<>();
        final Map<String, JsonNode> itemMastery = new HashMap<>();

        HomebrewMetaTypes(String jsonKey, String filename, JsonNode homebrewNode) {
            this.jsonKey = jsonKey;
            this.filename = filename;
            this.homebrewNode = homebrewNode;
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

        public void setPsionicType(String key, PsionicType value) {
            psionicTypes.put(key.toLowerCase(), value);
        }

        public SkillOrAbility getSkillType(String key) {
            return skillOrAbility.get(key.toLowerCase());
        }

        public void setSkillType(String key, JsonNode skill) {
            skillOrAbility.put(key.toLowerCase(), new CustomSkillOrAbility(skill));
        }

        public SpellSchool getSpellSchool(String key) {
            return spellSchoolTypes.get(key.toLowerCase());
        }

        public void setSpellSchool(String key, CustomSpellSchool value) {
            spellSchoolTypes.put(key.toLowerCase(), value);
        }

        public JsonNode getItemType(String abbreviation) {
            return itemTypes.get(abbreviation);
        }

        public JsonNode getItemProperty(String abbreviation) {
            return itemProperties.get(abbreviation);
        }

        public JsonNode getItemMastery(String name) {
            return itemMastery.get(name);
        }

        public void addElement(Tools5eIndexType type, String key, JsonNode value) {
            String name = SourceField.name.getTextOrEmpty(value);
            String abbreviation = Tools5eFields.abbreviation.getTextOrEmpty(value);
            switch (type) {
                case itemMastery -> {
                    if (isPresent(name)) {
                        itemMastery.put(name, value);
                    } else {
                        Tui.instance().errorf(Msg.BREW, "Missing name in %s", key);
                    }
                }
                case itemProperty -> {
                    if (isPresent(abbreviation)) {
                        itemProperties.put(abbreviation, value);
                    } else {
                        Tui.instance().errorf(Msg.BREW, "Missing abbreviation in %s", key);
                    }
                }
                case itemType -> {
                    if (isPresent(abbreviation)) {
                        itemTypes.put(abbreviation, value);
                    } else {
                        Tui.instance().errorf(Msg.BREW, "Missing abbreviation in %s", key);
                    }
                }
                default -> {
                } // no-op
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
