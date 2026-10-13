package com.nerodev.grandline.client.haki;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.client.fx.FxKit;
import com.nerodev.grandline.client.render.HakiLayer;
import com.nerodev.grandline.client.visual.ClientVisuals;
import com.nerodev.grandline.client.visual.LightningVisual;
import com.nerodev.grandline.data.GLAttachments;
import com.nerodev.grandline.data.PublicState;
import com.nerodev.grandline.fx.GLParticles;
import com.nerodev.grandline.haki.HakiAbilities;
import com.nerodev.grandline.haki.HakiManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client side of Haki: what Observation reveals (outlines through walls, red for hostiles), Future Sight's
 * telegraphs of where enemies are about to move, knocked-out creatures toppling over, the first-person hardened
 * arm, ambient crackle on infused fighters and the screen-edge tint of Future Sight and Conqueror's pressure.
 */
public final class HakiClient {
    public static final ResourceLocation OVERLAY_ID = GrandLine.id("haki_overlay");
    private static final int KO_FALL_TICKS = 6;
    private static final int KO_RISE_TICKS = 10;

    /** Knocked-out entity id -> {start, end} in client game time. */
    private static final Map<Integer, long[]> KNOCKED_OUT = new HashMap<>();
    private static float pressure;
    private static float flash;

    private HakiClient() {}

