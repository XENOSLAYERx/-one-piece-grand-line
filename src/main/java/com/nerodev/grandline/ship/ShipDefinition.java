package com.nerodev.grandline.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.phys.Vec3;

/**
 * A kind of ship, loaded from {@code data/<namespace>/grandline/ship/<id>.json}. Everything about it is data: its
 * size, how fast and nimble it is, how much punishment the hull takes, its cannons, crew, hold and colours. The
 * model is built from these numbers, so a new ship needs only a JSON file.
 *
 * @param length       bow to stern, in blocks
 * @param width        beam, in blocks
 * @param hullHeight   keel to deck, in blocks
 * @param draft        how deep the keel sits below the waterline
 * @param masts        number of masts (0 for none)
 * @param mastHeight   mast height above the deck
 * @param maxHealth    hull health
 * @param maxSpeed     top speed in blocks per tick
 * @param acceleration speed gained per tick at full sail
 * @param turnRate     degrees turned per tick at speed
 * @param cannons      cannons on each side
 * @param cannonReload ticks between broadsides on one side
 * @param cannonDamage damage of each cannonball
 * @param crew         people aboard (the first stands at the helm)
 * @param storageRows  rows of cargo (0-6)
 * @param burst        has a Coup de Burst (a cola-powered leap forward)
 */
