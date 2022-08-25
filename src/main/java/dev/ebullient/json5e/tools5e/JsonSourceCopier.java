package dev.ebullient.json5e.tools5e;

import dev.ebullient.json5e.io.Json5eTui;

public class JsonSourceCopier implements JsonSource {

    final JsonIndex index;

    JsonSourceCopier(JsonIndex index) {
        this.index = index;
    }

    @Override
    public Json5eTui tui() {
        return index.tui();
    }

    @Override
    public JsonIndex index() {
        return index;
    }

    @Override
    public CompendiumSources getSources() {
        throw new IllegalStateException("Should not call getSources while copying source");
    }
}
