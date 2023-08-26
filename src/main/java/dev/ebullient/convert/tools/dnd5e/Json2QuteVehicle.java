package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.AcHp;
import dev.ebullient.convert.tools.dnd5e.qute.QuteVehicle;
import dev.ebullient.convert.tools.dnd5e.qute.QuteVehicle.ShipAcHp;
import dev.ebullient.convert.tools.dnd5e.qute.QuteVehicle.ShipCrewCargoPace;
import dev.ebullient.convert.tools.dnd5e.qute.QuteVehicle.ShipSection;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteVehicle extends Json2QuteCommon {

    final VehicleType vehicleType;

    Json2QuteVehicle(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        vehicleType = VehicleType.from(rootNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());
        tags.add("vehicle", "type", vehicleType.value);

        List<String> terrain = VehicleFields.terrain.replaceTextFromList(rootNode, index);
        terrain.forEach(x -> tags.add("vehicle", "terrain", x));

        List<ImageRef> fluffImages = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.vehicleFluff, "##", fluffImages);
        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        return new QuteVehicle(sources,
                getSources().getName(),
                getSourceText(sources),
                vehicleType.name(),
                String.join(", ", terrain),
                abilityScores(),
                vehicleSize(tags),
                immuneResist(),
                shipCrewCargoPace(),
                shipSections(),
                getActions(),
                getToken(), fluffImages,
                String.join("\n", text),
                tags);
    }

    @Override
    Path getTokenSourcePath(String filename) {
        return Path.of("img", "vehicles", "tokens",
                getSources().mapPrimarySource(),
                filename + ".png");
    }

    String vehicleSize(Tags tags) {
        String dimensions = "";
        String weight = "";
        if (VehicleFields.dimensions.existsIn(rootNode)) {
            dimensions = " (" + VehicleFields.dimensions.joinAndReplace(rootNode, this, " by ") + ")";
        }
        if (VehicleFields.weight.existsIn(rootNode)) {
            weight += " (" + poundsToTons(VehicleFields.weight.getFrom(rootNode)) + ")";
        }
        if (Tools5eFields.size.existsIn(rootNode)) {
            String size = getSize(rootNode);
            tags.add("vehicle", "size", size);

            return size + " vehicle" + dimensions + weight;
        } else if (!dimensions.isBlank()) {
            return toTitleCase(vehicleType.value) + dimensions + weight;
        }
        return null;
    }

    String vehicleSpeed() {
        if (vehicleType == VehicleType.INFWAR) {
            return speed(Tools5eFields.speed.getFrom(rootNode));
        }
        return "";
    }

    private Collection<NamedText> getActions() {
        Collection<NamedText> actions = collectTraits("action");

        if (VehicleFields.other.existsIn(rootNode)) {
            for (JsonNode node : VehicleFields.other.iterateArrayFrom(rootNode)) {
                if (SourceField.name.getTextOrEmpty(node).equals("Actions")) {
                    addNamedTrait(actions, "", node);
                }
            }
        }
        return actions;
    }

    private ShipAcHp getAcHp(JsonNode node) {
        String cost = getCost(node);
        return new ShipAcHp(vehicleType.name(),
                VehicleFields.ac.getIntFrom(node).orElse(null),
                VehicleFields.acFrom.getTextOrNull(node),
                VehicleFields.hp.getIntFrom(node).orElse(null),
                VehicleFields.hpNote.getTextOrNull(node),
                VehicleFields.dt.getIntFrom(node).orElse(null),
                null,
                cost == null ? null : cost.toString());
    }

    private String getCost(JsonNode node) {
        String cost = convertCost(node);
        if (VehicleFields.costs.existsIn(node)) {
            List<String> costs = new ArrayList<>();
            for (JsonNode costNode : VehicleFields.costs.iterateArrayFrom(node)) {
                costs.add(convertCost(costNode));
            }
            cost = String.join(", ", costs);
        }
        return cost;
    }

    private String convertCost(JsonNode node) {
        Optional<Integer> costCp = VehicleFields.cost.getIntFrom(node);
        String note = VehicleFields.note.getTextOrNull(node);
        if (costCp.isPresent() || note != null) {
            return costCp.map(x -> convertCurrency(x)).orElse("\u23E4") + (note == null ? "" : " (" + note + ")");
        }
        return null;
    }

    private ShipCrewCargoPace shipCrewCargoPace() {
        Integer shipPace = null;
        String speedPace = "";
        ShipAcHp shipAcHp = null;
        String keelBeam = null;

        if (vehicleType == VehicleType.SHIP) {
            shipPace = VehicleFields.pace.getIntFrom(rootNode).orElse(null);
        } else if (vehicleType == VehicleType.SPELLJAMMER) {
            JsonNode speedNode = VehicleFields.speed.getFrom(rootNode);
            JsonNode paceNode = VehicleFields.pace.getFrom(rootNode);

            String speed = speed(speedNode, false);
            List<String> text = new ArrayList<>();
            for (String k : SPEED_MODE) {
                JsonNode pNode = paceNode.get(k);
                if (pNode != null) {
                    String prefix = "walk".equals(k) ? "" : k + " ";
                    double num = convertToNumber(pNode.asText());
                    text.add(prefix + pNode.asText() + " mph ^[" + (num * 24) + " miles per day]");
                }
            }
            String pace = text.isEmpty() ? "" : " (" + String.join(", ", text) + ")";
            speedPace = speed + pace;

            JsonNode hull = VehicleFields.hull.getFrom(rootNode);
            shipAcHp = getAcHp(hull);
            // Replace the cost with the value from the root node
            shipAcHp.cost = getCost(rootNode);

            if (VehicleFields.dimensions.existsIn(rootNode)) {
                keelBeam = VehicleFields.dimensions.joinAndReplace(rootNode, this, " by ");
            }
        } else if (vehicleType == VehicleType.INFWAR) {
            int dexMod = VehicleFields.dexMod.getIntFrom(rootNode).orElse(0);
            JsonNode hpNode = VehicleFields.hp.getFrom(rootNode);
            shipAcHp = new ShipAcHp(vehicleType.name(),
                    dexMod == 0 ? 19 : 19 + dexMod,
                    dexMod == 0 ? "" : "19 while motionless",
                    VehicleFields.hp.getIntFrom(hpNode).orElse(null),
                    null,
                    VehicleFields.dt.getIntFrom(hpNode).orElse(null),
                    VehicleFields.mt.getIntFrom(hpNode).orElse(null),
                    null);
            speedPace = speed(Tools5eFields.speed.getFrom(rootNode));
        } else if (vehicleType == VehicleType.CREATURE || vehicleType == VehicleType.OBJECT) {
            AcHp creatureAcHp = new AcHp();
            findAc(creatureAcHp);
            findHp(creatureAcHp);
            shipAcHp = new ShipAcHp(vehicleType.name(), creatureAcHp);
            speedPace = speed(Tools5eFields.speed.getFrom(rootNode));
        }

        String capCrew = VehicleFields.capCrew.getTextOrEmpty(rootNode);
        if (!capCrew.isBlank()) {
            capCrew = capCrew + (vehicleType == VehicleType.INFWAR
                    ? " Medium creatures"
                    : " crew");
        }

        String cargo = "";
        if (VehicleFields.capCargo.existsIn(rootNode)) {
            JsonNode cargoNode = VehicleFields.capCargo.getFrom(rootNode);
            if (cargoNode.isTextual()) {
                cargo = replaceText(cargoNode.asText());
            } else if (vehicleType == VehicleType.INFWAR) {
                cargo = poundsToTons(cargoNode);
            } else {
                String txt = cargoNode.asText();
                cargo = txt + " ton" + ("1".equals(txt) ? "" : "s");
            }
        }

        return new ShipCrewCargoPace(vehicleType.name(),
                capCrew,
                VehicleFields.capCrewNote.replaceTextFrom(rootNode, this),
                VehicleFields.capPassenger.getTextOrEmpty(rootNode),
                cargo,
                shipPace, speedPace, shipAcHp, keelBeam);
    }

    private String poundsToTons(JsonNode sourceNode) {
        int lbs = sourceNode.asInt();
        int tons = lbs / 2000;
        lbs %= 2000;
        return (tons == 0 ? "" : tons + " ton" + (tons == 1 ? "" : "s")) + (lbs == 0 ? "" : " " + lbs + " lb.");
    }

    private List<ShipSection> shipSections() {
        List<ShipSection> sections = new ArrayList<>();
        switch (vehicleType) {
            case SHIP -> getShipSections(sections);
            case SPELLJAMMER -> getSpelljammerSections(sections);
            case INFWAR -> getWarMachineSections(sections);
            case CREATURE -> getCreatureSections(sections);
            default -> {
            }
        }
        return sections;
    }

    void getShipSections(List<ShipSection> sections) {
        if (VehicleFields.hull.existsIn(rootNode)) {
            JsonNode hull = VehicleFields.hull.getFrom(rootNode);
            sections.add(new ShipSection("Hull",
                    getAcHp(hull), null, collectEntries(hull), null));
        }
        if (VehicleFields.trait.existsIn(rootNode)) {
            sections.add(new ShipSection("Traits",
                    null, null,
                    collectSortedEntries(rootNode, VehicleFields.trait),
                    null));
        }
        for (JsonNode node : VehicleFields.control.iterateArrayFrom(rootNode)) {
            String name = SourceField.name.replaceTextFrom(node, this);
            sections.add(new ShipSection("Control: " + name,
                    getAcHp(node), null, collectEntries(node), null));
        }
        for (JsonNode node : VehicleFields.movement.iterateArrayFrom(rootNode)) {
            String name = SourceField.name.replaceTextFrom(node, this);
            boolean isControl = VehicleFields.isControl.booleanOrDefault(node, false);

            List<String> speed = new ArrayList<>();
            addLocomotion(speed, VehicleFields.locomotion.getFrom(node));
            addMovementSpeed(speed, VehicleFields.speed.getFrom(node));

            sections.add(new ShipSection((isControl ? "Control and movement: " : "Movement: ") + name,
                    getAcHp(node), speed, collectEntries(node), null));
        }
        for (JsonNode node : VehicleFields.weapon.iterateArrayFrom(rootNode)) {
            String name = SourceField.name.replaceTextFrom(node, this);
            Optional<Integer> count = VehicleFields.count.getIntFrom(node);
            if (count.isPresent()) {
                name += " (" + count.get() + ")";
            }

            sections.add(new ShipSection("Weapon: " + name,
                    getAcHp(node), null, collectEntries(node), null));
        }
        if (VehicleFields.other.existsIn(rootNode)) {
            for (JsonNode node : VehicleFields.other.iterateArrayFrom(rootNode)) {
                String name = SourceField.name.replaceTextFrom(node, this);
                if (!name.isBlank() && !name.equals("Actions")) {
                    sections.add(new ShipSection(name,
                            getAcHp(node), null, collectEntries(node), null));
                }
            }
        }
    }

    void addLocomotion(List<String> speed, JsonNode locomotionNode) {
        for (JsonNode node : iterableElements(locomotionNode)) {
            String mode = VehicleFields.mode.getTextOrEmpty(node);
            speed.add(new NamedText("Locomotion (" + mode + ").",
                    collectEntries(node)).toString());
        }
    }

    void addMovementSpeed(List<String> speed, JsonNode speedNode) {
        for (JsonNode node : iterableElements(speedNode)) {
            String mode = VehicleFields.mode.getTextOrEmpty(node);
            speed.add(new NamedText("Speed (" + mode + ").",
                    collectEntries(node)).toString());
        }
    }

    private void getSpelljammerSections(List<ShipSection> sections) {
        for (JsonNode node : VehicleFields.weapon.iterateArrayFrom(rootNode)) {
            String name = SourceField.name.replaceTextFrom(node, this);
            Optional<Integer> count = VehicleFields.count.getIntFrom(node);
            Optional<Integer> crew = VehicleFields.crew.getIntFrom(node);
            boolean isMultiple = count.isPresent() && count.get() > 1;

            if (isMultiple) {
                name = count.get() + " " + name;
            }
            if (crew.isPresent()) {
                name += " (Crew: " + crew.get() + (isMultiple ? " each" : "") + ")";
            }

            List<String> actions = List.of();
            if (VehicleFields.action.existsIn(node)) {
                actions = collectNamedEntries(node, VehicleFields.action);
            }

            sections.add(new ShipSection("Weapon: " + name,
                    getAcHp(node), null, collectEntries(node), actions));
        }
    }

    private void getWarMachineSections(List<ShipSection> sections) {
        if (VehicleFields.trait.existsIn(rootNode)) {
            sections.add(new ShipSection("Traits",
                    null, null,
                    collectSortedEntries(rootNode, VehicleFields.trait),
                    null));
        }
        if (VehicleFields.actionStation.existsIn(rootNode)) {
            sections.add(new ShipSection("Action Stations",
                    null, null,
                    collectNamedEntries(rootNode, VehicleFields.actionStation),
                    null));
        }
        if (VehicleFields.reaction.existsIn(rootNode)) {
            sections.add(new ShipSection("Reactions",
                    null, null,
                    collectNamedEntries(rootNode, VehicleFields.reaction),
                    null));
        }
    }

    private void getCreatureSections(List<ShipSection> sections) {
        if (VehicleFields.trait.existsIn(rootNode)) {
            sections.add(new ShipSection("Traits",
                    null, null,
                    collectSortedEntries(rootNode, VehicleFields.trait),
                    null));
        }
        if (VehicleFields.action.existsIn(rootNode)) {
            sections.add(new ShipSection("Actions",
                    null, null,
                    collectSortedEntries(rootNode, VehicleFields.action),
                    null));
        }
        if (VehicleFields.bonus.existsIn(rootNode)) {
            sections.add(new ShipSection("Bonus Actions",
                    null, null,
                    collectSortedEntries(rootNode, VehicleFields.bonus),
                    null));
        }
        if (VehicleFields.reaction.existsIn(rootNode)) {
            sections.add(new ShipSection("Reactions",
                    null, null,
                    collectSortedEntries(rootNode, VehicleFields.reaction),
                    null));
        }
        if (VehicleFields.legendary.existsIn(rootNode)) {
            sections.add(new ShipSection("Legendary",
                    null, null,
                    collectNamedEntries(rootNode, VehicleFields.legendary),
                    null));
        }
        if (VehicleFields.mythic.existsIn(rootNode)) {
            sections.add(new ShipSection("Mythic Actions",
                    null, null,
                    collectNamedEntries(rootNode, VehicleFields.mythic),
                    null));
        }
    }

    private List<String> collectNamedEntries(JsonNode source, JsonNodeReader field) {
        List<NamedText> namedText = new ArrayList<>();
        collectTraits(namedText, field.getFrom(source));
        return namedText.stream().map(NamedText::toString).toList();
    }

    private List<String> collectSortedEntries(JsonNode source, JsonNodeReader field) {
        Collection<NamedText> namedText = collectSortedTraits(field.getFrom(source));
        return namedText.stream().map(NamedText::toString).toList();
    }

    enum VehicleType {
        SHIP("Ship"),
        SPELLJAMMER("Spelljammer"),
        INFWAR("Infernal War Machine"),
        CREATURE("Creature"),
        OBJECT("Object");

        final String value;

        VehicleType(String value) {
            this.value = value;
        }

        static VehicleType from(JsonNode node) {
            String vehicleType = VehicleFields.vehicleType.getTextOrDefault(node, "SHIP");
            return switch (vehicleType) {
                case "SHIP" -> SHIP;
                case "SPELLJAMMER" -> SPELLJAMMER;
                case "INFWAR" -> INFWAR;
                case "CREATURE" -> CREATURE;
                case "OBJECT" -> OBJECT;
                default -> throw new IllegalArgumentException("Unexpected vehicle type: " + vehicleType);
            };
        }
    }

    enum VehicleFields implements JsonNodeReader {
        ac,
        acFrom,
        action,
        actionStation,
        capCargo,
        capCreature,
        capCrew,
        capCrewNote,
        capPassenger,
        control,
        cost,
        costs,
        count,
        crew,
        dimensions,
        dt,
        hp,
        hpNote,
        hull,
        isControl,
        locomotion,
        mode,
        movement,
        note,
        other,
        pace,
        reaction,
        size,
        speed,
        terrain,
        trait,
        vehicleType,
        weapon,
        weight,
        dexMod,
        mt,
        bonus,
        mythic,
        legendary,
        lairActions,
        regionalEffects,
    }
}
