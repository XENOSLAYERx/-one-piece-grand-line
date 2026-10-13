package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fruit.logia.RumbleFruit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Soru Soru no Mi. Take the lifespan out of anyone who fears you, and breathe soul into the things around you: a
 * living sun, a thundercloud, a sword, and an army of homies.
 */
public class SoulFruit extends DevilFruit {
    public static final int COLOR = 0xF0E0A0;
    public static final int RED = 0xE84A4A;
    public static final int SUN = 0xFF8A2A;
    public static final int CLOUD = 0x7A8AB8;
    static final String M = "fruit/grandline:soul";
    public static final ResourceLocation HOMIE = GrandLine.id("soul_homie");

    public static final DeferredHolder<Ability, SoulPocus> SOUL_POCUS = ability("soul_soul_pocus", SoulPocus::new);
    public static final DeferredHolder<Ability, Prometheus> PROMETHEUS = ability("soul_prometheus", Prometheus::new);
    public static final DeferredHolder<Ability, Zeus> ZEUS = ability("soul_zeus", Zeus::new);
    public static final DeferredHolder<Ability, Napoleon> NAPOLEON = ability("soul_napoleon", Napoleon::new);
    public static final DeferredHolder<Ability, Homies> HOMIES = ability("soul_homies", Homies::new);
    public static final DeferredHolder<Ability, Maser> MASER = ability("soul_maser", Maser::new);
    public static final DeferredHolder<Ability, HeavenlyFire> HEAVENLY_FIRE = ability("soul_heavenly_fire", HeavenlyFire::new);

    public static final ResourceLocation SUN_KIND = GrandLine.id("soul_prometheus");
    public static final ResourceLocation SPEAR = GrandLine.id("soul_spear");

    public SoulFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.MYTHICAL, "Soru Soru no Mi", COLOR)
                .abilities(SOUL_POCUS, PROMETHEUS, ZEUS, NAPOLEON, HOMIES)
                .awakened(MASER, HEAVENLY_FIRE));
    }

    static void init() {}

    static void behaviors() {
        // Prometheus: a living sun hovering by the user's shoulder, burning whatever comes near.
        ProjectileBehavior.Registry.register(SUN_KIND, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.FIRE, p.damage());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
            }
        }, null));
        ProjectileBehavior.Registry.register(SPEAR, Projectiles.blast(5, AbilityDamage.FIRE,
                (p, target) -> target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100)), p -> {
                    if (p.level() instanceof ServerLevel level) {
                        Vfx.burst(level, p, p.position(), SUN, 5f, Kind.FLAME, Vfx.SHOCKWAVE | Vfx.FLASH | Vfx.HEAVY);
                    }
                }));
    }

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, HOMIE);
        Kit.discard(player, SUN_KIND);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Ask "life or treats?": everyone in front of you weaker than you has years of lifespan torn out (a share of
     * their health, and some of it is yours); the stronger merely falter.
     *
     * @name Soul Pocus
     */
    public static final class SoulPocus extends Ability {
        SoulPocus() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, RED).cooldown(160).stamina(18).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 8 * ctx.power(), 40)) {
                if (target.getHealth() < player.getHealth()) {
                    float taken = Math.min(20f, target.getMaxHealth() * 0.3f) * ctx.power();
                    if (Kit.hit(player, target, AbilityDamage.DRAIN, taken)) {
                        player.heal(taken * 0.5f);
                        Vfx.trail(ctx.level(), player, target.getBoundingBox().getCenter(), player.getEyePosition(), COLOR, 0.6f,
                                Kind.GLOW);
                    }
                } else {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
            }
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.CONQUEROR_WAVE.get(), 1f, 1.4f);
        }
    }

    /** Give a soul to the sun: Prometheus hovers at your shoulder and burns everything that comes near you. */
    public static final class Prometheus extends Ability {
        Prometheus() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SUN, SUN).cooldown(300).stamina(16).staminaPerTick(0.15f)
                    .maxActive(600).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, SUN_KIND);
            AbilityProjectile.zone(player, SUN_KIND, player.position().add(0, 2.5, 0), 4 * ctx.power(), SUN, 3f * ctx.power(), 620)
                    .following(new Vec3(0.8, 2.5, 0));
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 1.2f, 0.8f);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.discard(ctx.player(), SUN_KIND);
        }
    }

    /** Your thundercloud homie Zeus strikes the foe you point at with a bolt of lightning. */
    public static final class Zeus extends Ability {
        Zeus() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, CLOUD).cooldown(100).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 28).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 28).ifPresent(target -> {
                RumbleFruit.bolt(ctx.level(), target.position());
                Kit.hit(player, target, AbilityDamage.SHOCK, 14f * ctx.power());
                Vfx.burst(ctx.level(), target, target.position().add(0, 6, 0), CLOUD, 2.5f, Kind.SMOKE, 0);
            });
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.RUMBLE_THUNDER.get(), 1.2f, 1f);
        }
    }

    /** Napoleon, your living sword, cleaves through everything in a wide arc in front of you. */
    public static final class Napoleon extends Ability {
        Napoleon() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, RED).cooldown(60).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 6 * ctx.power(), 60)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 12f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.0, 0.3);
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(2)), player.getLookAngle(), COLOR, 3f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.7f);
        }
    }

    /** Breathe soul into the things around you: two flying homies dart out to fight for you for twenty seconds. */
    public static final class Homies extends Ability {
        Homies() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(500).stamina(20).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Minions.dismiss(player, HOMIE);
            for (Vec3 at : Kit.circle(player.position().add(0, 1.5, 0), 1.5, 2, 0)) {
                Minions.spawn(player, HOMIE, EntityType.VEX, at, 400, null);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.VEX_AMBIENT, 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Prometheus and Zeus fire together: a beam of fire and lightning burns through everything ahead.
     *
     * @name Maser
     */
    public static final class Maser extends Ability {
        Maser() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, SUN).cooldown(600).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 30 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.6)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 12f * ctx.power());
                Kit.hit(player, target, AbilityDamage.SHOCK, 10f * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
            }
            Vec3 end = AbilityDamage.lookTarget(player, range);
            Vfx.beam(ctx.level(), player, ctx.handPos(), end, SUN, 1.6f, Kind.FLAME, Vfx.BEAM_GLOW);
            Vfx.beam(ctx.level(), player, ctx.handPos(), end, 0xFFF27A, 0.5f, Kind.SPARK, Vfx.BEAM_BOLT);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.RUMBLE_THUNDER.get(), 1.6f, 0.8f);
        }
    }

    /** Hurl a spear of living fire that explodes where it lands, setting everything around ablaze. */
    public static final class HeavenlyFire extends Ability {
        HeavenlyFire() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, SUN).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SPEAR, 1.4, 1.2f, SUN, 20f, 40);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.FIRE_FIST.get(), 1.6f, 0.7f);
        }
    }
}
