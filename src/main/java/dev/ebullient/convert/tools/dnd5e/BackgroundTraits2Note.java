package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;

public class BackgroundTraits2Note extends Json2QuteCommon {

    public BackgroundTraits2Note(Tools5eIndex index) {
        super(index, Tools5eIndexType.sourceless, null);
    }

    public List<QuteNote> buildNotes() {
        List<QuteNote> notes = new ArrayList<>();

        addIfPresent(notes, Json2QuteBackground.traits, "Personality Traits");
        addIdealsIfPresent(notes);
        addIfPresent(notes, Json2QuteBackground.bonds, "Bonds");
        addIfPresent(notes, Json2QuteBackground.flaws, "Flaws");

        return notes;
    }

    @Override
    public QuteBase build() {
        return null;
    }

    private void addIfPresent(List<QuteNote> notes, Set<String> table, String title) {
        if (table.isEmpty()) {
            return;
        }

        List<String> text = table.stream()
                .map(x -> x.replaceAll("^\\|\\s*\\d+\\s*", ""))
                .collect(Collectors.toList());

        notes.add(new QuteNote(title, null,
                String.join("\n", listToTable(title, text)),
                List.of()));
    }

    private void addIdealsIfPresent(List<QuteNote> notes) {
        if (Json2QuteBackground.ideals.isEmpty()) {
            return;
        }

        List<String> ideals = Json2QuteBackground.ideals.stream()
                .map(x -> x.replace("**", ""))
                .map(x -> x.replaceAll("^\\|\\s*\\d+\\s*", ""))
                .collect(Collectors.toList());

        List<String> good = ideals.stream().filter(x -> x.contains("(Good)"))
                .collect(Collectors.toList());
        List<String> evil = ideals.stream().filter(x -> x.contains("(Evil)"))
                .collect(Collectors.toList());
        List<String> lawful = ideals.stream().filter(x -> x.contains("(Lawful)"))
                .collect(Collectors.toList());
        List<String> chaotic = ideals.stream().filter(x -> x.contains("(Chaotic)"))
                .collect(Collectors.toList());
        List<String> neutral = ideals.stream().filter(x -> x.contains("(Neutral)"))
                .collect(Collectors.toList());
        List<String> any = ideals.stream().filter(x -> x.contains("(Any)"))
                .collect(Collectors.toList());

        List<String> text = new ArrayList<>();

        text.add("## Ideals");
        text.add("");
        text.add("| All Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^good-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^evil-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^lawful-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^chaotic-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^neutral-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^all-ideals");
        text.add("");
        text.add("| Chaotic Good Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^good-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^chaotic-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^cg-ideals");
        text.add("");
        text.add("| Chaotic Evil Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^evil-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^chaotic-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^ce-ideals");
        text.add("");
        text.add("| Chaotic Neutral Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^neutral-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^chaotic-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^cn-ideals");
        text.add("");
        text.add("| Lawful Good Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^good-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^lawful-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^lg-ideals");
        text.add("");
        text.add("| Lawful Evil Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^evil-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^lawful-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^le-ideals");
        text.add("");
        text.add("| Lawful Neutral Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^neutral-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^lawful-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^ln-ideals");
        text.add("");
        text.add("| Neutral Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^neutral-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^n-ideals");
        text.add("");
        text.add("| Neutral Good Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^neutral-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^good-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^ng-ideals");
        text.add("");
        text.add("| Neutral Evil Ideals |");
        text.add("|------------|");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^neutral-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^evil-ideals)` " + " |");
        text.add("| `dice: [](" + index.compendiumRoot() + "tables/ideals.md#^universal-ideals-any)` " + " |");
        text.add("^ne-ideals");

        maybeAddBlankLine(text);
        text.addAll(tableSection("Good Ideals", good));
        maybeAddBlankLine(text);
        text.addAll(tableSection("Evil Ideals", evil));
        maybeAddBlankLine(text);
        text.addAll(tableSection("Lawful Ideals", lawful));
        maybeAddBlankLine(text);
        text.addAll(tableSection("Chaotic Ideals", chaotic));
        maybeAddBlankLine(text);
        text.addAll(tableSection("Neutral Ideals", neutral));
        maybeAddBlankLine(text);
        text.addAll(tableSection("Universal Ideals (Any)", any));

        notes.add(new QuteNote("Ideals", null,
                String.join("\n", text),
                List.of()));
    }

    List<String> tableSection(String title, List<String> elements) {
        List<String> section = listToTable(title, elements);
        section.add(0, "");
        section.add(0, "## " + title);
        return section;
    }

    List<String> listToTable(String tableHeading, List<String> elements) {
        String header = "| " + tableHeading + " |";
        List<String> text = new ArrayList<>();
        text.add(header);
        text.add(header.replaceAll("[^|]", "-"));
        text.addAll(elements);
        text.add("^" + Tui.slugify(tableHeading));
        return text;
    }
}
