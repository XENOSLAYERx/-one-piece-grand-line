package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wapu Wapu no Mi. Warp yourself, and anything you can see, from one place to another: step through space, put a
 * bullet straight into a far target, swap places, or drop a foe out of the sky.
 */
public class WarpFruit extends DevilFruit {
    public static final int COLOR = 0x5A3A8A;
    public static final int TEAL = 0x3AE0C8;
    static final String M = "fruit/grandline:warp";
    static final Map<UUID, Anchor> ANCHORS = new HashMap<>();

    public static final DeferredHolder<Ability, WarpStep> WARP_STEP = ability("warp_step", WarpStep::new);
    public static final DeferredHolder<Ability, WarpBullet> WARP_BULLET = ability("warp_bullet", WarpBullet::new);
    public static final DeferredHolder<Ability, SkyDrop> SKY_DROP = ability("warp_sky_drop", SkyDrop::new);
    public static final DeferredHolder<Ability, Swap> SWAP = ability("warp_swap", Swap::new);
    public static final DeferredHolder<Ability, WarpAnchor> WARP_ANCHOR = ability("warp_anchor", WarpAnchor::new);
    public static final DeferredHolder<Ability, MassDrop> MASS_DROP = ability("warp_mass_drop", MassDrop::new);
    public static final DeferredHolder<Ability, WarpBarrage> WARP_BARRAGE = ability("warp_barrage", WarpBarrage::new);

    record Anchor(ResourceKey<Level> dimension, Vec3 pos) {}

    public WarpFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Wapu Wapu no Mi", COLOR)
                .abilities(WARP_STEP, WARP_BULLET, SKY_DROP, SWAP, WARP_ANCHOR)
                .awakened(MASS_DROP, WARP_BARRAGE));
    }

    static void init() {}

    static void behaviors() {}

    @Override
    public void onLost(ServerPlayer player) {
        ANCHORS.remove(player.getUUID());
    }

    /** Warps {@code target} up to {@code height} blocks straight up (as high as there is room), to fall back down. */
    static void drop(ServerLevel level, LivingEntity target, double height) {
        Vec3 from = target.position();
        for (double h = height; h >= 3; h -= 1) {
            Vec3 to = from.add(0, h, 0);
            if (level.noCollision(target, target.getBoundingBox().move(0, h, 0))) {
                target.teleportTo(to.x, to.y, to.z);
                target.setDeltaMovement(Vec3.ZERO);
                target.hurtMarked = true;
                Vfx.blink(level, target, from, to, COLOR, Kind.STAR);
                return;
            }
        }
    }

    /** A bullet that leaves the barrel and arrives at the target in the same instant. */
    static void warpShot(AbilityContext ctx, LivingEntity target, float damage) {
        Kit.hit(ctx.player(), target, AbilityDamage.STRIKE, damage);
        Vfx.blink(ctx.level(), ctx.player(), ctx.handPos(), target.getBoundingBox().getCenter(), TEAL, Kind.STREAK);
        Vfx.impact(ctx.level(), ctx.player(), target.getBoundingBox().getCenter(), TEAL, 1f, Kind.SPARK);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Warp to the spot you're looking at, up to sixteen blocks away.
     *
     * @name Warp Step
     */
    public static final class WarpStep extends Ability {
        WarpStep() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.blink(ctx.player(), 16 * ctx.power(), TEAL, Kind.STAR);
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.3f);
        }
    }

    /**
     * Fire a bullet that warps straight into a target you can see, however far: no travel, no dodging.
     *
     * @name Warp Bullet
     */
    public static final class WarpBullet extends Ability {
        WarpBullet() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, TEAL).cooldown(50).stamina(9).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 48).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.target(ctx.player(), 48).ifPresent(target -> warpShot(ctx, target, 9f * ctx.power()));
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.CROSSBOW_SHOOT, 1.2f, 1.4f);
        }
    }

    /** Warp a foe high into the air above where it stands, and let it fall. */
    public static final class SkyDrop extends Ability {
        SkyDrop() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(160).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.target(ctx.player(), 16).ifPresent(target -> drop(ctx.level(), target, 8 * ctx.power()));
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.8f);
        }
    }

    /** Swap places with a creature you can see. */
    public static final class Swap extends Ability {
        Swap() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, TEAL).cooldown(160).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 24).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                Vec3 mine = player.position();
                Vec3 theirs = target.position();
                target.teleportTo(mine.x, mine.y, mine.z);
                player.teleportTo(theirs.x, theirs.y, theirs.z);
                player.resetFallDistance();
                Vfx.blink(ctx.level(), player, mine, theirs, TEAL, Kind.STAR);
                Vfx.blink(ctx.level(), target, theirs, mine, COLOR, Kind.STAR);
            });
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1.2f, 1f);
        }
    }

    /**
     * Drop a warp anchor where you stand: when the move ends, up to thirty seconds later, you warp back to it.
     *
     * @name Warp Anchor
     */
    public static final class WarpAnchor extends Ability {
        WarpAnchor() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CROWN, COLOR).cooldown(300).stamina(14).staminaPerTick(0.05f)
                    .maxActive(600).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ANCHORS.put(player.getUUID(), new Anchor(ctx.level().dimension(), player.position()));
            Vfx.burst(ctx.level(), player, player.position(), TEAL, 1.6f, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.sound(SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, 1f, 1.4f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            Anchor anchor = ANCHORS.get(ctx.player().getUUID());
            if (anchor != null && ctx.activeTicks() % 20 == 0 && anchor.dimension() == ctx.level().dimension()) {
                Vfx.impact(ctx.level(), null, anchor.pos().add(0, 0.2, 0), TEAL, 1f, Kind.RING);
            }
            return anchor != null;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ServerPlayer player = ctx.player();
            Anchor anchor = ANCHORS.remove(player.getUUID());
            if (anchor == null || anchor.dimension() != ctx.level().dimension() || !player.isAlive()) {
                return;
            }
            Vec3 from = player.position();
            player.teleportTo(anchor.pos().x, anchor.pos().y, anchor.pos().z);
            player.resetFallDistance();
            Vfx.blink(ctx.level(), player, from, anchor.pos(), TEAL, Kind.STAR);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Warp every foe around you high into the sky at once. */
    public static final class MassDrop extends Ability {
        MassDrop() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, COLOR).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                drop(ctx.level(), target, 10 * ctx.power());
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 10f, Kind.STAR, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 2f, 0.5f);
        }
    }

    /**
     * Empty your rifle through warp holes: a bullet lands in each of the six nearest foes in the same instant.
     *
     * @name Warp Barrage
     */
    public static final class WarpBarrage extends Ability {
        WarpBarrage() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, TEAL).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return AbilityDamage.around(player, player.position(), 32).isEmpty() ? Optional.of(Component.literal("No foes nearby"))
                    : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            List<LivingEntity> foes = AbilityDamage.around(player, player.position(), 32);
            for (LivingEntity target : foes.subList(0, Math.min(6, foes.size()))) {
                warpShot(ctx, target, 9f * ctx.power());
            }
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.CROSSBOW_SHOOT, 2f, 1.2f);
        }
    }
}
