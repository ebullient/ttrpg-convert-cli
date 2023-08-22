package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools vehicle attributes ({@code vehicle2md.txt})
 * <p>
 * Several different types of vehicle use this template, including:
 * Ship, spelljammer, infernal war manchie, objects and creatures.
 * They can have very different properties. Treat most as optional.
 * </p>
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase}.
 * </p>
 */
@TemplateData
public class QuteVehicle extends Tools5eQuteBase {

    /** Vehicle type: Ship, Spelljammer, Infernal War Machine, Creature, Object */
    public final String vehicleType;
    /** Ship size and dimensions. Used by Ship, Infernal War Machine */
    public final String sizeDimension;
    /** Vehicle terrain as a comma-separated list (all) */
    public final String terrain;
    /**
     * Object ability scores as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores}
     * Used by Ship, Infernal War Machine, Creature, Object
     */
    public final AbilityScores scores;

    /** Vehicle immunities and resistances as {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist} */
    public final ImmuneResist immuneResist;

    /**
     * Ship capacity and pace attributes as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteVehicle.ShipCrewCargoPace}.
     */
    public final ShipCrewCargoPace shipCrewCargoPace;
    /** List of vehicle actions as a collection of {@link dev.ebullient.convert.qute.NamedText} */
    public final Collection<NamedText> action;
    /**
     * Ship sections and traits as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteVehicle.ShipAcHp} (hull, sails,
     * oars, .. )
     */
    public final List<ShipSection> shipSections;

    /** Token image as {@link dev.ebullient.convert.qute.ImageRef} */
    public final ImageRef token;
    /** List of {@link dev.ebullient.convert.qute.ImageRef} related to the creature */
    public final List<ImageRef> fluffImages;

    public QuteVehicle(CompendiumSources sources, String name, String source,
            String vehicleType, String terrain,
            AbilityScores scores, String size,
            ImmuneResist immuneResist,
            ShipCrewCargoPace shipCrewCargoPace,
            List<ShipSection> shipSections,
            Collection<NamedText> action,
            ImageRef token, List<ImageRef> fluffImages,
            String text, Tags tags) {
        super(sources, name, source, text, tags);

        this.vehicleType = vehicleType;
        this.terrain = terrain;

        this.scores = scores;
        this.sizeDimension = size;

        this.immuneResist = immuneResist;

        this.shipCrewCargoPace = shipCrewCargoPace;
        this.shipSections = shipSections;
        this.action = action;

        this.token = token;
        this.fluffImages = fluffImages;
    }

    /** True if this vehicle is a Ship */
    public boolean getIsShip() {
        return "SHIP".equals(vehicleType);
    }

    /** True if this vehicle is a Spelljammer */
    public boolean getIsSpelljammer() {
        return "SPELLJAMMER".equals(vehicleType);
    }

    /** True if this vehicle is an Infernal War Machine */
    public boolean getIsWarMachine() {
        return "INFWAR".equals(vehicleType);
    }

    /** True if this vehicle is a Creature */
    public boolean getIsCreature() {
        return "CREATURE".equals(vehicleType);
    }

    /** True if this vehicle is an Object */
    public boolean getIsObject() {
        return "OBJECT".equals(vehicleType);
    }

    /**
     * 5eTools Ship crew, cargo, and pace attributes
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly:<br />
     * ```<br />
     * {#if resource.shipCrewCargoPace}<br />
     * {resource.shipCrewCargoPace}<br />
     * {/if}<br />
     * ```<br />
     * </p>
     */
    @TemplateData
    public static class ShipCrewCargoPace implements QuteUtil {
        final String vehicleType;
        /** Crew capacity (number) */
        public String crew;
        /** Additional crew notes */
        public String crewText;
        /** Passenger capacity (number) */
        public String passenger;
        /** Cargo capacity (string) */
        public String cargo;
        /** Spelljammer or Infernal War Machine HP/AC */
        public ShipAcHp acHp;
        /** Spelljammer Keel/Beam */
        public String keelBeam;
        /**
         * Ship pace (number, mph)
         * Ship speed is pace * 10 (*Special Travel Pace*, DMG p242).
         */
        public Integer shipPace;
        /**
         * Spelljammer speed and pace (preformatted string)
         */
        public String speedPace;

        public ShipCrewCargoPace(String vehicleType, String crew, String crewText, String passenger,
                String cargo, Integer pace, String speedPace, ShipAcHp acHp,
                String keelBeam) {
            this.vehicleType = vehicleType;
            this.crew = crew;
            this.crewText = crewText;
            this.passenger = passenger;
            this.cargo = cargo;
            this.shipPace = pace;
            this.speedPace = speedPace;
            this.acHp = acHp;
            this.keelBeam = keelBeam;
        }

