package dev.ebullient.convert.qute;

import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class Trait {
    public final String name;
    public final String desc;

    public Trait(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
}