    public static void register() {
        HakiEffects.register();
        NeoForge.EVENT_BUS.addListener(RenderArmEvent.class, HakiClient::onRenderArm);
    }

    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.HOTBAR, OVERLAY_ID, HakiClient::renderOverlay);
    }

    public static void clear() {
        KNOCKED_OUT.clear();
        pressure = 0;
        flash = 0;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Observation
    // ---------------------------------------------------------------------------------------------------------

    private static boolean sensing(Player self) {
        return AbilityState.of(self).isActive(HakiAbilities.OBSERVATION.getId());
    }

    /** True if Observation Haki lets the local player see {@code entity} through walls right now. */
    public static boolean senses(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        Player self = mc.player;
        if (self == null || entity == self || !mc.isSameThread() || !(entity instanceof LivingEntity)
                || entity instanceof ArmorStand || !entity.isAlive() || !sensing(self)) {
            return false;
        }
        double radius = HakiManager.senseRadius(self);
        return entity.distanceToSqr(self) <= radius * radius;
    }

    /** Outline colour of a sensed presence: hostile red, players cyan, everything else pale. */
    public static int senseColor(Entity entity) {
        if (entity instanceof Enemy) {
            return 0xFF3B3B;
        }
        return entity instanceof Player ? 0x7FD8FF : 0xDDEEFF;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Knock-outs
    // ---------------------------------------------------------------------------------------------------------

    static void knockOut(Entity entity, int ticks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            long now = level.getGameTime();
            KNOCKED_OUT.put(entity.getId(), new long[] {now, now + ticks});
        }
    }

    /** Called from the renderer mixin after body rotation: knocked-out creatures topple onto their side. */
    public static void applyKnockout(LivingEntity entity, PoseStack pose, float partialTick) {
        if (KNOCKED_OUT.isEmpty()) {
            return;
        }
        long[] span = KNOCKED_OUT.get(entity.getId());
        if (span == null) {
            return;
        }
        float now = entity.level().getGameTime() + partialTick;
        float fall = Mth.clamp((now - span[0]) / KO_FALL_TICKS, 0, 1);
        float rise = Mth.clamp((span[1] - now) / KO_RISE_TICKS, 0, 1);
        float t = Math.min(fall, rise);
        float eased = 1 - (1 - t) * (1 - t);
        pose.mulPose(Axis.ZP.rotationDegrees(85f * eased));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Ticking
    // ---------------------------------------------------------------------------------------------------------

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            clear();
            return;
        }
        long now = level.getGameTime();
        pressure = Math.max(0, pressure - 0.03f);
        flash = Math.max(0, flash - 0.08f);

        Iterator<Map.Entry<Integer, long[]>> it = KNOCKED_OUT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, long[]> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive() || now > entry.getValue()[1]) {
                it.remove();
            } else if (now % 8 == 0) {
                // Dizzy stars circling where the head lies.
                double a = now * 0.5;
                Vec3 head = entity.position().add(Math.cos(a) * 0.4, 0.5, Math.sin(a) * 0.4);
                FxKit.particle(level, GLParticles.STAR, 0xFFE68A, 0.5f, head, new Vec3(0, 0.02, 0));
            }
        }

        if (now % 3 == 0 && hasBit(mc.player, PublicState.HAKI_FUTURE_SIGHT)) {
            telegraph(level, mc.player);
        }

        for (Player player : level.players()) {
            if (player.isInvisible()) {
                continue;
            }
            int bits = HakiLayer.bits(player);
            if ((bits & PublicState.HAKI_INFUSION) != 0 && level.random.nextInt(6) == 0) {
                crackle(player);
            }
            if ((bits & PublicState.HAKI_FULL_BODY) != 0 && level.random.nextInt(10) == 0) {
                Vec3 at = player.position().add((level.random.nextDouble() - 0.5) * 0.7, level.random.nextDouble() * 1.8,
                        (level.random.nextDouble() - 0.5) * 0.7);
                FxKit.particle(level, GLParticles.STAR, 0xB49CFF, 0.5f, at, Vec3.ZERO);
            }
        }
    }

    private static boolean hasBit(Player player, int bit) {
        return player.hasData(GLAttachments.PUBLIC_STATE) && PublicState.of(player).hasHaki(bit);
    }

    /** Future Sight: a faint path from each nearby enemy to where it will be in about half a second. */
    private static void telegraph(ClientLevel level, Player self) {
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(self.blockPosition()).inflate(16),
                e -> e != self && e.isAlive() && !(e instanceof ArmorStand));
        int shown = 0;
        for (LivingEntity entity : near) {
            Vec3 motion = entity.getDeltaMovement();
            Vec3 from = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            Vec3 toward = self.position().add(0, self.getBbHeight() * 0.5, 0).subtract(from);
            // Hostiles that are not moving much are predicted to lunge at you.
            Vec3 predicted = motion.horizontalDistanceSqr() > 0.002 ? motion.scale(10)
                    : entity instanceof Enemy ? toward.normalize().scale(Math.min(2.5, toward.length())) : Vec3.ZERO;
            if (predicted.lengthSqr() < 0.05) {
                continue;
            }
            int color = entity instanceof Enemy ? 0xFF7A7A : 0xA8EEFF;
            FxKit.line(level, GLParticles.GLOW, color, 0.35f, from, from.add(predicted), 0.45, 0.0, 0.02);
            FxKit.particle(level, GLParticles.STAR, color, 0.7f, from.add(predicted), Vec3.ZERO);
            if (++shown >= 12) {
                break;
            }
        }
    }

    private static void crackle(Player player) {
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        boolean rightArm = player.getRandom().nextBoolean();
        Vec3 shoulder = player.position().add(0, player.getBbHeight() * 0.75, 0).add(right.scale(rightArm ? 0.35 : -0.35));
        Vec3 hand = shoulder.add(0, -0.6, 0);
        Vec3 out = hand.add(FxKit.randomUnit(player.getRandom()).scale(0.6 + player.getRandom().nextDouble() * 0.6));
        ClientVisuals.add(new LightningVisual(() -> hand, () -> out, 0xFF1A2A, 0x0A0004, 0.05f, 3, 0.3f, false));
    }

    /** Conqueror's Haki washing over the local player: a dark red pulse at the screen edges. */
    static void pressure(float strength) {
        pressure = Math.max(pressure, Mth.clamp(strength, 0, 1));
    }

    /** Future Sight activation: a pale flash. */
    static void flash(float strength) {
        flash = Math.max(flash, Mth.clamp(strength, 0, 1));
    }

    private static void renderOverlay(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (hasBit(mc.player, PublicState.HAKI_FUTURE_SIGHT)) {
            vignette(g, w, h, 0x7FD8FF, 0.22f);
        }
        if (pressure > 0) {
            vignette(g, w, h, 0x600008, pressure * 0.7f);
        }
        if (flash > 0) {
            g.fill(0, 0, w, h, ((int) (flash * 90) << 24) | 0xE8FBFF);
        }
    }

    private static void vignette(GuiGraphics g, int w, int h, int rgb, float alpha) {
        int edge = (int) (Mth.clamp(alpha, 0, 1) * 255) << 24 | rgb;
        int clear = rgb & 0xFFFFFF;
        int band = Math.max(12, h / 6);
        g.fillGradient(0, 0, w, band, edge, clear);
        g.fillGradient(0, h - band, w, h, clear, edge);
        // Sides: rotate a vertical gradient by drawing thin columns.
        int side = Math.max(12, w / 10);
        for (int x = 0; x < side; x += 2) {
            float k = 1f - x / (float) side;
            int a = (int) (Mth.clamp(alpha, 0, 1) * 255 * k * k) << 24;
            g.fill(x, 0, x + 2, h, a | rgb);
            g.fill(w - x - 2, 0, w - x, h, a | rgb);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // First person
    // ---------------------------------------------------------------------------------------------------------

    /** Hardened arms in first person: draw the normal hand, then the coating over it. */
    private static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        int bits = HakiLayer.bits(player);
        if ((bits & (PublicState.HAKI_ARM | PublicState.HAKI_FULL_BODY | PublicState.HAKI_INFUSION)) == 0) {
            return;
        }
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        event.setCanceled(true);
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        if (right) {
            renderer.renderRightHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            renderer.renderLeftHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player);
        }
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        boolean visible = sleeve.visible;
        sleeve.visible = true;
        if ((bits & (PublicState.HAKI_ARM | PublicState.HAKI_FULL_BODY)) != 0) {
            sleeve.render(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(HakiLayer.ARMAMENT)),
                    event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0xF0FFFFFF);
            float scroll = player.tickCount * 0.01f;
            sleeve.render(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.energySwirl(HakiLayer.SHEEN, scroll, scroll * 0.5f)),
                    event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0xFF54447F);
        }
        if ((bits & PublicState.HAKI_INFUSION) != 0) {
            float scroll = player.tickCount * 0.04f;
            sleeve.render(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.energySwirl(HakiLayer.SHEEN, -scroll, scroll)),
                    event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0xFF8C1018);
        }
        sleeve.visible = visible;
    }
}
