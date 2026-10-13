package com.nerodev.grandline;

import com.mojang.logging.LogUtils;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.combat.CombatManager;
import com.nerodev.grandline.command.OnePieceCommand;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.DeathModeHandler;
import com.nerodev.grandline.data.GLAttachments;
import com.nerodev.grandline.dev.AutoTestServer;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.GLEntities;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Disguise;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.kit.TempBlocks;
import com.nerodev.grandline.fx.GLParticles;
import com.nerodev.grandline.haki.HakiAbilities;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.item.GLItems;
import com.nerodev.grandline.network.GLNetwork;
import com.nerodev.grandline.registry.GLRegistries;
import com.nerodev.grandline.sound.GLSounds;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.TransformationManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Entry point for One Piece: Grand Line.
 * <p>
 * Each system wires itself up here through static listener methods so the bootstrap stays readable as
 * milestones add more systems.
 */
@Mod(GrandLine.MODID)
public final class GrandLine {
    public static final String MODID = "grandline";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GrandLine(IEventBus modBus, ModContainer container) {
        // Config: per-world server config (lives in <world>/serverconfig/grandline-server.toml).
        container.registerConfig(ModConfig.Type.SERVER, GLServerConfig.SPEC);
        modBus.addListener(GLServerConfig::onLoad);
        modBus.addListener(GLServerConfig::onReload);

        // Data-driven registries (JSON under data/<namespace>/grandline/<registry>/).
        modBus.addListener(GLRegistries::onNewDataPackRegistries);

        // Player data (attachments) and death handling.
        GLAttachments.register(modBus);
        DeathModeHandler.register();

        // Abilities, effects and networking.
        GLAbilities.register(modBus);
        GLParticles.register(modBus);
        GLSounds.register(modBus);
        GLMobEffects.register(modBus);
        AbilityManager.register();
        GLMobEffects.registerRules();
        modBus.addListener(GLNetwork::onRegisterPayloads);

        // Devil Fruits, transformations, entities and items.
        GLFruits.register(modBus);
        GLTransformations.register(modBus);
        GLEntities.register(modBus);
        GLItems.register(modBus);
        FruitManager.register();
        TransformationManager.register();
        TempBlocks.register();
        Minions.register();
        Disguise.register();
        Buffs.register();

        // Haki.
        HakiAbilities.register(modBus);
        HakiManager.register();
        CombatManager.register();

        // Commands.
        NeoForge.EVENT_BUS.addListener(OnePieceCommand::onRegisterCommands);

        // CI only (inactive unless a system property is set).
        AutoTestServer.register();

        LOGGER.info("One Piece: Grand Line {} initialised", version());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static String version() {
        return ModList.get().getModContainerById(MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
