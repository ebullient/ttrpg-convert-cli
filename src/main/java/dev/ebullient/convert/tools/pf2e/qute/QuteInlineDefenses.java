package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteInlineDefenses extends Pf2eQuteNote {

    public final QuteArmorClass ac;
    public final QuteSavingThrows savingThrows;
    public final List<QuteHitPoints> hpHardness;
    public final List<String> immunities;
    public final List<String> resistances;
    public final List<String> weaknesses;

    public QuteInlineDefenses(List<String> text, QuteArmorClass ac, QuteSavingThrows savingThrows,
            List<QuteHitPoints> hpHardness, List<String> immunities, List<String> resistances, List<String> weaknesses) {
        super(Pf2eIndexType.syntheticGroup, "Defenses", null, text, List.of());
        this.ac = ac;
        this.savingThrows = savingThrows;
        this.hpHardness = hpHardness;
        this.immunities = immunities;
        this.resistances = resistances;
        this.weaknesses = weaknesses;
    }

    @Override
    public String template() {
        return "inline-defenses2md.txt";
    }

    @TemplateData
    public static class QuteArmorClass {
        public Map<String, String> armorClass;
        public String note;
        public String abilities;

        public String toString() {
            return armorClass.entrySet().stream()
                    .map(e -> "**" + e.getKey() + "** " + e.getValue())
                    .collect(Collectors.joining(", "))
                    + note
                    + (abilities == null ? "" : "; " + abilities);
        }
    }

    @TemplateData
    public static class QuteSavingThrows {
        public Map<String, String> savingThrows;
        public String abilities;

        public String toString() {
            return savingThrows.entrySet().stream()
                    .map(e -> "**" + e.getKey() + "** " + e.getValue())
                    .collect(Collectors.joining(", "))
                    + (abilities == null ? "" : ", " + abilities);
        }
    }

    @TemplateData
    public static class QuteHitPoints {
        public String name;
        public String hpNotes;
        public String hpValue;
        public String hardnessNotes;
        public String hardnessValue;
        public String brokenThreshold;

        public String toString() {
            String n = name == "" ? "" : (name + " ");
            String hardPart = "";
            String btPart = "";
            String hpPart = "";
            String hpNotePart = "";

            if (hardnessValue != null) {
                hardPart = String.format("**%sHardness** %s%s",
                        n, hardnessValue,
                        hardnessNotes == null ? ", " : (" " + hardnessNotes + "; "));
            }
            if (brokenThreshold != null) {
                btPart = " (BT " + brokenThreshold + ")";
            }
            if (hpValue != null) {
                hpPart = String.format("**%sHP** %s", n, hpValue);
            }
            if (hpNotes != null) {
                hpNotePart = " " + hpNotes;
            }
            return hardPart + hpPart + btPart + hpNotePart;
        }

    }
}
