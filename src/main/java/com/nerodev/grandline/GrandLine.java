package com.nerodev.grandline;

import com.mojang.logging.LogUtils;
import com.nerodev.grandline.command.OnePieceCommand;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.registry.GLRegistries;
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

        // Commands.
        NeoForge.EVENT_BUS.addListener(OnePieceCommand::onRegisterCommands);

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
