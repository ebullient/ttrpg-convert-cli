package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.dnd5e.qute.AbilityScores;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteCommon implements JsonSource {
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
        appendFootnotes(text, 0);
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
            if (fluffNode.isArray()) {
                for (JsonNode f : iterableElements(fluffNode)) {
                    unpackFluffNode(fluffType, f, text, heading, images);
                }
            } else {
                unpackFluffNode(fluffType, fluffNode, text, heading, images);
            }
        }
        return text;
    }

    public void unpackFluffNode(Tools5eIndexType fluffType, JsonNode fluffNode, List<String> text, String heading,
            List<ImageRef> images) {
        JsonSourceCopier copier = new JsonSourceCopier(index);
        fluffNode = copier.handleCopy(fluffType, fluffNode);
        if (fluffNode.has("entries")) {
            boolean pushed = parseState().push(getSources(), fluffNode);
            try {
                appendToText(text, fluffNode, heading);
            } finally {
                parseState().pop(pushed);
            }
        }
        getImages(fluffNode.get("images"), images);
    }

    public List<ImageRef> getFluffImages(Tools5eIndexType fluffType) {
        List<ImageRef> images = new ArrayList<>();
        if (booleanOrDefault(rootNode, "hasFluffImages", false)) {
            String fluffKey = fluffType.createKey(rootNode);
            JsonNode fluffNode = index.getNode(fluffKey);
            if (fluffNode != null) {
                JsonSourceCopier copier = new JsonSourceCopier(index);
                fluffNode = copier.handleCopy(fluffType, fluffNode);
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
        if (speedNode == null) {
            return null;
        } else if (speedNode.isNumber()) {
            return String.format("%s ft.", speedNode.asText());
        }

        List<String> speed = new ArrayList<>();
        for (Entry<String, JsonNode> f : iterableFields(speedNode)) {
            if (f.getValue().isNumber()) {
                speed.add(String.format("%s %s ft.", f.getKey(), f.getValue().asText()));
            } else if (Tools5eFields.number.existsIn(f.getValue())) {
                speed.add(String.format("%s %s ft.%s",
                        f.getKey(),
                        Tools5eFields.number.replaceTextFrom(f.getValue(), this),
                        Tools5eFields.condition.existsIn(f.getValue())
                                ? " " + Tools5eFields.condition.replaceTextFrom(f.getValue(), this)
                                : ""));
            }
        }
        return replaceText(String.join(", ", speed));
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

    Collection<NamedText> collectTraits(String field) {
        List<NamedText> traits = new ArrayList<>();

        JsonNode header = rootNode.get(field + "Header");
        if (header != null) {
            addNamedTrait(traits, "", header);
        }

        JsonNode array = rootNode.get(field);
        if (array == null || array.isNull()) {
            return traits;
        } else if (array.isObject()) {
            tui().errorf("Unknown %s for %s: %s", field, sources.getKey(), array.toPrettyString());
            throw new IllegalArgumentException("Unknown field: " + getSources());
        }

        for (JsonNode e : iterableElements(array)) {
            String name = SourceField.name.replaceTextFrom(e, this)
                    .replaceAll(":$", "");
            addNamedTrait(traits, name, e);
        }
        return traits;
    }

    void addNamedTrait(Collection<NamedText> traits, String name, JsonNode node) {
        List<String> text = new ArrayList<>();
        appendToText(text, SourceField.entry.getFrom(node), null);
        appendToText(text, SourceField.entries.getFrom(node), null);
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
