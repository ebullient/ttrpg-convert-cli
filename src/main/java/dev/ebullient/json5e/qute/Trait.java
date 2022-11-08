package dev.ebullient.json5e.qute;

import io.quarkus.qute.TemplateData;

@TemplateData
public class Trait {
    public final String name;
    public final String desc;

    public Trait(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
}
