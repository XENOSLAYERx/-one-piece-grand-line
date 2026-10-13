package com.nerodev.grandline.client;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.client.anim.AnimationLoader;
import com.nerodev.grandline.client.dev.ClientAutoTest;
import com.nerodev.grandline.client.anim.PlayerAnimator;
import com.nerodev.grandline.client.fx.CameraShake;
import com.nerodev.grandline.client.hud.AbilityHud;
import com.nerodev.grandline.client.particle.GLParticleProviders;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only entry point: input, HUD, particles, effects and body animation. */
@Mod(value = GrandLine.MODID, dist = Dist.CLIENT)
public final class GrandLineClient {
    public GrandLineClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(GLKeys::register);
        modBus.addListener(AbilityHud::register);
        modBus.addListener(GLParticleProviders::register);
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener(AnimationLoader.INSTANCE));

        boolean autotest = ClientAutoTest.init();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
            GLKeys.onClientTick();
            PlayerAnimator.tick();
            CameraShake.tick();
            if (autotest) {
                ClientAutoTest.tick();
            }
        });
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, CameraShake::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> PlayerAnimator.clear());
    }
}
