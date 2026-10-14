package com.nerodev.grandline.npc;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** How an islander spends their day. */
public enum NpcRole implements StringRepresentable {
    /** Walks a beat around their post, fights their faction's enemies and the monsters that threaten the town. */
    SOLDIER(true, true),
    /** Keeps close to their post and fights their faction's enemies. */
    GUARD(true, false),
    /** Wanders near home, runs from danger and calls for help. */
    CIVILIAN(false, false),
    /** Keeps shop near home (or travels far, by their wander radius), trades with anyone they don't fear. */
    MERCHANT(false, false);

    public static final Codec<NpcRole> CODEC = StringRepresentable.fromEnum(NpcRole::values);

    private final boolean fighter;
    private final boolean patrols;

    NpcRole(boolean fighter, boolean patrols) {
        this.fighter = fighter;
        this.patrols = patrols;
    }

    /** Fighters attack enemies; everyone else flees from them. */
    public boolean fighter() {
        return fighter;
    }

    /** Soldiers walk a beat; guards only shuffle around their post. */
    public boolean patrols() {
        return patrols;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
