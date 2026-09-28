package com.grandline;

import com.grandline.command.OnePieceCommand;
import com.grandline.data.PlayerDataCapability;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod("grandline")
public class OnePieceMod {
    public static final String MODID = "grandline";
    public OnePieceMod(IEventBus modBus) {
        PlayerDataCapability.register(modBus);
        NeoForge.EVENT_BUS.addListener(OnePieceCommand::register);
    }
}