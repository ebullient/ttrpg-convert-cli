package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.CompendiumSources;
import io.quarkus.qute.TemplateData;

@TemplateData
public class ToolsPf2eSources extends CompendiumSources {

    public ToolsPf2eSources(IndexType type, String key, JsonNode jsonElement) {
        super(type, key, jsonElement);
        //TODO Auto-generated constructor stub
    }

}
