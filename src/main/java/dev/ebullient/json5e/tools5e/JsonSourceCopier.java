package dev.ebullient.json5e.tools5e;

public class JsonSourceCopier implements JsonSource {

    final JsonIndex index;

    JsonSourceCopier(JsonIndex index) {
        this.index = index;
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
