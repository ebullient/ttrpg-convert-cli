package dev.ebullient.convert.qute;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@TemplateData
public class SourceAndPage {
    public final String source;
    public final String page;

    public SourceAndPage(JsonNode jsonElement) {
        source = SourceField.source.getTextOrNull(jsonElement);
        page = SourceField.page.getTextOrNull(jsonElement);
    }

    public SourceAndPage(String source, String page) {
        this.source = source;
        this.page = page;
    }

    public String toString() {
        if (source != null) {
            String book = TtrpgConfig.sourceToLongName(source);
            if (page != null) {
                return String.format("%s p. %s", book, page);
            }
            return book;
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((page == null) ? 0 : page.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SourceAndPage other = (SourceAndPage) obj;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        if (page == null) {
            return other.page == null;
        } else
            return page.equals(other.page);
    }
}
