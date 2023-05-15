package dev.ebullient.convert.tools.dnd5e;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.tools.CompendiumSources;
import io.quarkus.qute.TemplateData;

@TemplateData
public class Tools5eSources extends CompendiumSources {
    final boolean srd;
    final boolean basicRules;

    public Tools5eSources(Tools5eIndexType type, String key, JsonNode jsonElement) {
        super(type, key, jsonElement);
        this.basicRules = jsonElement.has("basicRules")
                ? jsonElement.get("basicRules").asBoolean(false)
                : false;
        this.srd = jsonElement.has("srd")
                ? jsonElement.get("srd").asBoolean(false)
                : false;
    }

    @Override
    public Tools5eIndexType getType() {
        return (Tools5eIndexType) type;
    }

    @Override
    public String getSourceText(boolean useSrd) {
        if (useSrd) {
            return "SRD / Basic Rules";
        }
        return sourceText;
    }

    public List<String> getSourceTags() {
        return List.of("compendium/src/" + primarySource().toLowerCase());
    }

    @Override
    protected String findSourceText(JsonNode jsonElement) {
        String srcText = super.findSourceText(jsonElement);

        String srdBasic = null;
        if (srd && basicRules) {
            srdBasic = "Available in the SRD and the Basic Rules.";
        } else if (srd) {
            srdBasic = "Available in the SRD.";
        } else if (basicRules) {
            srdBasic = "Available in the Basic Rules.";
        }

        String sourceText = String.join(", ", srcText);
        if (srdBasic != null) {
            return sourceText.isEmpty()
                    ? srdBasic
                    : sourceText + ". " + srdBasic;
        }

        return sourceText;
    }

    @Override
    protected boolean subclassFilter(String source) {
        return !List.of("phb", "mm", "dmg").contains(source.toLowerCase());
    }

    public Optional<String> uaSource() {
        Optional<String> source = bookSources.stream().filter(x -> x.contains("UA") && !x.equals("UAWGE")).findFirst();
        return source.map(TtrpgConfig::sourceToAbbreviation);
    }

    public String alternateSource() {
        Iterator<String> i = bookSources.iterator();
        if (bookSources.size() > 1) {
            i.next();
        }
        return i.next();
    }

    @Override
    public String toString() {
        return "sources[" + key + ']';
    }
}
