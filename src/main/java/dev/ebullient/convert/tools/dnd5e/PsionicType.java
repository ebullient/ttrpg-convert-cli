package dev.ebullient.convert.tools.dnd5e;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

public interface PsionicType {

    default String combineWith(String order) {
        if (order == null || order.isEmpty()) {
            return getFullName();
        }

        return isAltDisplay()
                ? getFullName() + " (" + order + ")"
                : order + " " + getFullName();
    }

    String getFullName();

    boolean isAltDisplay();

    @RegisterForReflection
    static class CustomPsionicType implements PsionicType {
        @JsonProperty("full")
        String fullName;
        @JsonProperty("short")
        String shortName;
        boolean hasOrder;
        boolean isAltDisplay;

        public String getFullName() {
            return fullName;
        }

        public boolean isAltDisplay() {
            return isAltDisplay;
        }
    }

    enum PsionicTypeEnum implements PsionicType {

        Discipline("Discipline", "D"),
        Talent("Talent", "T");

        private String fullName;
        private String shortName;

        PsionicTypeEnum(String fullName, String shortName) {
            this.fullName = fullName;
            this.shortName = shortName;
        }

        public String getFullName() {
            return fullName;
        }

        public boolean isAltDisplay() {
            return false;
        }
    }
}
