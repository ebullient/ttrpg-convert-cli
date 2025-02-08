package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.joinConjunct;
import static dev.ebullient.convert.StringUtil.toOrdinal;

import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.Json2QuteMonster.MonsterFields;
import dev.ebullient.convert.tools.dnd5e.qute.AbilityScores;
import dev.ebullient.convert.tools.dnd5e.qute.AcHp;
import dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteCommon implements JsonSource {
    static final Pattern featPattern = Pattern.compile("([^|]+)\\|?.*");
    static final List<String> SPEED_MODE = List.of("walk", "burrow", "climb", "fly", "swim");
    static final List<String> specialTraits = List.of("special equipment", "shapechanger");
    static final Map<String, String> SCF_TYPE_TO_NAME = Map.of(
            "arcane", "Arcane Focus",
            "druid", "Druidic Focus",
            "holy", "Holy Symbol");

    static final Comparator<Entry<String, List<String>>> compareNumberStrings = Comparator
            .comparingInt(e -> Integer.parseInt(e.getKey()));

    protected final Tools5eIndex index;
    protected final Tools5eSources sources;
    protected final Tools5eIndexType type;
    protected final JsonNode rootNode;
    protected String imagePath = null;

    Json2QuteCommon(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        this.index = index;
        this.rootNode = jsonNode;
        this.type = type;
        this.sources = type.multiNode() ? null : Tools5eSources.findOrTemporary(jsonNode);
    }

    public Json2QuteCommon withImagePath(String imagePath) {
        this.imagePath = imagePath;
        return this;
    }

    public String getName() {
        return this.sources.getName();
    }

    @Override
    public Tools5eSources getSources() {
        return sources;
    }

    @Override
    public Tools5eIndex index() {
        return index;
    }

    @Override
    public String getImagePath() {
        if (imagePath != null) {
            return imagePath;
        }
        return JsonSource.super.getImagePath();
    }

    public String getText(String heading) {
        List<String> text = new ArrayList<>();
        appendToText(text, SourceField.entries.getFrom(rootNode), heading);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public String getFluffDescription(Tools5eIndexType fluffType, String heading, List<ImageRef> images) {
        return getFluffDescription(rootNode, fluffType, heading, images);
    }

    public String getFluffDescription(JsonNode fromNode, Tools5eIndexType fluffType, String heading, List<ImageRef> images) {
        List<String> text = getFluff(fromNode, fluffType, heading, images);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public List<String> getFluff(Tools5eIndexType fluffType, String heading, List<ImageRef> images) {
        return getFluff(rootNode, fluffType, heading, images);
    }

    public List<String> getFluff(JsonNode fromNode, Tools5eIndexType fluffType, String heading, List<ImageRef> images) {
        List<String> text = new ArrayList<>();
        JsonNode fluffNode = null;
        if (TtrpgValue.indexFluffKey.existsIn(fromNode)) {
            // Specific variant
            String fluffKey = TtrpgValue.indexFluffKey.getTextOrEmpty(fromNode);
            fluffNode = index.getOrigin(fluffKey);
        } else if (Tools5eFields.fluff.existsIn(fromNode)) {
            fluffNode = Tools5eFields.fluff.getFrom(fromNode);
            JsonNode monsterFluff = Tools5eFields._monsterFluff.getFrom(fluffNode);
            if (monsterFluff != null) {
                String fluffKey = fluffType.createKey(monsterFluff);
                fluffNode = index.getOrigin(fluffKey);
            }
        } else if (Tools5eFields.hasFluff.booleanOrDefault(fromNode, false)
                || Tools5eFields.hasFluffImages.booleanOrDefault(fromNode, false)) {
            String fluffKey = fluffType.createKey(fromNode);
            fluffNode = index.getOrigin(fluffKey);
        }

        if (fluffNode != null) {
            unpackFluffNode(fluffType, fluffNode, text, heading, images);
        }
        return text;
    }

    public void unpackFluffNode(Tools5eIndexType fluffType, JsonNode fluffNode, List<String> text, String heading,
            List<ImageRef> images) {

        boolean pushed = parseState().push(getSources(), fluffNode);
        try {
            if (fluffNode.isArray()) {
                appendToText(text, fluffNode, heading);
            } else {
                appendToText(text, SourceField.entries.getFrom(fluffNode), heading);
            }
        } finally {
            parseState().pop(pushed);
        }

        if (Tools5eFields.images.existsIn(fluffNode)) {
            getImages(Tools5eFields.images.getFrom(fluffNode), images);
        } else if (Tools5eFields.hasFluffImages.booleanOrDefault(fluffNode, false)) {
            String fluffKey = fluffType.createKey(fluffNode);
            fluffNode = index.getOrigin(fluffKey);
            if (fluffNode != null) {
                getImages(Tools5eFields.images.getFrom(fluffNode), images);
            }
        }
    }

    public List<ImageRef> getFluffImages(Tools5eIndexType fluffType) {
        List<ImageRef> images = new ArrayList<>();
        if (Tools5eFields.hasFluffImages.booleanOrDefault(rootNode, false)) {
            String fluffKey = fluffType.createKey(rootNode);
            JsonNode fluffNode = index.getOrigin(fluffKey);
            if (fluffNode != null) {
                getImages(Tools5eFields.images.getFrom(fluffNode), images);
            }
        }
        return images;
    }

    public void getImages(JsonNode imageNode, List<ImageRef> images) {
        if (imageNode != null && imageNode.isArray()) {
            for (Iterator<JsonNode> i = imageNode.elements(); i.hasNext();) {
                ImageRef ir = readImageRef(i.next());
                if (ir != null) {
                    images.add(ir);
                }
            }
        }
    }

    // {"ability":[{"dex":13}]}
    private String abilityPrereq(JsonNode abilityPrereq) {
        ArrayNode elements = ensureArray(abilityPrereq);

        boolean multipleInner = false;
        boolean multiMultipleInner = false;
        JsonNode allValuesEqual = null;

        // See if all of the abilities have the same value
        outer: for (JsonNode abMetaNode : elements) {
            ObjectNode objectNode = (ObjectNode) abMetaNode;

            for (JsonNode valueNode : objectNode) {
                if (allValuesEqual == null) {
                    allValuesEqual = valueNode;
                } else {
                    var ave = allValuesEqual;
                    boolean allMatch = StreamSupport.stream(objectNode.spliterator(), false)
                            .allMatch(node -> node.equals(ave));
                    if (!allMatch) {
                        allValuesEqual = null;
                        break outer;
                    }
                }
            }
        }

        List<String> abilityOptions = new ArrayList<>();
        for (JsonNode abMetaNode : elements) {
            if (allValuesEqual != null) {
                List<String> options = new ArrayList<>();
                multipleInner |= abMetaNode.size() > 1;
                abMetaNode.fieldNames().forEachRemaining(x -> {
                    options.add(SkillOrAbility.format(x, index(), getSources()));
                });
                abilityOptions.add(joinConjunct(" and ", options));
            } else {
                Map<String, List<String>> groups = new HashMap<>();
                for (Entry<String, JsonNode> score : iterableFields(abMetaNode)) {
                    groups.computeIfAbsent(score.getValue().asText(), k -> new ArrayList<>())
                            .add(SkillOrAbility.format(score.getKey(), index(), sources));
                }

                boolean isMulti = groups.values().stream().anyMatch(x -> x.size() > 1);
                multiMultipleInner |= isMulti;
                multipleInner |= isMulti;

                List<String> byScore = groups.entrySet().stream()
                        .sorted((a, b) -> compareNumberStrings.compare(b, a))
                        .map(e -> {
                            List<String> abs = e.getValue().stream()
                                    .map(x -> index().findSkillOrAbility(x, sources))
                                    .sorted(SkillOrAbility.comparator)
                                    .map(x -> x.value())
                                    .toList();
                            return String.format("%s %s or higher",
                                    joinConjunct(" and ", abs),
                                    e.getKey());
                        })
                        .toList();

                abilityOptions.add(isMulti
                        ? joinConjunct("; ", " and ", byScore)
                        : joinConjunct(" and ", byScore));
            }
        }

        var isComplex = multipleInner || multiMultipleInner || allValuesEqual == null;
        String joined = joinConjunct(
                multiMultipleInner ? " - " : multipleInner ? "; " : ", ",
                isComplex ? " OR " : " or ",
                abilityOptions);

        return joined + (allValuesEqual != null
                ? " " + allValuesEqual.asText() + " or higher"
                : "");
    }

    // {"name":"Rune Carver","displayEntry":"{@background Rune Carver|BGG}"}]
    private String backgroundPrereq(JsonNode backgroundPrereq) {
        List<String> backgrounds = new ArrayList<>();
        for (JsonNode p : iterableElements(backgroundPrereq)) {
            JsonNode displayEntry = PrereqFields.displayEntry.getFrom(p);
            if (displayEntry != null) {
                backgrounds.add(replaceText(displayEntry.asText()));
            } else {
                String name = SourceField.name.getTextOrEmpty(p);
                backgrounds.add(index.linkifyByName(Tools5eIndexType.background, name));
            }
        }
        return joinConjunct(" or ", backgrounds);
    }

    private String replaceConjoinOr(JsonNode campaignPrereq, String suffix) {
        List<String> cmpn = new ArrayList<>();
        for (JsonNode p : iterableElements(campaignPrereq)) {
            replaceText(p.asText());
        }
        return joinConjunct(" or ", cmpn) + suffix;
    }

    private String expertisePrereq(JsonNode expertisePrereq) {
        // "prerequisite": [
        //     {
        //         "expertise": [
        //             {
        //                 "skill": true
        //             }
        //         ]
        //     }
        // ],
        List<String> expertise = new ArrayList<>();
        for (JsonNode p : iterableElements(expertisePrereq)) {
            for (Entry<String, JsonNode> prof : iterableFields(p)) {
                switch (prof.getKey()) {
                    case "skill" -> {
                        if (prof.getValue().asBoolean()) {
                            expertise.add("Expertise in a skill");
                        } else {
                            tui().warnf(Msg.UNKNOWN, "unknown expertise prereq value %s from %s / %s",
                                    p.toString(), getSources().getKey(), parseState().getSource());
                        }
                    }
                    default -> {
                        tui().warnf(Msg.UNKNOWN, "unknown expertise prereq type %s from %s / %s",
                                p.toString(), getSources().getKey(), parseState().getSource());
                    }
                }
            }
        }
        return joinConjunct(" or ", expertise);
    }

    // "scion of the outer planes|ua2022wondersofthemultiverse|scion of the outer planes (good outer plane)"
    // "scion of the outer planes|sato|scion of the outer planes (good outer plane)"
    private String featPrereq(JsonNode featPrereq) {
        List<String> feats = new ArrayList<>();
        for (JsonNode p : iterableElements(featPrereq)) {
            replaceText(String.format("{@feat %s} feat", p.asText()));
        }
        return joinConjunct(" or ", feats);
    }

    private String itemTypePrereq(JsonNode itemTypePrereq) {
        List<String> types = new ArrayList<>();
        for (JsonNode p : iterableElements(itemTypePrereq)) {
            ItemType type = index.findItemType(p.asText(), getSources());
            if (type != null) {
                types.add(type.linkify());
            } else {
                tui().warnf(Msg.UNKNOWN, "unknown item type prereq %s from %s / %s",
                        p.asText(), getSources().getKey(), parseState().getSource());
            }
        }
        return joinConjunct(" and ", types);
    }

    private String itemPropertyPrereq(JsonNode itemPropertyPrereq) {
        List<String> props = new ArrayList<>();
        for (JsonNode p : iterableElements(itemPropertyPrereq)) {
            ItemProperty prop = index.findItemProperty(p.asText(), getSources());
            if (prop != null) {
                props.add(prop.linkify());
            } else {
                tui().warnf(Msg.UNKNOWN, "unknown item property prereq %s from %s / %s",
                        p.asText(), getSources().getKey(), parseState().getSource());
            }
        }
        return joinConjunct(" and ", props);
    }

    // "level":4
    // "level":{"level":1,"class":{"name":"Fighter","visible":true}}}
    private String levelPrereq(JsonNode levelPrereq) {
        if (levelPrereq.isArray())
            tui().errorf("levelPrereq: Array parameter");

        if (levelPrereq.isNumber()) {
            return toOrdinal(levelPrereq.asInt());
        }

        String level = Tools5eFields.level.getTextOrThrow(levelPrereq);
        JsonNode classNode = SourceField._class_.getFrom(levelPrereq);
        JsonNode subclassNode = Tools5eFields.subclass.getFrom(levelPrereq);

        // neither class nor subclass is defined
        if (classNode == null && subclassNode == null) {
            return toOrdinal(level);
        }

        boolean isLevelVisible = !"1".equals(level); // hide implied first level
        boolean isSubclassVisible = Tools5eFields.visible.booleanOrDefault(subclassNode, false);
        boolean isClassVisible = classNode != null
                && (isSubclassVisible || Tools5eFields.visible.booleanOrDefault(classNode, false));

        String classPart = "";
        if (isClassVisible && isSubclassVisible) {
            classPart = String.format("%s (%s)",
                    SourceField.name.getTextOrEmpty(classNode),
                    SourceField.name.getTextOrEmpty(subclassNode));
        } else if (isClassVisible) {
            classPart = SourceField.name.getTextOrEmpty(classNode);
        } else if (isSubclassVisible) {
            tui().warnf("Subclass %s without class in %s", subclassNode, levelPrereq);
        }

        return String.format("%s%s",
                isLevelVisible ? toOrdinal(level) : "",
                isClassVisible ? " " + classPart : "");
    }

    // {"proficiency":[{"armor":"medium"}]}
    // {"proficiency":[{"weaponGroup":"martial"}]}
    private String proficiencyPrereq(JsonNode profPrereq) {
        List<String> profs = new ArrayList<>();
        for (JsonNode p : iterableElements(profPrereq)) {
            for (Entry<String, JsonNode> prof : iterableFields(p)) {
                switch (prof.getKey()) {
                    case "armor" -> profs.add(String.format("%s armor",
                            replaceText(prof.getValue().asText())));
                    case "weapon" -> profs.add(String.format("a %s weapon",
                            replaceText(prof.getValue().asText())));
                    case "weaponGroup" -> profs.add(String.format("%s weapons",
                            replaceText(prof.getValue().asText())));
                    default -> {
                        tui().warnf(Msg.UNKNOWN, "unknown proficiency prereq %s from %s / %s",
                                p.toString(), getSources().getKey(), parseState().getSource());
                    }
                }
            }
        }
        return String.format("Proficiency with %s", joinConjunct(" or ", profs));
    }

    // [{"name":"elf"}]
    // [{"name":"half-elf"}]
    // [{"name":"small race","displayEntry":"a Small race"}]
    private String racePrereq(JsonNode racePrereq) {
        List<String> races = new ArrayList<>();
        for (JsonNode p : iterableElements(racePrereq)) {
            JsonNode displayEntry = PrereqFields.displayEntry.getFrom(p);
            if (displayEntry != null) {
                races.add(replaceText(displayEntry.asText()));
            } else {
                String name = SourceField.name.getTextOrEmpty(p);
                String subraceName = Tools5eFields.subrace.getTextOrNull(p);
                races.add(index.linkifyByName(Tools5eIndexType.race, Json2QuteRace.getSubraceName(name, subraceName)));
            }
        }
        return joinConjunct(" or ", races);
    }

    private String scfPrereq(JsonNode scfPrereq) {
        if (scfPrereq.isBoolean()) {
            return replaceText("Ability to use a {@variantrule Spellcasting Focus|XPHB}");
        }

        List<String> scfTypes = new ArrayList<>();
        for (JsonNode p : iterableElements(scfPrereq)) {
            String type = p.asText();
            String name = SCF_TYPE_TO_NAME.getOrDefault(type, type);
            String article = scfTypes.isEmpty()
                    ? articleFor(name) + " "
                    : "";

            if (!name.equals(type)) {
                name = replaceText("{@item " + name + "|XPHB}");
            }

            scfTypes.add("%s%s".formatted(article, name));
        }

        return replaceText("Ability to use %s as a {@variantrule Spellcasting Focus|XPHB}"
                .formatted(joinConjunct(" or ", scfTypes)));
    }

    private List<String> testBoolean(JsonNode node, String valueIfTrue) {
        return node.booleanValue()
                ? List.of(valueIfTrue)
                : List.of();
    }

    private String spellPrereq(JsonNode spellPrereq) {
        List<String> spells = new ArrayList<>();
        for (JsonNode p : iterableElements(spellPrereq)) {
            if (p.isTextual()) {
                String[] split = p.asText().split("#");
                if (split.length == 1) {
                    spells.add(replaceText(String.format("{@spell %s}", split[0])));
                } else if ("c".equals(split[1])) {
                    spells.add(replaceText(String.format("{@spell %s} cantrip", split[0])));
                } else if ("x".equals(split[1])) {
                    spells.add(replaceText(String.format("{@spell hex} spell or a warlock feature that curses", split[0])));
                } else {
                    tui().warnf(Msg.UNKNOWN, "unknown spell prereq %s from %s / %s",
                            p.toString(), getSources().getKey(), parseState().getSource());
                }
            } else {
                spells.add(replaceText(String.format("{@filter %s|spells|%s}",
                        SourceField.entry.getTextOrEmpty(p),
                        PrereqFields.choose.getTextOrEmpty(p))));
            }
        }
        return joinConjunct(" or ", spells);
    }

    private ObjectNode sharedPrerequisites(ArrayNode prerequisites) {
        ObjectNode shared = prerequisites.objectNode();

        if (prerequisites.size() > 1) {
            List<JsonNode> others = streamOf(prerequisites).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            // slice(1)
            JsonNode first = others.get(0);
            others.remove(0);

            others.stream()
                    .reduce(first,
                            (a, b) -> objectIntersect(a, b),
                            (a, b) -> ((ObjectNode) a).setAll((ObjectNode) b));
        }
        return shared;
    }

    String listPrerequisites(JsonNode variantNode) {
        List<String> allValues = new ArrayList<>();
        boolean hasNote = false;

        ArrayNode prerequisites = PrereqFields.prerequisite.readArrayFrom(variantNode);

        // find shared/common prereqs
        ObjectNode prereqsShared = sharedPrerequisites(prerequisites);
        String sharedText = prereqsShared.size() > 0
                ? listPrerequisites(prereqsShared)
                : null;

        for (JsonNode prerequisite : prerequisites) {
            List<String> values = new ArrayList<>();
            String note = null;

            List<PrereqFields> fields = streamOfFieldNames(prerequisite)
                    .map(x -> {
                        PrereqFields field = fromString(x);
                        if (field == PrereqFields.unknown) {
                            tui().warnf(Msg.UNKNOWN, "Unexpected prerequisite %s (from %s / %s)",
                                    x, prerequisite, getSources().getKey());
                        }
                        return field;
                    })
                    .sorted()
                    .toList();

            for (PrereqFields field : fields) {
                if (prereqsShared.has(field.nodeName())) {
                    continue;
                }
                JsonNode value = field.getFrom(prerequisite);

                switch (field) {
                    case ability -> values.add(abilityPrereq(value));
                    case alignment -> values.add(alignmentListToFull(value));
                    case background -> values.add(backgroundPrereq(value));
                    case campaign -> values.add(replaceConjoinOr(value, " Campaign"));
                    case culture -> values.add(replaceConjoinOr(value, " Culture"));
                    case expertise -> values.add(expertisePrereq(value));
                    case feat -> values.add(featPrereq(value));
                    case feature -> values.add(replaceConjoinOr(value, ""));
                    case optionalfeature -> values.add(replaceConjoinOr(value, ""));
                    case group -> values.add(replaceConjoinOr(value, " Group"));
                    case item -> values.add(replaceConjoinOr(value, ""));
                    case itemProperty -> values.add(itemPropertyPrereq(value));
                    case itemType -> values.add(itemTypePrereq(value));
                    case level -> values.add(levelPrereq(value));
                    case other -> values.add(replaceText(value));
                    case otherSummary -> values.add(SourceField.entry.replaceTextFrom(value, this));
                    case pact -> values.add("Pact of the " + replaceText(value));
                    case patron -> values.add(replaceText(value + " Patron"));
                    case proficiency -> values.add(proficiencyPrereq(value));
                    case race -> values.add(racePrereq(value));
                    case spell -> values.add(spellPrereq(value));
                    case spellcastingFocus -> values.add(scfPrereq(value));
                    // --- Boolean values ----
                    case psionics -> values.addAll(testBoolean(value,
                            replaceText("Psionic Talent feature or {@feat Wild Talent|UA2020PsionicOptionsRevisited} feat")));
                    case spellcasting -> values.addAll(testBoolean(value,
                            "The ability to cast at least one spell"));
                    case spellcasting2020 -> values.addAll(testBoolean(value,
                            "Spellcasting or Pact Magic feature"));
                    case spellcastingFeature -> values.addAll(testBoolean(value,
                            "Spellcasting feature"));
                    case spellcastingPrepared -> values.addAll(testBoolean(value,
                            "Spellcasting feature from a class that prepares spells"));
                    // --- Other: Note ----
                    case note -> note = replaceText(value);
                    default -> {
                        tui().debugf(Msg.UNKNOWN, "Unexpected prerequisite %s (from %s)", field.nodeName(), prerequisite);
                    }
                }
            }

            // remove empty values
            values = values.stream().filter(StringUtil::isPresent).toList();

            hasNote |= isPresent(note);
            String prereqs = String.join(
                    values.stream().anyMatch(x -> x.contains(" or ")) ? "; " : ", ",
                    values);
            allValues.add(prereqs + (isPresent(note) ? ". " + note : ""));
        }

        String joinedText = hasNote
                ? String.join(" Or, ", allValues)
                : joinConjunct(allValues.stream().anyMatch(x -> x.contains(" or ")) ? "; " : ", ",
                        " or ", allValues);

        return sharedText == null
                ? joinedText
                : sharedText + ", plus " + joinedText;

    }

    ImmuneResist immuneResist() {
        return new ImmuneResist(
                collectImmunities(rootNode, VulnerabilityFields.vulnerable),
                collectImmunities(rootNode, VulnerabilityFields.resist),
                collectImmunities(rootNode, VulnerabilityFields.immune),
                collectImmunities(rootNode, VulnerabilityFields.conditionImmune));
    }

    AbilityScores abilityScores() {
        if (!rootNode.has("str")
                && !rootNode.has("dex")
                && !rootNode.has("con")
                && !rootNode.has("int")
                && !rootNode.has("wis")
                && !rootNode.has("cha")) {
            return AbilityScores.DEFAULT;
        }
        return new AbilityScores(
                intOrDefault(rootNode, "str", 10),
                intOrDefault(rootNode, "dex", 10),
                intOrDefault(rootNode, "con", 10),
                intOrDefault(rootNode, "int", 10),
                intOrDefault(rootNode, "wis", 10),
                intOrDefault(rootNode, "cha", 10));
    }

    String speed(JsonNode speedNode) {
        return speed(speedNode, true);
    }

    String speed(JsonNode speedNode, boolean includeZeroWalk) {
        if (speedNode == null) {
            return null;
        } else if (speedNode.isNumber()) {
            return String.format("%s ft.", speedNode.asText());
        }
        JsonNode alternate = Tools5eFields.alternate.getFrom(speedNode);
        String note = SourceField.note.replaceTextFrom(speedNode, this);

        List<String> speed = new ArrayList<>();
        for (String k : SPEED_MODE) {
            JsonNode v = speedNode.get(k);
            JsonNode altV = alternate == null ? null : alternate.get(k);
            if (v != null) {
                String prefix = "walk".equals(k) ? "" : k + " ";
                speed.add(prefix + speedValue(k, v, includeZeroWalk));
                if (altV != null && altV.isArray()) {
                    altV.forEach(x -> speed.add(prefix + speedValue(k, x, includeZeroWalk)));
                }
            }
        }
        return replaceText(String.join(", ", speed)
                + (note.isBlank() ? "" : " " + note));
    }

    String speedValue(String key, JsonNode speedValue, boolean includeZeroWalk) {
        if (speedValue == null || speedValue.isNull()) {
            if (includeZeroWalk && "walk".equals(key)) {
                return "0 ft.";
            }
            return "";
        } else if (speedValue.isBoolean() && !"walk".equals(key)) {
            return "equal to walking speed";
        } else if (speedValue.isNumber()) {
            return String.format("%s ft.", speedValue.asText());
        } else if (speedValue.isTextual()) { // Varies
            return speedValue.asText();
        }

        int number = Tools5eFields.number.intOrDefault(speedValue, 0);
        if (!includeZeroWalk && number == 0 && "walk".equals(key)) {
            return "";
        }
        String condition = Tools5eFields.condition.replaceTextFrom(speedValue, this);
        return String.format("%s ft.%s", number,
                condition.isEmpty() ? "" : " " + condition);
    }

    void findAc(AcHp acHp) {
        JsonNode acNode = MonsterFields.ac.getFrom(rootNode);
        if (acNode == null) {
            return;
        }
        if (acNode.isIntegralNumber()) {
            acHp.ac = acNode.asInt();
        } else if (acNode.isArray()) {
            List<String> details = new ArrayList<>();
            for (JsonNode acValue : iterableElements(acNode)) {
                if (acHp.ac == null && details.isEmpty()) { // first time
                    if (acValue.isIntegralNumber()) {
                        acHp.ac = acValue.asInt();
                    } else if (acValue.isObject()) {
                        if (MonsterFields.ac.existsIn(acValue)) {
                            acHp.ac = MonsterFields.ac.getFrom(acValue).asInt();
                        }
                        if (MonsterFields.special.existsIn(acValue)) {
                            details.add(MonsterFields.special.replaceTextFrom(acValue, this));
                        } else if (MonsterFields.from.existsIn(acValue)) {
                            details.add(joinAndReplace(MonsterFields.from.readArrayFrom(acValue)));
                        }
                    }
                } else { // nth time: conditional AC. Append to acText
                    StringBuilder value = new StringBuilder();
                    value.append(MonsterFields.ac.replaceTextFrom(acValue, this));
                    if (MonsterFields.from.existsIn(acValue)) {
                        value.append(" from ").append(joinAndReplace(MonsterFields.from.readArrayFrom(acValue)));
                    }
                    if (Tools5eFields.condition.existsIn(acValue)) {
                        value.append(" ").append(Tools5eFields.condition.replaceTextFrom(acValue, this));
                    }
                    details.add(value.toString());
                }
            }
            if (!details.isEmpty()) {
                acHp.acText = replaceText(String.join("; ", details));
            }
        } else if (MonsterFields.special.existsIn(acNode)) {
            acHp.acText = MonsterFields.special.replaceTextFrom(acNode, this);
        } else {
            tui().warnf(Msg.UNKNOWN, "Unknown armor class in monster %s: %s", sources.getKey(), acNode.toPrettyString());
        }
    }

    void findHp(AcHp acHp) {
        JsonNode health = MonsterFields.hp.getFrom(rootNode);

        if (health != null && health.isNumber()) {
            acHp.hp = health.asInt();
        } else if (MonsterFields.special.existsIn(health)) {
            String special = MonsterFields.special.replaceTextFrom(health, this);
            if (special.matches("^[\\d\"]+$")) {
                acHp.hp = Integer.parseInt(special.replace("\"", ""));
                if (MonsterFields.original.existsIn(health)) {
                    acHp.hpText = MonsterFields.original.replaceTextFrom(health, this);
                }
            } else {
                acHp.hpText = replaceText(special);
            }
        } else {
            if (MonsterFields.average.existsIn(health)) {
                acHp.hp = MonsterFields.average.getFrom(health).asInt();
            }
            if (MonsterFields.formula.existsIn(health)) {
                acHp.hitDice = MonsterFields.formula.getFrom(health).asText();
            }
        }

        if (acHp.hpText == null && acHp.hitDice == null && acHp.hp == null) {
            acHp.hp = null;
            acHp.hpText = "—";
        }
    }

    ImageRef getToken() {
        // 5eTools mirror 1 - png; differences in path construction
        //
        // static getTokenUrl (obj) {
        //     return obj.tokenUrl
        //       || UrlUtil.link(`${Renderer.get().baseMediaUrls["img"]
        //       || Renderer.get().baseUrl}img/objects/tokens/${Parser.sourceJsonToAbv(obj.source)}/${Parser.nameToTokenName(obj.name)}.png`);
        // }
        //          Renderer.get().baseUrl}img/${Parser.sourceJsonToAbv(mon.source)}/${Parser.nameToTokenName(mon.name)}.png`);
        //          Renderer.get().baseUrl}img/vehicles/tokens/${Parser.sourceJsonToAbv(veh.source)}/${Parser.nameToTokenName(veh.name)}.png`);
        //
        // nameToTokenName = function (name) { return name.toAscii().replace(/"/g, ""); }
        //
        // 5eTools mirror 2 - webp
        //
        // Notice injection of base path (img) into rendered URL
        // static getTokenUrl (mon) {
        //     if (mon.tokenUrl) return mon.tokenUrl;
        //     return Renderer.get().getMediaUrl("img",
        //          `bestiary/tokens/${Parser.sourceJsonToAbv(mon.source)}/${Parser.nameToTokenName(mon.name)}.webp`);
        // }
        //          `objects/tokens/${Parser.sourceJsonToAbv(obj.source)}/${Parser.nameToTokenName(obj.name)}.webp`
        //          `vehicles/tokens/${Parser.sourceJsonToAbv(veh.source)}/${Parser.nameToTokenName(veh.name)}.webp`
        // this.getMediaUrl = function (mediaDir, path) {
        //      if (Renderer.get().baseMediaUrls[mediaDir])
        //          return `${Renderer.get().baseMediaUrls[mediaDir]}${path}`;
        //      return `${Renderer.get().baseUrl}${mediaDir}/${path}`;
        // };
        String targetDir = getImagePath() + "/token/";

        // "original" is set by conjured monster variant
        String name = getTextOrDefault(rootNode, "original", getName());
        String filename = Normalizer.normalize(name, Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("Æ", "AE")
                .replace("æ", "ae")
                .replace("\"", "");

        String sourcePath;
        if (Tools5eFields.tokenHref.existsIn(rootNode)) {
            JsonHref href = readHref(Tools5eFields.tokenHref.getFrom(rootNode));
            sourcePath = href.url == null ? href.path : href.url;
        } else {
            sourcePath = Tools5eFields.tokenUrl.getTextOrNull(rootNode);
        }

        final String ext = sourcePath == null
                ? ".webp"
                : sourcePath.substring(sourcePath.lastIndexOf('.'));

        if (sourcePath == null && Tools5eFields.hasToken.booleanOrDefault(rootNode, false)) {
            // Construct the source path
            List<String> paths = new ArrayList<>();
            switch (type) {
                case monster -> paths.add("bestiary/tokens");
                case object -> paths.add("objects/tokens");
                case vehicle -> paths.add("vehicles/tokens");
                default -> throw new IllegalArgumentException("Unknown type looking for token path: " + type);
            }
            paths.add(getSources().mapPrimarySource());
            paths.add(filename + ext);

            sourcePath = (String.join("/", paths)).replace("//", "/");
        }

        if (sourcePath == null) {
            return null;
        }

        Path targetFile = Path.of(targetDir,
                Tools5eQuteBase.fixFileName(slugify(filename), getSources()) + ext);

        return getSources().buildTokenImageRef(index, sourcePath, targetFile, true);
    }

    String collectImmunities(JsonNode fromNode, VulnerabilityFields field) {
        if (field.existsIn(fromNode)) { // filter out null elements
            List<String> immunities = new ArrayList<>();
            StringBuilder separator = new StringBuilder();

            for (JsonNode value : field.iterateArrayFrom(fromNode)) {
                if (value.isTextual()) { // damage or condition type
                    immunities.add(textValue(field, value.asText()));
                } else if (VulnerabilityFields.special.existsIn(value)) { // "special"
                    immunities.add(VulnerabilityFields.special.replaceTextFrom(value, this)
                            .replace("see (below|above)", "see details"));
                } else { // conditional
                    List<String> allText = new ArrayList<>();
                    if (VulnerabilityFields.preNote.existsIn(value)) {
                        allText.add(VulnerabilityFields.preNote.replaceTextFrom(value, this));
                    }
                    allText.add(collectImmunities(value, field));
                    if (VulnerabilityFields.note.existsIn(value)) {
                        allText.add(VulnerabilityFields.note.replaceTextFrom(value, this));
                    }
                    if (separator.length() == 0 && !allText.isEmpty()) {
                        separator.append("; ");
                        immunities.add(String.join(" ", allText));
                    }
                }
            }

            if (separator.length() == 0) {
                separator.append(", ");
            }
            return String.join(separator.toString(), immunities);
        }
        return null;
    }

    private String textValue(VulnerabilityFields field, String text) {
        if (field == VulnerabilityFields.conditionImmune) {
            return linkify(Tools5eIndexType.condition, text);
        }
        return text;
    }

    Collection<NamedText> collectSortedTraits(JsonNode array) {
        boolean pushed = parseState().pushTrait();
        try {
            // gather traits into a sorted array
            ArrayNode sorted = Tui.MAPPER.createArrayNode();
            sorted.addAll(sortedTraits(array));

            List<NamedText> namedText = new ArrayList<>();
            collectTraits(namedText, sorted);
            return namedText;
        } finally {
            parseState().pop(pushed);
        }
    }

    Collection<NamedText> collectTraits(String field) {
        boolean pushed = parseState().pushTrait();
        try {
            List<NamedText> traits = new ArrayList<>();
            JsonNode header = rootNode.get(field + "Header");
            if (header != null) {
                addNamedTrait(traits, "", header);
            }
            collectTraits(traits, rootNode.get(field));
            return traits;
        } finally {
            parseState().pop(pushed);
        }
    }

    void collectTraits(List<NamedText> traits, JsonNode array) {
        boolean pushed = parseState().pushTrait();
        try {
            if (array == null || array.isNull()) {
                return;
            } else if (array.isObject()) {
                tui().warnf(Msg.UNKNOWN, "Unknown %s for %s: %s", array, sources.getKey(), array.toPrettyString());
                return;
            }
            for (JsonNode e : iterableElements(array)) {
                String name = SourceField.name.replaceTextFrom(e, this)
                        .replaceAll(":$", "");
                addNamedTrait(traits, name, e);
            }
        } finally {
            parseState().pop(pushed);
        }
    }

    void addNamedTrait(Collection<NamedText> traits, String name, JsonNode node) {
        boolean pushed = parseState().pushTrait();
        try {
            List<String> text = new ArrayList<>();
            List<NamedText> nested = List.of();
            if (node.isObject()) {
                if (!SourceField.name.existsIn(node)) {
                    appendToText(text, node, null);
                } else {
                    appendToText(text, SourceField.entry.getFrom(node), null);
                    appendToText(text, SourceField.entries.getFrom(node), null);
                }
            } else if (node.isArray()) {
                // preformat text, but also collect nodes
                appendToText(text, node, null);
                nested = new ArrayList<>();
                collectTraits(nested, node);
            } else {
                appendToText(text, node, null);
            }
            NamedText nt = new NamedText(name, text, nested);
            if (nt.hasContent()) {
                traits.add(nt);
            }
        } finally {
            parseState().pop(pushed);
        }
    }

    List<String> collectEntries(JsonNode node) {
        List<String> text = new ArrayList<>();
        appendToText(text, SourceField.entry.getFrom(node), null);
        appendToText(text, SourceField.entries.getFrom(node), null);
        return text;
    }

    Collection<JsonNode> sortedTraits(JsonNode arrayNode) {
        boolean pushed = parseState().pushTrait();
        try {
            if (arrayNode == null || arrayNode.isNull()) {
                return List.of();
            } else if (arrayNode.isObject()) {
                tui().errorf("Can't sort an object: %s", arrayNode);
                throw new IllegalArgumentException("Object passed to sortedTraits: " + getSources());
            }

            return streamOf(arrayNode).sorted((a, b) -> {
                Optional<Integer> aSort = Tools5eFields.sort.intFrom(a);
                Optional<Integer> bSort = Tools5eFields.sort.intFrom(b);

                if (aSort.isPresent() && bSort.isPresent()) {
                    return aSort.get().compareTo(bSort.get());
                } else if (aSort.isPresent() && bSort.isEmpty()) {
                    return -1;
                } else if (aSort.isEmpty() && bSort.isPresent()) {
                    return 1;
                }

                String aName = SourceField.name.replaceTextFrom(a, this).toLowerCase();
                String bName = SourceField.name.replaceTextFrom(b, this).toLowerCase();
                if (aName.isEmpty() && bName.isEmpty()) {
                    return 0;
                }

                boolean isOnlyA = aName.endsWith(" only)");
                boolean isOnlyB = bName.endsWith(" only)");
                if (!isOnlyA && isOnlyB) {
                    return -1;
                } else if (isOnlyA && !isOnlyB) {
                    return 1;
                }

                int specialA = specialTraits.indexOf(aName);
                int specialB = specialTraits.indexOf(bName);
                if (specialA > -1 && specialB > -1) {
                    return specialA - specialB;
                } else if (specialA > -1 && specialB == -1) {
                    return -1;
                } else if (specialA == -1 && specialB > -1) {
                    return 1;
                }

                return aName.compareTo(bName);
            }).toList();
        } finally {
            parseState().pop(pushed);
        }
    }

    public final Tools5eQuteBase build() {
        boolean pushed = parseState().push(getSources(), rootNode);
        try {
            return buildQuteResource();
        } catch (Exception e) {
            tui().errorf(e, "build(): Error processing '%s': %s", getName(), e.toString());
            throw e;
        } finally {
            parseState().pop(pushed);
        }
    }

    public final Tools5eQuteNote buildNote() {
        boolean pushed = parseState().push(getSources(), rootNode);
        try {
            return buildQuteNote();
        } catch (Exception e) {
            tui().errorf(e, "buildNote(): Error processing '%s': %s", getName(), e.toString());
            throw e;
        } finally {
            parseState().pop(pushed);
        }
    }

    protected Tools5eQuteBase buildQuteResource() {
        tui().warnf("The default buildQuteResource method was called for %s. Was this intended?", sources.toString());
        return null;
    }

    protected Tools5eQuteNote buildQuteNote() {
        tui().warnf("The default buildQuteNote method was called for %s. Was this intended?", sources.toString());
        return null;
    }

    enum VulnerabilityFields implements JsonNodeReader {
        cond,
        conditionImmune,
        immune,
        note,
        preNote,
        resist,
        special,
        vulnerable,
    }

    // weighted (order matters)
    enum PrereqFields implements JsonNodeReader {
        /* */ level,
        /* */ pact,
        /* */ patron,
        /* */ spell,
        /* */ race,
        /* */ alignment,
        /* */ ability,
        /* */ proficiency,
        /* */ expertise,
        /* */ spellcasting,
        /* */ spellcasting2020,
        /* */ spellcastingFeature,
        /* */ spellcastingPrepared,
        /* */ spellcastingFocus,
        /* */ psionics,
        /* */ feature,
        /* */ feat,
        /* */ background,
        /* */ item,
        /* */ itemType,
        /* */ itemProperty,
        /* */ campaign,
        /* */ culture,
        /* */ group,
        /* */ other,
        /* */ otherSummary,
        choose, // inner field for spells
        displayEntry, // inner field for display
        note, // field alongside other fields
        prerequisite, // prereq field itself
        optionalfeature,
        unknown // catcher for unknown attributes (see #fromString())
    }

    static PrereqFields fromString(String name) {
        for (PrereqFields f : PrereqFields.values()) {
            if (f.name().equals(name)) {
                return f;
            }
        }
        return PrereqFields.unknown;
    }

    enum AbilityScoreIncreaseFields implements JsonNodeReader {
        str,
        dex,
        con,
        intel,
        wis,
        cha,
        choose,
        unknown // catcher for unknown attributes (see #fromString())
    }

    static AbilityScoreIncreaseFields abilityScoreIncreaseFieldFromString(String name) {
        for (AbilityScoreIncreaseFields field : AbilityScoreIncreaseFields.values()) {
            if (field.name().equals(name))
                return field;

            if (name == "int")
                return AbilityScoreIncreaseFields.intel;
        }
        return AbilityScoreIncreaseFields.unknown;
    }
}
