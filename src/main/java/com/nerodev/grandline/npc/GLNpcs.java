package com.nerodev.grandline.npc;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.bounty.Bounties;
import com.nerodev.grandline.crew.Crews;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Islanders: the NPC entity, their musket balls, and the events that turn deeds into standing and alarms. */
public final class GLNpcs {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GrandLine.MODID);

    public static final Supplier<EntityType<NpcEntity>> NPC = ENTITIES.register("npc",
            () -> EntityType.Builder.<NpcEntity>of(NpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("npc"));
    public static final Supplier<EntityType<NpcShot>> NPC_SHOT = ENTITIES.register("npc_shot",
            () -> EntityType.Builder.<NpcShot>of(NpcShot::new, MobCategory.MISC)
                    .sized(0.2f, 0.2f)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("npc_shot"));

    // The islanders who live on the islands (see IslandBuilder).
    public static final ResourceLocation VILLAGER = GrandLine.id("villager");
    public static final ResourceLocation TOWNSPERSON = GrandLine.id("townsperson");
    public static final ResourceLocation BARKEEPER = GrandLine.id("barkeeper");
    public static final ResourceLocation MARINE_RECRUIT = GrandLine.id("marine_recruit");
    public static final ResourceLocation MARINE_RIFLEMAN = GrandLine.id("marine_rifleman");
    public static final ResourceLocation MARINE_LIEUTENANT = GrandLine.id("marine_lieutenant");
    public static final ResourceLocation MARINE_QUARTERMASTER = GrandLine.id("marine_quartermaster");
    public static final ResourceLocation FISHMAN_PIRATE = GrandLine.id("fishman_pirate");
    public static final ResourceLocation TANGERINE_FARMER = GrandLine.id("tangerine_farmer");
    public static final ResourceLocation TRAVELLING_MERCHANT = GrandLine.id("travelling_merchant");

    private GLNpcs() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(GLNpcs::onAttributes);
        NeoForge.EVENT_BUS.addListener(GLNpcs::onDamaged);
        NeoForge.EVENT_BUS.addListener(GLNpcs::onDeath);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(NPC.get(), NpcEntity.createAttributes().build());
    }

    /** Striking an islander costs standing and raises the alarm; striking a player calls their faction to help. */
    private static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) {
            return;
        }
        if (victim instanceof NpcEntity npc) {
            if (attacker instanceof ServerPlayer player) {
                boolean unprovoked = npc.reactionTo(player) != Reaction.HOSTILE;
                Factions.onAssault(player, npc);
                if (unprovoked) {
                    Bounties.onAssault(player, npc);
                }
            } else {
                Factions.alarm(npc, attacker);
            }
        } else if (victim instanceof ServerPlayer player) {
            Factions.defend(player, attacker);
        }
        // A crew fights together: recruits answer when a crewmate is attacked or picks a fight.
        Crews.rally(victim, attacker);
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (victim instanceof NpcEntity npc) {
            boolean wasHostile = npc.reactionTo(player) == Reaction.HOSTILE;
            Factions.onKill(player, npc);
            Bounties.onKill(player, npc);
            if (wasHostile && npc.role().fighter()) {
                Crews.addRenown(player, Math.max(1, (long) (npc.getMaxHealth() / 10)));
            }
        } else if (victim instanceof ServerPlayer slain && slain != player) {
            Bounties.onPlayerKill(player, slain);
        }
    }
}
