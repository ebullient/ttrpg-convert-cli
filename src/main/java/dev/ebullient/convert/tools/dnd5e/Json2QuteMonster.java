package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.Tuple;
import dev.ebullient.convert.tools.dnd5e.qute.AbilityScores;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SavesAndSkills;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spellcasting;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spells;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteMonster extends Json2QuteCommon {

    private static final Pattern UPPERCASE_LETTER = Pattern.compile("([A-Z]|\\d+)");
    private static final List<String> LEGENDARY_IGNORE_LIST = List.of("name", "source", "page",
            TtrpgValue.indexInputType.name(), TtrpgValue.indexKey.name(), "_copy", "_meta");

    public static boolean isNpc(JsonNode source) {
        if (source.has("isNpc")) {
            return source.get("isNpc").asBoolean(false);
        }
        if (source.has("isNamedCreature")) {
            return source.get("isNamedCreature").asBoolean(false);
        }
        return false;
    }

    String creatureType;
    String subtype;
    Integer ac;
    String acText;
    Integer hp;
    String hpText;
    String hitDice;
    final boolean isNpc;

    Json2QuteMonster(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        findCreatureType();
        findAc();
        findHp();
        isNpc = isNpc(rootNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        String size = getSize(rootNode);
        String environment = joinAndReplace(rootNode, "environment");
        String cr = monsterCr(rootNode);
        String pb = monsterPb(cr);

        Tags tags = new Tags(getSources());
        tags.add("monster", "size", slugify(size));
        if (subtype == null || subtype.isEmpty()) {
            tags.add("monster", "type", slugify(creatureType));
        } else {
            String sType = slugify(creatureType);
            for (String detail : subtype.split("\\s*,\\s*")) {
                tags.add("monster", "type", sType, slugify(detail));
            }
        }
        if (!environment.isBlank()) {
            for (String env : environment.split("\\s*,\\s*")) {
                tags.add("monster", "environment", slugify(env));
            }
        }

        List<ImageRef> fluffImages = new ArrayList<>();
        String fluff = getFluffDescription(Tools5eIndexType.monsterFluff, "##", fluffImages);
        if (fluff != null && fluff.contains("##")) {
            fluff += "\n\n## Stat Block";
        }

        return new QuteMonster(sources,
                decoratedMonsterName(sources),
                getSourceText(sources),
                isNpc,
                size, creatureType, subtype, monsterAlignment(),
                ac, acText, hp, hpText, hitDice,
                monsterSpeed(), monsterScores(),
                monsterSavesAndSkills(),
                joinAndReplace(rootNode, "senses"),
                intOrDefault(rootNode, "passive", 10),
                monsterImmunities("vulnerable"),
                monsterImmunities("resist"),
                monsterImmunities("immune"),
                monsterImmunities("conditionImmune"),
                joinAndReplace(rootNode, "languages"),
                cr, pb,
                monsterTraits("trait"), monsterTraits("action"),
                monsterTraits("bonus"), monsterTraits("reaction"),
                monsterTraits("legendary"),
                legendaryGroup(),
                monsterSpellcasting(),
                fluff,
                environment,
                new ArrayList<>(sources.getSources()),
                getToken(),
                fluffImages,
                tags,
                cfg().alwaysUseDiceRoller());
    }

    void findCreatureType() {
        JsonNode typeNode = rootNode.get("type");
        if (typeNode == null) {
            tui().warn("Empty type for " + getSources());
            return;
        }
        if (typeNode.isTextual()) {
            creatureType = typeNode.asText();
            return;
        }

        // We have an object: type + tags
        creatureType = typeNode.get("type").asText();
        List<String> tags = new ArrayList<>();
        typeNode.withArray("tags").forEach(tag -> {
            if (tag.isTextual()) {
                tags.add(tag.asText());
            } else {
                tags.add(String.format("%s %s",
                        tag.get("prefix").asText(),
                        tag.get("tag").asText()));
            }
        });
        if (!tags.isEmpty()) {
            subtype = String.join(", ", tags);
        }
    }

    void findAc() {
        JsonNode acNode = rootNode.get("ac");
        if (acNode.isIntegralNumber()) {
            ac = acNode.asInt();
        } else if (acNode.isArray()) {
            List<String> details = new ArrayList<>();
            acNode.forEach(acValue -> {
                if (ac == null && details.isEmpty()) { // first time
                    if (acValue.isIntegralNumber()) {
                        ac = acValue.asInt();
                    } else if (acValue.isObject()) {
                        if (acValue.has("ac")) {
                            ac = acValue.get("ac").asInt();
                        }
                        if (acValue.has("special")) {
                            details.add(acValue.get("special").asText());
                        } else if (acValue.has("from")) {
                            details.add(joinAndReplace(acValue.withArray("from")));
                        }
                    }
                } else { // nth time: conditional AC. Append to acText
                    StringBuilder value = new StringBuilder();
                    value.append(acValue.get("ac").asText());
                    if (acValue.has("from")) {
                        value.append(" from ").append(joinAndReplace(acValue.withArray("from")));
                    }
                    if (acValue.has("condition")) {
                        value.append(" ").append(acValue.get("condition").asText());
                    }
                    details.add(value.toString());
                }
            });
            if (!details.isEmpty()) {
                acText = replaceText(String.join("; ", details));
            }
        } else {
            tui().errorf("Unknown armor class in monster %s: %s", sources.getKey(), acNode.toPrettyString());
        }
    }

    void findHp() {
        JsonNode health = rootNode.get("hp");
        if (health.has("special")) {
            String special = health.get("special").asText();
            if (special.matches("^[\\d\"]+$")) {
                hp = Integer.parseInt(special.replace("\"", ""));
                if (health.has("original")) {
                    hpText = health.get("original").asText();
                }
            } else {
                hpText = replaceText(special);
            }
        } else {
            if (health.has("average")) {
                hp = health.get("average").asInt();
            }
            if (health.has("formula")) {
                hitDice = health.get("formula").asText();
            }
        }

        if (hpText == null && hitDice == null && hp == null) {
            tui().errorf("Unknown hp from %s: %s", getSources(), health.toPrettyString());
            throw new IllegalArgumentException("Unknown hp from " + getSources());
        }
    }

    String monsterSpeed() {
        List<String> speed = new ArrayList<>();
        rootNode.get("speed").fields().forEachRemaining(f -> {
            if (f.getValue().isNumber()) {
                speed.add(String.format("%s %s ft.", f.getKey(), f.getValue().asText()));
            } else if (f.getValue().has("number")) {
                speed.add(String.format("%s %s ft.%s",
                        f.getKey(),
                        f.getValue().get("number").asText(),
                        f.getValue().has("condition")
                                ? " " + f.getValue().get("condition").asText()
                                : ""));
            }
        });
        return replaceText(String.join(", ", speed));
    }

    AbilityScores monsterScores() {
        return new AbilityScores(
                intOrDefault(rootNode, "str", 10),
                intOrDefault(rootNode, "dex", 10),
                intOrDefault(rootNode, "con", 10),
                intOrDefault(rootNode, "int", 10),
                intOrDefault(rootNode, "wis", 10),
                intOrDefault(rootNode, "cha", 10));
    }

    String monsterPb(String cr) {
        if (cr != null) {
            return "+" + crToPb(cr);
        }
        return "+2";
    }

    SavesAndSkills monsterSavesAndSkills() {
        SavesAndSkills savesSkills = new SavesAndSkills();
        savesSkills.saveMap = new HashMap<>();
        savesSkills.saves = getModifiers("save", savesSkills.saveMap);
        savesSkills.skillMap = new HashMap<>();
        savesSkills.skills = getModifiers("skill", savesSkills.skillMap);
        return savesSkills;
    }

    String getModifiers(String field, Map<String, String> values) {
        if (!rootNode.has(field)) {
            return null;
        }
        List<String> text = new ArrayList<>();
        StringBuilder separator = new StringBuilder();
        rootNode.get(field).fields().forEachRemaining(f -> {
            if (f.getKey().equals("other")) {
                f.getValue().forEach(e -> {
                    if (e.has("oneOf")) {
                        List<String> nested = new ArrayList<>();
                        e.get("oneOf").fields()
                                .forEachRemaining(x -> nested.add(getModifier(x.getKey(), x.getValue(), values)));
                        text.add("_One of_ " + String.join(", ", nested));
                        if (separator.length() == 0) {
                            separator.append("; ");
                        }
                    } else {
                        tui().errorf("What is this (from %s): %s", sources.getKey());
                    }
                });
            } else {
                text.add(getModifier(f.getKey(), f.getValue(), values));
            }
        });
        if (separator.length() == 0) {
            separator.append(", ");
        }
        return String.join(separator.toString(), text);
    }

    String getModifier(String key, JsonNode value, Map<String, String> values) {
        String ability = SkillOrAbility.format(key);
        String modifier = replaceText(value.asText());
        values.put(ability, modifier);

        return String.format("%s %s%s", ability,
                value.isInt() && value.asInt() > 0 ? "+" : "", modifier);
    }

    String monsterImmunities(String field) {
        if (rootNode.has(field) && rootNode.get(field).isArray()) {
            List<String> immunities = new ArrayList<>();
            StringBuilder separator = new StringBuilder();
            rootNode.withArray(field).forEach(immunity -> {
                if (immunity.isTextual()) {
                    immunities.add(immunity.asText());
                } else {
                    StringBuilder str = new StringBuilder();
                    str.append(joinAndReplace(immunity, field));
                    if (immunity.has("note")) {
                        str.append(" ")
                                .append(immunity.get("note").asText());
                    }

                    if (separator.length() == 0) {
                        separator.append("; ");
                    }
                    immunities.add(str.toString());
                }
            });
            if (separator.length() == 0) {
                separator.append(", ");
            }
            return String.join(separator.toString(), immunities);
        }
        return null;
    }

    String monsterAlignment() {
        ArrayNode a1 = rootNode.withArray("alignment");
        if (a1.size() == 0) {
            return "Unaligned";
        }
        if (a1.size() == 1 && a1.get(0).has("special")) {
            return a1.get(0).get("special").asText();
        }

        String prefix = MonsterFields.alignmentPrefix.getTextOrDefault(rootNode, "");
        prefix = (prefix.isEmpty() ? "" : prefix + " ");

        String choices = a1.toString();
        if (choices.contains("note")) {
            List<String> notes = new ArrayList<>(List.of(choices.split("},\\{")));
            for (int i = 0; i < notes.size(); i++) {
                int pos = notes.get(i).indexOf("note");
                String alignment = mapAlignmentToString(toAlignmentCharacters(notes.get(i).substring(0, pos)));
                String note = notes.get(i).substring(pos + 4).replaceAll("[^A-Za-z ]+", "");
                notes.set(i, String.format("%s (%s)", alignment, note));
            }
            return prefix + String.join(", ", notes);
        } else {
            choices = toAlignmentCharacters(choices);
            return prefix + mapAlignmentToString(choices);
        }
    }

    String toAlignmentCharacters(String src) {
        return src.replaceAll("\"[A-Z]*[a-z ]+\"", "") // remove notes
                .replaceAll("[^LCNEGAUXY]", ""); // keep only alignment characters
    }

    List<Spellcasting> monsterSpellcasting() {
        JsonNode array = rootNode.get("spellcasting");
        if (array == null || array.isNull()) {
            return null;
        } else if (array.isObject()) {
            tui().errorf("Unknown spellcasting for %s: %s", sources.getKey(), array.toPrettyString());
            throw new IllegalArgumentException("Unknown spellcasting: " + getSources());
        }

        List<Spellcasting> casting = new ArrayList<>();
        array.forEach(scNode -> {
            Spellcasting spellcasting = new Spellcasting();
            spellcasting.name = SourceField.name.replaceTextFrom(scNode, this);

            spellcasting.headerEntries = new ArrayList<>();
            appendToText(spellcasting.headerEntries, scNode.get("headerEntries"), null);

            spellcasting.footerEntries = new ArrayList<>();
            appendToText(spellcasting.footerEntries, scNode.get("footerEntries"), null);

            if (scNode.has("will")) {
                spellcasting.will = getSpells(scNode.get("will"));
            }
            if (scNode.has("daily")) {
                spellcasting.daily = new TreeMap<>();
                scNode.get("daily").fields()
                        .forEachRemaining(f -> spellcasting.daily.put(f.getKey(), getSpells(f.getValue())));
            }
            if (scNode.has("spells")) {
                spellcasting.spells = new TreeMap<>();
                scNode.get("spells").fields().forEachRemaining(f -> {
                    JsonNode spellNode = f.getValue();
                    Spells spells = new Spells();
                    if (spellNode.isArray()) {
                        spells.spells = getSpells(spellNode);
                    } else {
                        spells.slots = intOrDefault(spellNode, "slots", 0);
                        spells.lowerBound = intOrDefault(spellNode, "lower", 0);
                        spells.spells = getSpells(spellNode.get("spells"));
                    }
                    spellcasting.spells.put(f.getKey(), spells);
                });
            }
            casting.add(spellcasting);
        });
        return casting;
    }

    List<String> getSpells(JsonNode source) {
        if (source == null) {
            tui().errorf("Null spells from %s", sources.getKey());
            return List.of();
        }
        List<String> spells = new ArrayList<>();
        source.forEach(s -> spells.add(replaceText(s.asText())));
        return spells;
    }

    Collection<NamedText> legendaryGroup() {
        JsonNode group = rootNode.get("legendaryGroup");
        if (group == null) {
            return null;
        }
        String key = Tools5eIndexType.legendaryGroup.createKey(group);
        if (!index.sourceIncluded(group.get("source").asText())) {
            tui().debugf("Legendary group %s source excluded", key);
        }
        JsonNode content = index.getOrigin(key);
        if (content == null) {
            tui().debugf("No legendary group content for %s", key);
            return null;
        }

        List<NamedText> traits = new ArrayList<>();
        for (Entry<String, JsonNode> field : iterableFields(content)) {
            String fieldName = field.getKey();
            if (LEGENDARY_IGNORE_LIST.contains(fieldName)) {
                continue;
            }
            fieldName = fieldName.substring(0, 1).toUpperCase()
                    + UPPERCASE_LETTER.matcher(fieldName.substring(1))
                            .replaceAll(matchResult -> " " + (matchResult.group(1).toLowerCase()));

            List<String> text = new ArrayList<>();
            appendToText(text, field.getValue(), null);
            traits.add(new NamedText(fieldName, String.join("\n", text)));
        }
        return traits;
    }

    Collection<NamedText> monsterTraits(String field) {
        JsonNode array = rootNode.get(field);
        if (array == null || array.isNull()) {
            return null;
        } else if (array.isObject()) {
            tui().errorf("Unknown %s for %s: %s", field, sources.getKey(), array.toPrettyString());
            throw new IllegalArgumentException("Unknown field: " + getSources());
        }

        List<NamedText> traits = new ArrayList<>();
        for (JsonNode e : iterableElements(array)) {
            String name = SourceField.name.replaceTextFrom(e, this)
                    .replaceAll(":$", "");

            List<String> text = new ArrayList<>();
            appendToText(text, SourceField.entry.getFrom(e), null);
            appendToText(text, SourceField.entries.getFrom(e), null);

            String body = String.join("\n", text);
            if (body.startsWith(">")) {
                body = "\n" + body;
            }
            traits.add(new NamedText(name, body));
        }
        return traits;
    }

    ImageRef getToken() {
        String tokenString = MonsterFields.tokenUrl.getTextOrNull(rootNode);
        if (booleanOrDefault(rootNode, "hasToken", false)) {
            // const imgLink = Renderer.monster.getTokenUrl(mon);
            // return mon.tokenUrl || UrlUtil.link(`${Renderer.get().baseMediaUrls["img"] || Renderer.get().baseUrl}img/${Parser.sourceJsonToAbv(mon.source)}/${Parser.nameToTokenName(mon.name)}.png`);
            // nameToTokenName = function (name) { return name.toAscii().replace(/"/g, ""); }

            // origin is set by conjured monster variant (below)
            String name = getTextOrDefault(rootNode, "original", getName());
            String filename = Normalizer.normalize(name, Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replace("Æ", "AE")
                    .replace("æ", "ae")
                    .replace("\"", "");

            Path sourcePath = Path.of("img",
                    getSources().mapPrimarySource(),
                    filename + ".png");

            Path target = Path.of(getImagePath(),
                    "token",
                    slugify(filename) + ".png");

            return buildImageRef(sourcePath, target);
        } else if (tokenString != null) {
            return getSources().buildImageRef(null, tokenString);
        }
        return null;
    }

    @Override
    public String getImagePath() {
        return Tools5eQuteBase.monsterPath(isNpc, creatureType);
    }

    public static List<Tuple> findConjuredMonsterVariants(Tools5eIndex index, Tools5eIndexType type,
            String key, JsonNode jsonSource) {
        final Pattern variantPattern = Pattern.compile("(\\d+) \\((.*?)\\)");
        int startLevel = jsonSource.get("summonedBySpellLevel").asInt();

        String name = jsonSource.get("name").asText();
        String hpString = jsonSource.get("hp").get("special").asText();
        String acString = jsonSource.get("ac").get(0).get("special").asText();

        List<Tuple> variants = new ArrayList<>();
        for (int i = startLevel; i < 10; i++) {
            if (hpString.contains(" or ")) {
                // "50 (Demon only) or 40 (Devil only) or 60 (Yugoloth only) + 15 for each spell
                // level above 6th"
                // "30 (Ghostly and Putrid only) or 20 (Skeletal only) + 10 for each spell level
                // above 3rd"
                String[] parts = hpString.split(" \\+ ");
                String[] variantGroups = parts[0].split(" or ");
                for (String group : variantGroups) {
                    Matcher m = variantPattern.matcher(group);
                    if (m.find()) {
                        String amount = m.group(1);
                        String variant = m.group(2);

                        if (variant.contains(" and ")) {
                            for (String v : variant.split(" and ")) {
                                String variantName = String.format("%s (%s, %s-Level Spell)",
                                        name, v.replace(" only", ""), JsonSource.levelToString(i));
                                createVariant(index, variants, jsonSource, type, variantName, i,
                                        amount + " + " + parts[1], acString);
                            }
                        } else {
                            String variantName = String.format("%s (%s, %s-Level Spell)",
                                    name, variant.replace(" only", ""), JsonSource.levelToString(i));
                            createVariant(index, variants, jsonSource, type, variantName, i,
                                    amount + " + " + parts[1], acString);
                        }
                    } else {
                        index.tui().errorf("Unknown HP variant from %s: %s", key, hpString);
                    }
                }
            } else {
                String variantName = String.format("%s (%s-level Spell)", name, JsonSource.levelToString(i));
                createVariant(index, variants, jsonSource, type, variantName, i, hpString, acString);
            }
        }
        return variants;
    }

    static void createVariant(Tools5eIndex index, List<Tuple> variants,
            JsonNode jsonSource, Tools5eIndexType type,
            String variantName, int level, String hpString, String acString) {

        ConjuredMonster fixed = new ConjuredMonster(level, variantName, hpString, acString, jsonSource);

        ObjectNode adjustedSource = (ObjectNode) index.copyNode(jsonSource);
        adjustedSource.set("original", adjustedSource.get("name"));
        adjustedSource.replace("name", fixed.getName());
        adjustedSource.replace("ac", fixed.getAc());
        adjustedSource.replace("hp", fixed.getHp());

        String newKey = type.createKey(adjustedSource);
        variants.add(new Tuple(newKey, adjustedSource));
    }

    public static class ConjuredMonster {
        final String name;
        final MonsterAC monsterAc;
        final MonsterHp monsterHp;

        public ConjuredMonster(int level, String name, String hpString, String acString, JsonNode jsonSource) {
            this.name = name;
            this.monsterAc = new MonsterAC(level, acString);
            this.monsterHp = new MonsterHp(level, hpString, jsonSource);
        }

        public JsonNode getName() {
            return new TextNode(name);
        }

        public JsonNode getAc() {
            MonsterAC[] result = new MonsterAC[] { monsterAc };
            return Tui.MAPPER.valueToTree(result);
        }

        public JsonNode getHp() {
            return Tui.MAPPER.valueToTree(monsterHp);
        }
    }

    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class MonsterAC {
        public final int ac;
        public final String[] from;
        public final String original;

        // "ac": [
        // {
        // "ac": 19,
        // "from": [
        // "natural armor"
        // ]
        // }
        // ],
        public MonsterAC(int level, String acString) {
            this.original = acString;
            if (acString.contains(" + ")) {
                String[] parts = acString.split(" \\+ ");
                int value = Integer.parseInt(parts[0]);
                String armor = null;
                // "11 + the level of the spell (natural armor)"
                // "13 + PB (natural armor)"
                if (parts[1].contains("the level of the spell")) {
                    value += level;
                    armor = parts[1]
                            .replace("the level of the spell", "")
                            .replace("(", "")
                            .replace(")", "")
                            .trim();
                } else if (parts[1].contains("PB")) {
                    armor = parts[1]
                            .replace("PB", "")
                            .replace("(", "")
                            .replace(")", "")
                            .trim()
                            + " + caster proficiency bonus";
                }
                this.ac = value;
                this.from = armor == null ? null : new String[] { armor };
            } else {
                throw new IllegalArgumentException("Unknown AC pattern: " + acString);
            }
        }
    }

    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MonsterHp {
        static final Pattern hpPattern = Pattern
                .compile("(\\d+) \\+ (\\d+) for each spell level above (\\d)(nd|rd|th|st) ?\\(?(.*?)?\\)?");

        public final String special;
        public final String original;

        // Want to go from:
        // "40 + 10 for each spell level above 4th"
        // "50 + 10 for each spell level above 5th (the dragon has a number of Hit Dice
        // [d10s] equal to the level of the spell)"
        // TO:
        // "hp": {
        // "average": 195,
        // "formula": "17d12 + 85"
        // },
        // OR
        // "hp": {
        // "special": "195",
        // },
        public MonsterHp(int level, String hpString, JsonNode jsonSource) {
            this.original = hpString;

            Integer value = null;
            if (hpString.contains("Constitution modifier")) {
                // "equal the undead's Constitution modifier + your spellcasting ability
                // modifier + ten times the spell's level"
                int con = jsonSource.get("con").asInt();
                value = con + (10 * level);
            } else if (hpString.contains("half the hit point maximum of its summoner")) {
                // nothing we can do
            } else if (hpString.contains(" + ")) {
                Matcher m = hpPattern.matcher(hpString);
                if (m.find()) {
                    value = Integer.parseInt(m.group(1));
                    int scale = level - Integer.parseInt(m.group(3));
                    value += Integer.parseInt(m.group(2)) * scale;
                } else {
                    throw new IllegalArgumentException("Unknown HP pattern: " + hpString);
                }
            } else {
                throw new IllegalArgumentException("Unknown HP pattern: " + hpString);
            }

            this.special = value == null ? "" : value + "";
        }
    }

    enum MonsterFields implements JsonNodeReader {
        alignment,
        alignmentPrefix,
        tokenUrl,
    }
}