        public String toString() {
            List<String> out = new ArrayList<>();
            String crewPresent = isPresent(crew)
                    ? crew + (isPresent(crewText) ? " " + crewText : "")
                    : "\u2014";

            if ("SPELLJAMMER".equals(vehicleType)) {
                // This is a spelljammer
                String acText = isPresent(acHp.acText) ? " (" + acHp.acText + ")" : "";
                out.add("- **Armor Class** " + (isPresent(acHp.ac) ? (acHp.ac + acText) : "\u2014"));
                out.add("- **Hit Points** " + (isPresent(acHp.hp) ? acHp.hp : "\u2014"));
                out.add("- **Damage Threshold** " + (isPresent(acHp.dt) ? acHp.dt : "\u2014"));
                out.add("- **Speed** " + speedPace);
                out.add("- **Cargo** " + (isPresent(cargo) ? cargo + makePlural(" ton", cargo) : "\u2014"));
                out.add("- **Crew** " + crewPresent);
                out.add("- **Keel/Beam** " + (isPresent(keelBeam) ? keelBeam : "\u2014"));
                out.add("- **Cost** " + (isPresent(acHp.cost) ? acHp.cost : "\u2014"));
            } else {
                if (isPresent(crew) || isPresent(passenger)) {
                    List<String> inner = new ArrayList<>();
                    if (isPresent(crew)) {
                        inner.add(crewPresent);
                    }
                    if (isPresent(passenger)) {
                        inner.add(passenger + makePlural(" passenger", passenger));
                    }
                    out.add("- **Creature Capacity** " + String.join(", ", inner));
                }
                if (isPresent(cargo)) {
                    out.add("- **Cargo Capacity** " + cargo);
                }
                if (isPresent(acHp)) {
                    out.add(acHp.toString());
                }
                if ("SHIP".equals(vehicleType) && isPresent(shipPace)) {
                    out.add("- **Travel Pace** "
                            + shipPace + " miles per hour ("
                            + (shipPace * 24) + " miles per day)");
                    out.add("- *Speed* " + (shipPace * 10) + " ft. ^[Based on _Special Travel Pace_, DMG p242]");
                } else if (isPresent(speedPace)) {
                    if ("INFWAR".equals(vehicleType)) {
                        int s = Integer.parseInt(speedPace.replace(" ft.", ""));
                        int pace = s / 10;
                        int day = (s * 24) / 10;
                        out.add("- **Speed** " + s + " ft.\n"
                                + "- *Travel Pace* "
                                + pace + " miles per hour ("
                                + day + " miles per day) ^[Based on _Special Travel Pace_, DMG p242]");
                    } else {
                        out.add("- **Speed** " + speedPace);
                    }
                }
            }

            return String.join("\n", out);
        }
    }

    /**
     * 5eTools vehicle armor class and hit points attributes
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly.
     * </p>
     */
    @TemplateData
    public static class ShipAcHp extends AcHp {
        final String vehicleType;

        /** Damage threshold; number */
        public Integer dt;
        /** Infernal War Machine mishap threshold; number */
        public Integer mt;
        /** Cost (per unit); preformatted string */
        public String cost;

        public ShipAcHp(String type, Integer ac, String acText, Integer hp, String hpText, Integer dt, Integer mt,
                String cost) {
            super(ac, acText, hp, hpText, null);
            this.vehicleType = type;
            this.dt = dt;
            this.mt = mt;
            this.cost = cost;
        }

        public ShipAcHp(String type, AcHp creatureAcHp) {
            super(creatureAcHp);
            this.vehicleType = type;
        }

        public String toString() {
            if ("CREATURE".equals(vehicleType)) {
                return super.toString();
            }
            List<String> out = new ArrayList<>();
            if (isPresent(ac)) {
                out.add("- **Armor Class** " + ac + (isPresent(acText) ? " (" + acText + ")" : ""));
            }
            if (isPresent(hp)) {
                List<String> threshold = new ArrayList<>();
                if (isPresent(dt)) {
                    threshold.add("damage threshold " + dt);
                }
                if (isPresent(mt)) {
                    threshold.add("mishap threshold " + mt);
                }
                String thresholdText = String.join(", ", threshold);

                out.add("- **Hit Points** " + hp
                        + (isPresent(thresholdText) ? " (" + thresholdText + ")" : "")
                        + (hpText != null ? "; " + hpText : ""));
            }
            if (isPresent(cost)) {
                out.add("- **Cost** " + cost);
            }
            return String.join("\n", out);
        }
    }

    /**
     * 5eTools vehicle sections
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly.
     * </p>
     */
    @TemplateData
    public static class ShipSection implements QuteUtil {
        /** Name */
        public String name;
        /** Armor class and hit points as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteVehicle.ShipAcHp} */
        public ShipAcHp acHp;
        /** Speed as a list of pre-formatted strings */
        public List<String> speed;
        /** Pre-formatted text description */
        public List<String> desc;
        /** Pre-formatted actions */
        public List<String> actions;

        public ShipSection(String name, ShipAcHp acHp, List<String> speed, List<String> desc, List<String> actions) {
            this.name = name;
            this.acHp = acHp;
            this.speed = speed;
            this.desc = desc;
            this.actions = actions;
        }

        public String toString() {
            List<String> out = new ArrayList<>();

            out.add("## " + name);
            out.add("");
            if (isPresent(acHp)) {
                out.add(acHp.toString());
            }
            if (isPresent(speed)) {
                speed.forEach(s -> out.add("- " + s));
            }
            if (isPresent(desc)) {
                desc.forEach(d -> {
                    out.add("");
                    out.add(d);
                });
            }
            if (isPresent(actions)) {
                actions.forEach(a -> {
                    out.add("");
                    out.add(a);
                });
            }
            return String.join("\n", out)
                    .replaceAll("\n{3,}", "\n\n");
        }
    }
}
