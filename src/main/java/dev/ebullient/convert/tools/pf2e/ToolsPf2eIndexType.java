package dev.ebullient.convert.tools.pf2e;

public enum ToolsPf2eIndexType implements dev.ebullient.convert.tools.IndexType {
    ancestry;

    String templateName;

    ToolsPf2eIndexType() {
        this.templateName = this.name();
    }

    ToolsPf2eIndexType(String templateName) {
        this.templateName = templateName;
    }

    public String templateName() {
        return templateName;
    }

    public static ToolsPf2eIndexType getTypeFromKey(String key) {
        String typeKey = key.substring(0, key.indexOf("|"));
        return valueOf(typeKey);
    }
}
