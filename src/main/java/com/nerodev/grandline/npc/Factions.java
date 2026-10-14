package com.nerodev.grandline.npc;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.definition.FactionDefinition;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * How the factions of the sea regard players and each other, and how deeds change a player's standing.
 * <p>
 * A faction's people are hostile to a player whose standing with them has sunk to {@link #HOSTILE}, who belongs to
 * one of their enemy factions, or whose bounty reaches the faction's {@code wanted_bounty}; they are friendly to
 * their own members (unless the member became a traitor) and to anyone whose standing reached {@link #FRIENDLY}.
 * Townsfolk are no faction, but they remember how they are treated under {@link #CIVILIANS}.
 */
public final class Factions {
    public static final int HOSTILE = -30;
    public static final int FRIENDLY = 30;
    /** A member whose standing falls this low is a traitor and is attacked by their own faction. */
    public static final int TRAITOR = -100;
    public static final ResourceLocation CIVILIANS = GrandLine.id("civilians");
    public static final ResourceLocation MARINES = GrandLine.id("marine");

    /** Standing lost with a faction for killing one of its people, and gained with each of its enemies. */
    public static final int KILL_PENALTY = 25;
    public static final int KILL_REWARD = 8;
    /** Standing lost for striking someone who was not hostile to you. */
    public static final int ASSAULT_PENALTY = 3;
    /** Standing lost with the townsfolk (and the Marines) for killing a civilian. */
    public static final int MURDER_PENALTY = 40;
    /** How far a cry for help carries. */
    public static final double ALARM_RANGE = 24;

    private Factions() {}

    public static Optional<FactionDefinition> definition(RegistryAccess access, ResourceLocation id) {
        return access.registry(GLRegistries.FACTION).map(registry -> registry.get(id));
    }

    public static TextColor color(RegistryAccess access, Optional<ResourceLocation> faction) {
        return faction.flatMap(id -> definition(access, id)).map(FactionDefinition::color).orElse(TextColor.fromRgb(0xF2EEDC));
    }

    public static boolean member(Player player, ResourceLocation faction) {
        return GLPlayer.data(player).faction().map(faction::equals).orElse(false);
    }

    /** How the people of {@code faction} (or the townsfolk, if empty) regard {@code player}. */
    public static Reaction reaction(Player player, Optional<ResourceLocation> faction) {
        PlayerData data = GLPlayer.data(player);
        if (faction.isEmpty()) {
            int standing = data.reputation(CIVILIANS);
            return standing <= HOSTILE ? Reaction.HOSTILE : standing >= FRIENDLY ? Reaction.FRIENDLY : Reaction.NEUTRAL;
        }
        ResourceLocation id = faction.get();
        int standing = data.reputation(id);
        if (data.faction().map(id::equals).orElse(false)) {
            return standing <= TRAITOR ? Reaction.HOSTILE : Reaction.FRIENDLY;
        }
        if (standing <= HOSTILE) {
            return Reaction.HOSTILE;
        }
        Optional<FactionDefinition> def = definition(player.level().registryAccess(), id);
        if (def.isPresent()) {
            if (data.faction().map(def.get()::isEnemy).orElse(false)) {
                return Reaction.HOSTILE;
            }
            if (def.get().wantedBounty().map(wanted -> data.bounty() >= wanted).orElse(false)) {
                return Reaction.HOSTILE;
            }
        }
        return standing >= FRIENDLY ? Reaction.FRIENDLY : Reaction.NEUTRAL;
    }

    /** Whether people of these two factions fight on sight (either names the other as an enemy). */
    public static boolean enemies(RegistryAccess access, Optional<ResourceLocation> a, Optional<ResourceLocation> b) {
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
            return false;
        }
        return definition(access, a.get()).map(def -> def.isEnemy(b.get())).orElse(false)
                || definition(access, b.get()).map(def -> def.isEnemy(a.get())).orElse(false);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Deeds
    // ---------------------------------------------------------------------------------------------------------

    /** {@code player} struck {@code npc}: standing drops if it was not already a fight, and its people come running. */
    public static void onAssault(ServerPlayer player, NpcEntity npc) {
        boolean wasHostile = npc.reactionTo(player) == Reaction.HOSTILE;
        if (!wasHostile) {
            ResourceLocation key = npc.faction().orElse(CIVILIANS);
            GLPlayer.edit(player, data -> data.addReputation(key, -ASSAULT_PENALTY));
        }
        npc.provoke(player);
        alarm(npc, player);
    }

    /** {@code player} killed {@code npc}: the victim's people resent it and their enemies approve. */
    public static void onKill(ServerPlayer player, NpcEntity npc) {
        Optional<ResourceLocation> faction = npc.faction();
        RegistryAccess access = player.level().registryAccess();
        GLPlayer.edit(player, data -> {
            if (faction.isPresent()) {
                data.addReputation(faction.get(), -KILL_PENALTY);
                Optional<Registry<FactionDefinition>> registry = access.registry(GLRegistries.FACTION);
                registry.ifPresent(r -> {
                    for (Map.Entry<ResourceKey<FactionDefinition>, FactionDefinition> e : r.entrySet()) {
                        if (enemies(access, Optional.of(e.getKey().location()), faction)) {
                            data.addReputation(e.getKey().location(), KILL_REWARD);
                        }
                    }
                });
            } else {
                data.addReputation(CIVILIANS, -MURDER_PENALTY);
                data.addReputation(MARINES, -MURDER_PENALTY / 2);
            }
        });
        alarm(npc, player);
    }

    /** A completed trade warms a merchant's people to the customer a little. */
    public static void onTrade(ServerPlayer player, NpcEntity merchant) {
        ResourceLocation key = merchant.faction().orElse(CIVILIANS);
        if (GLPlayer.data(player).reputation(key) < FRIENDLY * 2) {
            GLPlayer.edit(player, data -> data.addReputation(key, 1));
        }
    }

    /**
     * Someone attacked {@code victim}: islanders of the same faction nearby turn on {@code attacker}; when a
     * civilian is the victim the Marines answer the call, and every civilian around remembers who did it.
     */
    public static void alarm(NpcEntity victim, LivingEntity attacker) {
        AABB area = victim.getBoundingBox().inflate(ALARM_RANGE);
        Optional<ResourceLocation> faction = victim.faction();
        for (NpcEntity other : victim.level().getEntitiesOfClass(NpcEntity.class, area, n -> n != victim && n.isAlive())) {
            boolean kin = other.faction().equals(faction);
            boolean law = faction.isEmpty() && other.faction().map(MARINES::equals).orElse(false);
            if (kin || law || other.faction().isEmpty() && !other.role().fighter()) {
                other.provoke(attacker);
            }
        }
    }

    /** A player was attacked: their own faction's fighters nearby come to their defence. */
    public static void defend(ServerPlayer player, LivingEntity attacker) {
        Optional<ResourceLocation> faction = GLPlayer.data(player).faction();
        if (faction.isEmpty() || attacker == player) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(ALARM_RANGE * 0.66);
        for (NpcEntity npc : player.level().getEntitiesOfClass(NpcEntity.class, area,
                n -> n.isAlive() && n != attacker && n.role().fighter() && n.faction().equals(faction))) {
            if (npc.reactionTo(player) == Reaction.FRIENDLY) {
                npc.provoke(attacker);
            }
        }
    }
}