public record ShipDefinition(Component displayName, Component description, float length, float width, float hullHeight, float draft,
        int masts, float mastHeight, float maxHealth, float maxSpeed, float acceleration, float turnRate, int cannons, int cannonReload,
        float cannonDamage, int crew, int storageRows, SailStyle sail, int hullColor, int trimColor, int sailColor, boolean castle,
        boolean burst) {

    public static final Codec<ShipDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("display_name").forGetter(ShipDefinition::displayName),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(ShipDefinition::description),
            RecordCodecBuilder.<ShipDefinition.Size>mapCodec(s -> s.group(
                    Codec.floatRange(2, 40).fieldOf("length").forGetter(Size::length),
                    Codec.floatRange(1.5f, 12).fieldOf("width").forGetter(Size::width),
                    Codec.floatRange(0.3f, 6).fieldOf("hull_height").forGetter(Size::hullHeight),
                    Codec.floatRange(0, 3).optionalFieldOf("draft", 0.4f).forGetter(Size::draft),
                    Codec.intRange(0, 4).optionalFieldOf("masts", 1).forGetter(Size::masts),
                    Codec.floatRange(0, 30).optionalFieldOf("mast_height", 6f).forGetter(Size::mastHeight),
                    Codec.BOOL.optionalFieldOf("castle", false).forGetter(Size::castle)
            ).apply(s, Size::new)).forGetter(Size::of),
            RecordCodecBuilder.<ShipDefinition.Handling>mapCodec(h -> h.group(
                    Codec.floatRange(1, 5000).fieldOf("max_health").forGetter(Handling::maxHealth),
                    Codec.floatRange(0.01f, 2).fieldOf("max_speed").forGetter(Handling::maxSpeed),
                    Codec.floatRange(0.0005f, 0.1f).fieldOf("acceleration").forGetter(Handling::acceleration),
                    Codec.floatRange(0.1f, 20).fieldOf("turn_rate").forGetter(Handling::turnRate),
                    Codec.BOOL.optionalFieldOf("burst", false).forGetter(Handling::burst)
            ).apply(h, Handling::new)).forGetter(Handling::of),
            RecordCodecBuilder.<ShipDefinition.Arms>mapCodec(a -> a.group(
                    Codec.intRange(0, 8).optionalFieldOf("cannons", 0).forGetter(Arms::cannons),
                    Codec.intRange(5, 1200).optionalFieldOf("cannon_reload", 60).forGetter(Arms::cannonReload),
                    Codec.floatRange(0, 200).optionalFieldOf("cannon_damage", 14f).forGetter(Arms::cannonDamage),
                    Codec.intRange(1, 16).optionalFieldOf("crew", 2).forGetter(Arms::crew),
                    Codec.intRange(0, 6).optionalFieldOf("storage_rows", 1).forGetter(Arms::storageRows)
            ).apply(a, Arms::new)).forGetter(Arms::of),
            RecordCodecBuilder.<ShipDefinition.Look>mapCodec(l -> l.group(
                    SailStyle.CODEC.optionalFieldOf("sail", SailStyle.PLAIN).forGetter(Look::sail),
                    Codec.INT.optionalFieldOf("hull_color", 0x8A6A44).forGetter(Look::hullColor),
                    Codec.INT.optionalFieldOf("trim_color", 0x4A3424).forGetter(Look::trimColor),
                    Codec.INT.optionalFieldOf("sail_color", 0xFFFFFF).forGetter(Look::sailColor)
            ).apply(l, Look::new)).forGetter(Look::of)
    ).apply(i, (name, description, size, handling, arms, look) -> new ShipDefinition(name, description, size.length(), size.width(),
            size.hullHeight(), size.draft(), size.masts(), size.mastHeight(), handling.maxHealth(), handling.maxSpeed(),
            handling.acceleration(), handling.turnRate(), arms.cannons(), arms.cannonReload(), arms.cannonDamage(), arms.crew(),
            arms.storageRows(), look.sail(), look.hullColor(), look.trimColor(), look.sailColor(), size.castle(), handling.burst())));

    record Size(float length, float width, float hullHeight, float draft, int masts, float mastHeight, boolean castle) {
        static Size of(ShipDefinition d) {
            return new Size(d.length, d.width, d.hullHeight, d.draft, d.masts, d.mastHeight, d.castle);
        }
    }

    record Handling(float maxHealth, float maxSpeed, float acceleration, float turnRate, boolean burst) {
        static Handling of(ShipDefinition d) {
            return new Handling(d.maxHealth, d.maxSpeed, d.acceleration, d.turnRate, d.burst);
        }
    }

    record Arms(int cannons, int cannonReload, float cannonDamage, int crew, int storageRows) {
        static Arms of(ShipDefinition d) {
            return new Arms(d.cannons, d.cannonReload, d.cannonDamage, d.crew, d.storageRows);
        }
    }

    record Look(SailStyle sail, int hullColor, int trimColor, int sailColor) {
        static Look of(ShipDefinition d) {
            return new Look(d.sail, d.hullColor, d.trimColor, d.sailColor);
        }
    }

    /** How many collision segments the hull is made of, bow to stern. */
    public int segments() {
        return Math.max(1, Math.min(ShipEntity.MAX_PARTS, (int) Math.ceil(length / width)));
    }

    /**
     * Where each crew member stands, in ship space (x to port, y up from the keel, z toward the bow). The first is the
     * helm at the stern; the rest spread along the deck in two files.
     */
    public List<Vec3> seats() {
        List<Vec3> seats = new ArrayList<>();
        float deck = hullHeight + (castle ? 1.4f : 0f);
        seats.add(new Vec3(0, deck, -length / 2 + Math.min(1.6f, length * 0.2f)));
        int others = crew - 1;
        float from = -length / 2 + Math.min(3.2f, length * 0.35f);
        float to = length / 2 - Math.min(1.6f, length * 0.2f);
        int rows = (others + 1) / 2;
        for (int i = 0; i < others; i++) {
            int row = i / 2;
            float z = rows <= 1 ? (from + to) / 2 : from + (to - from) * row / (rows - 1);
            float x = others == 1 ? 0 : (i % 2 == 0 ? 1 : -1) * Math.max(0.5f, width / 4);
            seats.add(new Vec3(x, hullHeight, z));
        }
        return seats;
    }

    /** Positions of the cannons on one side, in ship space; side 1 is port, -1 starboard. */
    public List<Vec3> cannonPositions(int side) {
        List<Vec3> out = new ArrayList<>();
        float span = length * 0.55f;
        for (int i = 0; i < cannons; i++) {
            float z = cannons == 1 ? 0 : -span / 2 + span * i / (cannons - 1);
            out.add(new Vec3(side * (width / 2 + 0.25), hullHeight * 0.62, z));
        }
        return out;
    }
}
