package com.nerodev.grandline.client;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.client.anim.AnimationLoader;
import com.nerodev.grandline.client.anim.PlayerAnimator;
import com.nerodev.grandline.client.combat.ClientCombat;
import com.nerodev.grandline.client.combat.CombatHud;
import com.nerodev.grandline.client.dev.ClientAutoTest;
import com.nerodev.grandline.client.fruit.LogiaLooks;
import com.nerodev.grandline.client.fruit.ParameciaLooksA;
import com.nerodev.grandline.client.fruit.ParameciaLooksB;
import com.nerodev.grandline.client.fruit.ParameciaLooksC;
import com.nerodev.grandline.client.fruit.ParameciaLooksD;
import com.nerodev.grandline.client.fruit.ParameciaLooksE;
import com.nerodev.grandline.client.fruit.ParameciaLooksF;
import com.nerodev.grandline.client.fruit.ParameciaLooksG;
import com.nerodev.grandline.client.fruit.TechniqueLooks;
import com.nerodev.grandline.client.fruit.ZoanProjectileLooks;
import com.nerodev.grandline.client.fx.CameraShake;
import com.nerodev.grandline.client.fx.FruitEffects;
import com.nerodev.grandline.client.fx.GenericEffects;
import com.nerodev.grandline.client.haki.HakiClient;
import com.nerodev.grandline.client.hud.AbilityHud;
import com.nerodev.grandline.client.model.TransformationModels;
import com.nerodev.grandline.client.particle.GLParticleProviders;
import com.nerodev.grandline.client.render.AbilityProjectileRenderer;
import com.nerodev.grandline.client.render.DisguiseRenderer;
import com.nerodev.grandline.client.render.HakiLayer;
import com.nerodev.grandline.client.render.ProjectileVisuals;
import com.nerodev.grandline.client.render.TransformationLayer;
import com.nerodev.grandline.client.render.TransformationRenderer;
import com.nerodev.grandline.client.visual.ClientVisuals;
import com.nerodev.grandline.client.visual.StatusVisuals;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.GLEntities;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only entry point: input, HUD, particles, effects, body animation and transformation rendering. */
@Mod(value = GrandLine.MODID, dist = Dist.CLIENT)
public final class GrandLineClient {
    public GrandLineClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(GLKeys::register);
        modBus.addListener(AbilityHud::register);
        modBus.addListener(CombatHud::register);
        modBus.addListener(HakiClient::registerOverlay);
        modBus.addListener(GLParticleProviders::register);
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener(AnimationLoader.INSTANCE));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerEntityRenderer(GLEntities.ABILITY_PROJECTILE.get(), AbilityProjectileRenderer::new));
        modBus.addListener(TransformationModels::registerLayers);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, GrandLineClient::addLayers);

        FruitEffects.register();
        GenericEffects.register();
        StatusVisuals.register();
        HakiClient.register();
        TransformationRenderer.register();
        DisguiseRenderer.register();
        LogiaLooks.install();
        ParameciaLooksA.install();
        ParameciaLooksB.install();
        ParameciaLooksC.install();
        ParameciaLooksD.install();
        ParameciaLooksE.install();
        ParameciaLooksF.install();
        ParameciaLooksG.install();
        ZoanProjectileLooks.install();
        TechniqueLooks.install();
        AbilityProjectile.clientTick = ProjectileVisuals::tick;

        boolean autotest = ClientAutoTest.init();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
            GLKeys.onClientTick();
            PlayerAnimator.tick();
            CameraShake.tick();
            ClientVisuals.tick();
            TransformationRenderer.tick();
            DisguiseRenderer.tick();
            HakiClient.tick();
            StatusVisuals.tick();
            if (autotest) {
                ClientAutoTest.tick();
            }
        });
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, CameraShake::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, ClientVisuals::onRenderStage);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            PlayerAnimator.clear();
            ClientVisuals.clear();
            HakiClient.clear();
            StatusVisuals.clear();
            ClientCombat.reset();
        });
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new TransformationLayer(renderer, event.getEntityModels(), skin == PlayerSkin.Model.SLIM));
                renderer.addLayer(new HakiLayer(renderer));
            }
        }
    }
}
