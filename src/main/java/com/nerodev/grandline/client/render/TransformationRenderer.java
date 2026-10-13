package com.nerodev.grandline.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nerodev.grandline.client.fx.FxKit;
import com.nerodev.grandline.client.model.BeastModel;
import com.nerodev.grandline.client.model.LeopardModel;
import com.nerodev.grandline.client.model.ZoanLooks;
import com.nerodev.grandline.data.GLAttachments;
import com.nerodev.grandline.data.PublicState;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.fx.GLParticles;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.Transformation;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client side of transformations: swaps the player model for the leopard in beast form, hides first-person hands
 * when they are paws, keeps hitboxes in sync when a transformation changes, and emits ambient particles
 * (Gear Second steam, Gear Fifth clouds, awakened flame embers).
 */
public final class TransformationRenderer {
    @Nullable
    private static LeopardModel leopard;
    private static final Map<Integer, Optional<ResourceLocation>> LAST_SEEN = new HashMap<>();

    private TransformationRenderer() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RenderPlayerEvent.Pre.class, TransformationRenderer::onRenderPlayer);
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, TransformationRenderer::onRenderHand);
    }

    private static boolean isBeast(Player player) {
        return player.hasData(GLAttachments.PUBLIC_STATE)
                && TransformationManager.isActive(player, GLTransformations.LEOPARD_BEAST.get());
    }

    /** The Zoan beast body the player is in, if any (the leopard has its own model above). */
    @Nullable
    private static ZoanLooks.Beast zoanBeast(Player player) {
        if (!player.hasData(GLAttachments.PUBLIC_STATE)) {
            return null;
        }
        return PublicState.of(player).transformation().map(ZoanLooks::beast).orElse(null);
    }

    private static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || player.isInvisible()) {
            return;
        }
        ZoanLooks.Beast zoan = zoanBeast(player);
        if (zoan != null) {
            event.setCanceled(true);
            renderZoan(event, player, zoan);
            return;
        }
        if (!isBeast(player)) {
            return;
        }
        event.setCanceled(true);
        if (leopard == null) {
            leopard = new LeopardModel(Minecraft.getInstance().getEntityModels().bakeLayer(LeopardModel.LAYER));
        }
        float partial = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        pose.scale(-1f, -1f, 1f);
        pose.translate(0f, -1.501f, 0f);
        float headYaw = Mth.rotLerp(partial, player.yHeadRotO, player.yHeadRot) - bodyYaw;
        float pitch = Mth.lerp(partial, player.xRotO, player.getXRot());
        leopard.setupAnim(player, player.walkAnimation.position(partial), player.walkAnimation.speed(partial),
                player.tickCount + partial, headYaw, pitch);
        leopard.renderToBuffer(pose, event.getMultiBufferSource().getBuffer(leopard.renderType(TransformationLayer.LEOPARD_TEXTURE)),
                event.getPackedLight(), LivingEntityRenderer.getOverlayCoords(player, 0f), 0xFFFFFFFF);
        pose.popPose();
    }

    /** Draws a Zoan beast in place of the player, facing the body's yaw, animated from the player's movement. */
    private static void renderZoan(RenderPlayerEvent.Pre event, AbstractClientPlayer player, ZoanLooks.Beast zoan) {
        BeastModel model = ZoanLooks.model(zoan.shape());
        float partial = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        pose.scale(-1f, -1f, 1f);
        float scale = zoan.shape().scale();
        pose.scale(scale, scale, scale);
        pose.translate(0f, -1.501f, 0f);
        float headYaw = Mth.wrapDegrees(Mth.rotLerp(partial, player.yHeadRotO, player.yHeadRot) - bodyYaw);
        float pitch = Mth.lerp(partial, player.xRotO, player.getXRot());
        model.attackTime = player.getAttackAnim(partial);
        model.setupAnim(player, player.walkAnimation.position(partial), player.walkAnimation.speed(partial),
                player.tickCount + partial, headYaw, pitch);
        int light = zoan.glowing() ? LightTexture.FULL_BRIGHT : event.getPackedLight();
        model.renderToBuffer(pose, event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(zoan.shape().texture())),
                light, LivingEntityRenderer.getOverlayCoords(player, 0f), 0xFFFFFFFF);
        pose.popPose();
    }

    private static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && (isBeast(player) || zoanBeast(player) != null)) {
            event.setCanceled(true);
        }
    }

    /** Once per client tick for every player in view. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            LAST_SEEN.clear();
            leopard = null;
            ZoanLooks.clearModels();
            return;
        }
        RandomSource random = mc.level.random;
        for (Player player : mc.level.players()) {
            if (!player.hasData(GLAttachments.PUBLIC_STATE)) {
                continue;
            }
            PublicState state = PublicState.of(player);
            Optional<ResourceLocation> now = state.transformation();
            Optional<ResourceLocation> before = LAST_SEEN.put(player.getId(), now);
            if (before != null && !before.equals(now)) {
                player.refreshDimensions();
            }
            ambient(player, state, random);
        }
        if (mc.level.getGameTime() % 200 == 0) {
            LAST_SEEN.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        }
    }

    private static void ambient(Player player, PublicState state, RandomSource random) {
        if (player.isInvisible() || player.isSpectator()) {
            return;
        }
        Transformation active = TransformationManager.active(player);
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.55, 0);
        var level = Minecraft.getInstance().level;
        if (active == GLTransformations.GEAR_SECOND.get() && player.tickCount % 2 == 0) {
            Vec3 at = center.add((random.nextDouble() - 0.5) * 0.7, (random.nextDouble() - 0.5) * 1.2, (random.nextDouble() - 0.5) * 0.7);
            FxKit.particle(level, GLParticles.SMOKE, 0xF2E6E8, 0.45f, at, new Vec3(0, 0.06, 0));
        } else if (active == GLTransformations.GEAR_FIFTH.get() && player.tickCount % 2 == 0) {
            Vec3 at = center.add((random.nextDouble() - 0.5) * 1.4, random.nextDouble() * 1.4 - 0.4, (random.nextDouble() - 0.5) * 1.4);
            FxKit.particle(level, GLParticles.GLOW, 0xFFFFFF, 0.9f, at, new Vec3(0, 0.02, 0));
            if (random.nextInt(6) == 0) {
                FxKit.particle(level, GLParticles.STAR, 0xFFF8D0, 0.8f, at, Vec3.ZERO);
            }
        }
        if (state.awakened() && state.fruit().map(id -> id.equals(GLFruits.FLAME.getId())).orElse(false)
                && player.tickCount % 4 == 0) {
            Vec3 at = player.position().add((random.nextDouble() - 0.5) * 0.8, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.8);
            FxKit.particle(level, GLParticles.FLAME, 0xFF6A1A, 0.6f, at, new Vec3(0, 0.04, 0));
        }
    }
}
