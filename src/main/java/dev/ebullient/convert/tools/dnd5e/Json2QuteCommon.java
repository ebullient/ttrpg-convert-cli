package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.dnd5e.Json2QuteMonster.MonsterFields;
import dev.ebullient.convert.tools.dnd5e.qute.AbilityScores;
import dev.ebullient.convert.tools.dnd5e.qute.AcHp;
import dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteCommon implements JsonSource {
    static final List<String> SPEED_MODE = List.of("walk", "burrow", "climb", "fly", "swim");
    static final List<String> specialTraits = List.of("special equipment", "shapechanger");

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

    String getName() {
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

    Path getTokenSourcePath(String filename) {
        return Path.of("img",
                getSources().mapPrimarySource(),
                filename + ".png");
    }

    public String getText(String heading) {
        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, heading);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public String getFluffDescription(Tools5eIndexType fluffType, String heading, List<ImageRef> images) {
        List<String> text = getFluff(fluffType, heading, images);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public List<String> getFluff(Tools5eIndexType fluffType, String heading, List<ImageRef> images) {
        List<String> text = new ArrayList<>();
        JsonNode fluffNode = null;
        if (Tools5eFields.fluff.existsIn(rootNode)) {
            fluffNode = rootNode.get("fluff");
            JsonNode monsterFluff = Tools5eFields._monsterFluff.getFrom(fluffNode);
            if (monsterFluff != null) {
                String fluffKey = fluffType.createKey(monsterFluff);
                fluffNode = index.getNode(fluffKey);
            }
        } else if (Tools5eFields.hasFluff.booleanOrDefault(rootNode, false)
                || Tools5eFields.hasFluffImages.booleanOrDefault(rootNode, false)) {
            String fluffKey = fluffType.createKey(rootNode);
            fluffNode = index.getNode(fluffKey);
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

        getImages(fluffNode.get("images"), images);
    }

    public List<ImageRef> getFluffImages(Tools5eIndexType fluffType) {
        List<ImageRef> images = new ArrayList<>();
        if (booleanOrDefault(rootNode, "hasFluffImages", false)) {
            String fluffKey = fluffType.createKey(rootNode);
            JsonNode fluffNode = index.getNode(fluffKey);
            if (fluffNode != null) {
                getImages(fluffNode.get("images"), images);
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

    ImmuneResist immuneResist() {
        return new ImmuneResist(
                collectImmunities("vulnerable"),
                collectImmunities("resist"),
                collectImmunities("immune"),
                collectImmunities("conditionImmune"));
    }

    AbilityScores abilityScores() {
        if (!rootNode.has("str")
                && !rootNode.has("dex")
                && !rootNode.has("con")
                && !rootNode.has("int")
                && !rootNode.has("wis")
                && !rootNode.has("cha")) {
            return null;
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
                            details.add(joinAndReplace(MonsterFields.from.arrayFrom(acValue)));
                        }
                    }
                } else { // nth time: conditional AC. Append to acText
                    StringBuilder value = new StringBuilder();
                    value.append(MonsterFields.ac.replaceTextFrom(acValue, this));
                    if (MonsterFields.from.existsIn(acValue)) {
                        value.append(" from ").append(joinAndReplace(MonsterFields.from.arrayFrom(acValue)));
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
            tui().errorf("Unknown armor class in monster %s: %s", sources.getKey(), acNode.toPrettyString());
        }
    }

    void findHp(AcHp acHp) {
        JsonNode health = MonsterFields.hp.getFrom(rootNode);
        if (health.isNumber()) {
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
            tui().errorf("Unknown hp from %s: %s", getSources(), health.toPrettyString());
            throw new IllegalArgumentException("Unknown hp from " + getSources());
        }
    }

    ImageRef getToken() {
        String tokenString = Tools5eFields.tokenUrl.getTextOrNull(rootNode);
        if (Tools5eFields.hasToken.booleanOrDefault(rootNode, false)) {
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

            Path sourcePath = getTokenSourcePath(filename);

            Path target = Path.of(getImagePath(),
                    "token",
                    slugify(filename) + ".png");

            return buildImageRef(sourcePath, target);
        } else if (tokenString != null) {
            return getSources().buildImageRef(null, tokenString);
        }
        return null;
    }

    String collectImmunities(String field) {
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

    Collection<NamedText> collectSortedTraits(JsonNode array) {
        // gather traits into a sorted array
        ArrayNode sorted = Tui.MAPPER.createArrayNode();
        sorted.addAll(sortedTraits(array));

        List<NamedText> namedText = new ArrayList<>();
        collectTraits(namedText, sorted);
        return namedText;
    }

    Collection<NamedText> collectTraits(String field) {
        List<NamedText> traits = new ArrayList<>();

        JsonNode header = rootNode.get(field + "Header");
        if (header != null) {
            addNamedTrait(traits, "", header);
        }

        collectTraits(traits, rootNode.get(field));
        return traits;
    }

    void collectTraits(List<NamedText> traits, JsonNode array) {
        if (array == null || array.isNull()) {
            return;
        } else if (array.isObject()) {
            tui().errorf("Unknown %s for %s: %s", array, sources.getKey(), array.toPrettyString());
            throw new IllegalArgumentException("Unknown field: " + getSources());
        }

        for (JsonNode e : iterableElements(array)) {
            String name = SourceField.name.replaceTextFrom(e, this)
                    .replaceAll(":$", "");
            addNamedTrait(traits, name, e);
        }
    }

    void addNamedTrait(Collection<NamedText> traits, String name, JsonNode node) {
        List<String> text = new ArrayList<>();
        if (node.isObject()) {
            if (!SourceField.name.existsIn(node)) {
                appendToText(text, node, null);
            } else {
                appendToText(text, SourceField.entry.getFrom(node), null);
                appendToText(text, SourceField.entries.getFrom(node), null);
            }
        } else {
            appendToText(text, node, null);
        }
        NamedText nt = new NamedText(name, text);
        if (nt.hasContent()) {
            traits.add(nt);
        }
    }

    List<String> collectEntries(JsonNode node) {
        List<String> text = new ArrayList<>();
        appendToText(text, SourceField.entry.getFrom(node), null);
        appendToText(text, SourceField.entries.getFrom(node), null);
        return text;
    }

    Collection<JsonNode> sortedTraits(JsonNode arrayNode) {
        if (arrayNode == null || arrayNode.isNull()) {
            return List.of();
        } else if (arrayNode.isObject()) {
            tui().errorf("Can't sort an object: %s", arrayNode);
            throw new IllegalArgumentException("Object passed to sortedTraits: " + getSources());
        }

        return streamOf(arrayNode).sorted((a, b) -> {
            Optional<Integer> aSort = Tools5eFields.sort.getIntFrom(a);
            Optional<Integer> bSort = Tools5eFields.sort.getIntFrom(b);

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
}
