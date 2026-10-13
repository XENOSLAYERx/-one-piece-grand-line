package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mero Mero no Mi. Anyone who feels the slightest attraction to the user, struck by a beam of love, turns to stone.
 * Bosses are too proud to be smitten, and other players only stiffen for a moment.
 */
public class LoveFruit extends DevilFruit {
    public static final int COLOR = 0xF04A8A;
    public static final int PINK = 0xFFD0E0;
    static final String M = "fruit/grandline:love";

    public static final DeferredHolder<Ability, Mellow> MELLOW = ability("love_mellow", Mellow::new);
    public static final DeferredHolder<Ability, SlaveArrow> SLAVE_ARROW = ability("love_slave_arrow", SlaveArrow::new);
    public static final DeferredHolder<Ability, PistolKiss> PISTOL_KISS = ability("love_pistol_kiss", PistolKiss::new);
    public static final DeferredHolder<Ability, PerfumeFemur> PERFUME_FEMUR = ability("love_perfume_femur", PerfumeFemur::new);
    public static final DeferredHolder<Ability, Shatter> SHATTER = ability("love_shatter", Shatter::new);
    public static final DeferredHolder<Ability, LoveWave> LOVE_WAVE = ability("love_wave", LoveWave::new);
    public static final DeferredHolder<Ability, Aphrodite> APHRODITE = ability("love_aphrodite", Aphrodite::new);

    public static final ResourceLocation ARROW = GrandLine.id("love_arrow");
    public static final ResourceLocation KISS = GrandLine.id("love_kiss");

    public LoveFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Mero Mero no Mi", COLOR)
                .abilities(MELLOW, SLAVE_ARROW, PISTOL_KISS, PERFUME_FEMUR, SHATTER)
                .awakened(LOVE_WAVE, APHRODITE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(ARROW, Projectiles.homing(18, 0.2, Projectiles.bolt(AbilityDamage.STRIKE,
                (p, target) -> petrify(target, 30))));
        ProjectileBehavior.Registry.register(KISS, Projectiles.bolt(AbilityDamage.STRIKE, (p, target) -> {
            AbilityDamage.knockback(target, p.position().subtract(p.getDeltaMovement()), 1.2, 0.3);
            Kit.root(target, Vfx.StatusLook.STONE, 40);
        }));
    }

    /** Turns a smitten creature to stone for {@code ticks} (bosses are immune, players held more briefly). */
    static boolean petrify(LivingEntity target, int ticks) {
        if (target.getType().is(Tags.EntityTypes.BOSSES)) {
            return false;
        }
        Kit.encase(target, Vfx.StatusLook.STONE, target instanceof Player ? Math.max(10, ticks / 2) : ticks);
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Form a heart with your hands and fire a beam of love: everyone it catches turns to stone.
     *
     * @name Mero Mero Mellow
     */
    public static final class Mellow extends Ability {
        Mellow() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(200).stamina(18).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, range, 35)) {
                petrify(target, 100);
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(range)), COLOR, 2f,
                    Kind.HEART, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.6f);
        }
    }

    /**
     * Draw back a bow of love and loose a volley of heart arrows that seek out your foes.
     *
     * @name Slave Arrow
     */
    public static final class SlaveArrow extends Ability {
        SlaveArrow() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, PINK).cooldown(90).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.15)).add(0, 0.1, 0).normalize();
                AbilityProjectile.spawn(player, ARROW, ctx.handPos(), dir.scale(1.4), 0.4f, COLOR, 3f * ctx.power(), 40);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ARROW_SHOOT, 1f, 1.6f);
        }
    }

    /**
     * Blow a kiss that flies like a bullet: it strikes hard and turns the target's feet to stone.
     *
     * @name Pistol Kiss
     */
    public static final class PistolKiss extends Ability {
        PistolKiss() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, PINK).cooldown(60).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, KISS, 2.4, 0.35f, COLOR, 10f, 20);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.8f);
        }
    }

    /**
     * A kick that turns whatever it strikes to stone before it lands.
     *
     * @name Perfume Femur
     */
    public static final class PerfumeFemur extends Ability {
        PerfumeFemur() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, COLOR).cooldown(80).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.8 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * ctx.power());
                petrify(target, 40);
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.5)), player.getLookAngle(), PINK, 1.8f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.2f);
        }
    }

    /** Shatter the stone around every petrified foe near you: each takes a heavy blow and is released. */
    public static final class Shatter extends Ability {
        Shatter() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(200).stamina(18).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 16)) {
                if (target.hasEffect(GLMobEffects.ENCASED) || target.hasEffect(GLMobEffects.ROOTED)) {
                    target.removeEffect(GLMobEffects.ENCASED);
                    target.removeEffect(GLMobEffects.ROOTED);
                    Kit.hit(player, target, AbilityDamage.CRUSH, 14f * ctx.power());
                    Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), 0x8A8A82, 1.6f, Kind.SHARD, Vfx.FLASH);
                }
            }
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.STONE_BREAK, 1.6f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A wave of love pours out all around you: everyone it reaches turns to stone. */
    public static final class LoveWave extends Ability {
        LoveWave() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(700).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                petrify(target, 80);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, (float) radius, Kind.HEART, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1f);
        }
    }

    /**
     * A great heart of love flies ahead, striking and petrifying everything in its path.
     *
     * @name Aphrodite's Kiss
     */
    public static final class Aphrodite extends Ability {
        Aphrodite() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, PINK).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 24 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 2.0)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 18f * ctx.power());
                petrify(target, 80);
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), AbilityDamage.lookTarget(player, range), COLOR, 2.4f, Kind.HEART,
                    Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.4f);
        }
    }
}
