package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Bari Bari no Mi. Barriers that nothing can break: walls, a ball around the body, a box around a foe, and two walls
 * that crush whatever stands between them.
 */
public class BarrierFruit extends DevilFruit {
    public static final int COLOR = 0x7AF0E8;
    public static final int DEEP = 0x2A8A8A;
    static final String M = "fruit/grandline:barrier";
    static final BlockState PANE = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();

    public static final DeferredHolder<Ability, BarrierWall> BARRIER_WALL = ability("barrier_wall", BarrierWall::new);
    public static final DeferredHolder<Ability, BarrierBulls> BARRIER_BULLS = ability("barrier_bulls", BarrierBulls::new);
    public static final DeferredHolder<Ability, BarrierCrash> BARRIER_CRASH = ability("barrier_crash", BarrierCrash::new);
    public static final DeferredHolder<Ability, BarrierBall> BARRIER_BALL = ability("barrier_ball", BarrierBall::new);
    public static final DeferredHolder<Ability, BarrierBox> BARRIER_BOX = ability("barrier_box", BarrierBox::new);
    public static final DeferredHolder<Ability, BarrierCrush> BARRIER_CRUSH = ability("barrier_crush", BarrierCrush::new);
    public static final DeferredHolder<Ability, BarrierDome> BARRIER_DOME = ability("barrier_dome", BarrierDome::new);

    public static final ResourceLocation SLAB = GrandLine.id("barrier_slab");

    public BarrierFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Bari Bari no Mi", COLOR)
                .abilities(BARRIER_WALL, BARRIER_BULLS, BARRIER_CRASH, BARRIER_BALL, BARRIER_BOX)
                .awakened(BARRIER_CRUSH, BARRIER_DOME));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SLAB, Projectiles.piercing(AbilityDamage.STRIKE,
                (p, target) -> AbilityDamage.knockback(target, p.position().subtract(p.getDeltaMovement()), 1.8, 0.4)));
    }

    /** Inside a Barrier Ball almost nothing gets through. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (AbilityState.of(user).isActive(BARRIER_BALL.getId()) && event.getSource().getEntity() != null) {
            event.setAmount(event.getAmount() * 0.2f);
            Vfx.impact(user.serverLevel(), user, user.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.RING);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Raise an unbreakable barrier wall in front of you.
     *
     * @name Barrier
     */
    public static final class BarrierWall extends Ability {
        BarrierWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(120).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(2.5));
            Kit.placeUnbreakable(ctx.level(), Kit.wall(center, dir, 5, 4), PANE, 160);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), COLOR, 2.5f, Kind.RING, Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_PLACE, 1.4f, 0.8f);
        }
    }

    /**
     * Charge behind a barrier held out in front of you, ramming everyone in your path aside.
     *
     * @name Barrier Bulls
     */
    public static final class BarrierBulls extends Ability {
        BarrierBulls() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, DEEP).cooldown(80).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 12 * ctx.power(), 1.6)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 2.0, 0.4);
            }
            Vec3 start = player.position().add(0, 1, 0);
            Kit.dash(player, 1.8, 0.1);
            Vfx.trail(ctx.level(), player, start, start.add(player.getLookAngle().scale(12)), COLOR, 1.2f, Kind.RING);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.4f, 0.6f);
        }
    }

    /**
     * Throw a flat slab of barrier that smashes through a line of foes.
     *
     * @name Barrier Crash
     */
    public static final class BarrierCrash extends Ability {
        BarrierCrash() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(70).stamina(12).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SLAB, 1.5, 1.8f, COLOR, 10f, 30);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.2f);
        }
    }

    /**
     * Seal yourself in a sphere of barrier: projectiles bounce off and blows barely land.
     *
     * @name Barrier Ball
     */
    public static final class BarrierBall extends Ability {
        BarrierBall() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(14).staminaPerTick(0.25f)
                    .maxActive(200).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.sound(SoundEvents.AMETHYST_BLOCK_PLACE, 1.2f, 1f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 40 == 0) {
                Vfx.status(ctx.level(), player, Vfx.StatusLook.BARRIER, 42);
            }
            for (Projectile p : ctx.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(2),
                    e -> e.getOwner() != player)) {
                p.setDeltaMovement(p.getDeltaMovement().scale(-0.6));
                p.hurtMarked = true;
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.6f, Kind.RING, 0);
        }
    }

    /**
     * Trap a foe in a box of barrier: it is stuck inside for four seconds.
     *
     * @name Barrier Box
     */
    public static final class BarrierBox extends Ability {
        BarrierBox() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(220).stamina(16).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.target(ctx.player(), 16).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.BARRIER, 80);
                Kit.placeUnbreakable(ctx.level(), Kit.cage(target), PANE, 80);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_PLACE, 1.6f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Two barrier walls slam together on the spot you point at, crushing everything between.
     *
     * @name Barrier Crush
     */
    public static final class BarrierCrush extends Ability {
        BarrierCrush() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, DEEP).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 4 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 20f * ctx.power());
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                        60, 2));
            }
            Vec3 side = ctx.right().scale(3);
            Vfx.trail(ctx.level(), player, at.add(side).add(0, 1.5, 0), at.add(0, 1.5, 0), COLOR, 2f, Kind.RING);
            Vfx.trail(ctx.level(), player, at.subtract(side).add(0, 1.5, 0), at.add(0, 1.5, 0), COLOR, 2f, Kind.RING);
            Vfx.burst(ctx.level(), player, at.add(0, 1.5, 0), COLOR, 4f, Kind.SHARD, Vfx.SHOCKWAVE | Vfx.FLASH | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.6f, 1.2f);
        }
    }

    /**
     * Throw an unbreakable dome over yourself and everyone near you for fifteen seconds.
     *
     * @name Barrier Dome
     */
    public static final class BarrierDome extends Ability {
        BarrierDome() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, DEEP).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.placeUnbreakable(ctx.level(), Kit.dome(player.blockPosition(), 6), PANE, 300);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 6f, Kind.RING, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_PLACE, 2f, 0.5f);
        }
    }
}
