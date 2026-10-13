package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.props;

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
import com.nerodev.grandline.fruit.kit.Projectiles;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Shiku Shiku no Mi. Spread sickness, as the sickly Doc Q does: a touch that leaves fever and nausea behind, clouds
 * of contagion, and his infamous apples, some of which are just apples and some of which explode.
 */
public class SickFruit extends DevilFruit {
    public static final int COLOR = 0x8AA83A;
    public static final int APPLE = 0xD83A2A;
    public static final int MIASMA_COLOR = 0x6A8A3A;
    static final String M = "fruit/grandline:sick";

    public static final DeferredHolder<Ability, SickTouch> SICK_TOUCH = ability("sick_touch", SickTouch::new);
    public static final DeferredHolder<Ability, DeathApple> DEATH_APPLE = ability("sick_death_apple", DeathApple::new);
    public static final DeferredHolder<Ability, Fever> FEVER = ability("sick_fever", Fever::new);
    public static final DeferredHolder<Ability, Contagion> CONTAGION = ability("sick_contagion", Contagion::new);
    public static final DeferredHolder<Ability, AppleRoulette> APPLE_ROULETTE = ability("sick_apple_roulette", AppleRoulette::new);
    public static final DeferredHolder<Ability, Pandemic> PANDEMIC = ability("sick_pandemic", Pandemic::new);
    public static final DeferredHolder<Ability, Carrier> CARRIER = ability("sick_carrier", Carrier::new);

    public static final ResourceLocation BOMB_APPLE = GrandLine.id("sick_bomb_apple");
    public static final ResourceLocation ROTTEN_APPLE = GrandLine.id("sick_rotten_apple");
    public static final ResourceLocation MIASMA = GrandLine.id("sick_miasma");

    public SickFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Shiku Shiku no Mi", COLOR)
                .abilities(SICK_TOUCH, DEATH_APPLE, FEVER, CONTAGION, APPLE_ROULETTE)
                .awakened(PANDEMIC, CARRIER));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BOMB_APPLE, Projectiles.blast(3, AbilityDamage.BLAST, null,
                p -> Vfx.burst((ServerLevel) p.level(), p, p.position(), APPLE, 3f, Kind.SPARK, Vfx.RING | Vfx.FLASH | Vfx.HEAVY)));
        ProjectileBehavior.Registry.register(ROTTEN_APPLE, Projectiles.bolt(AbilityDamage.POISON, (p, target) -> sicken(target, 120, 1)));
        ProjectileBehavior.Registry.register(MIASMA, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.POISON, p.damage());
                sicken(target, 60, 0);
            }
        }, null));
    }

    /** Falls ill: poisoned, feverish (weak and slow), nauseous and starving, for {@code ticks}. */
    static void sicken(LivingEntity target, int ticks, int level) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, level));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, level));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, level));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.min(ticks, 100), 0));
        if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks, level + 1));
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Lay a hand on the foe in front of you: it falls sick on the spot, poisoned, feverish and nauseous.
     *
     * @name Sick Touch
     */
    public static final class SickTouch extends Ability {
        SickTouch() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.POISON, 6f * ctx.power());
                sicken(target, 200, 1);
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), COLOR, 1.4f, Kind.SMOKE);
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.WITCH_DRINK, 1f, 0.6f);
        }
    }

    /** Toss one of your apples: it bursts on whatever it hits with a bang. */
    public static final class DeathApple extends Ability {
        DeathApple() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, APPLE).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, BOMB_APPLE, 1.1, 0.45f, APPLE, 12f, 40);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SNOWBALL_THROW, 1f, 0.6f);
        }
    }

    /** Breathe a fever over everything in front of you: it weakens, slows and staggers. */
    public static final class Fever extends Ability {
        Fever() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(140).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double reach = 8 * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, reach, 50)) {
                Kit.hit(player, target, AbilityDamage.POISON, 6f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 1));
            }
            Vfx.beam(ctx.level(), player, player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(reach)), COLOR,
                    2f, Kind.SMOKE, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.WITCH_AMBIENT, 1f, 0.6f);
        }
    }

    /** Leave a cloud of contagion where you point: for eight seconds everything inside keeps falling sick. */
    public static final class Contagion extends Ability {
        Contagion() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, MIASMA_COLOR).cooldown(260).stamina(16).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 8);
            AbilityProjectile.zone(player, MIASMA, at, 5 * ctx.power(), MIASMA_COLOR, 3f * ctx.power(), 160);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GAS_HISS.get(), 1.2f, 0.6f);
        }
    }

    /**
     * Doc Q's game of chance: throw five apples in a fan. Each one may be an ordinary rotten apple that sickens what it
     * hits, or one that explodes.
     */
    public static final class AppleRoulette extends Ability {
        AppleRoulette() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, APPLE).cooldown(240).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (int i = -2; i <= 2; i++) {
                boolean bomb = player.getRandom().nextFloat() < 0.45f;
                Vec3 dir = look.yRot(i * 0.18f).add(0, 0.12, 0).normalize();
                AbilityProjectile.spawn(player, bomb ? BOMB_APPLE : ROTTEN_APPLE, ctx.handPos(), dir.scale(1.0), 0.45f, APPLE,
                        (bomb ? 14f : 6f) * ctx.power(), 40).withPower(ctx.power());
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SNOWBALL_THROW, 1.2f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A pandemic: everything around you falls gravely ill at once, wasting, poisoned and barely able to stand. */
    public static final class Pandemic extends Ability {
        Pandemic() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, MIASMA_COLOR).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.DRAIN, 10f * ctx.power());
                sicken(target, 240, 2);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1));
            }
            Vfx.burst(ctx.level(), player, player.position(), MIASMA_COLOR, (float) radius, Kind.SMOKE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GAS_BOOM.get(), 1.4f, 0.5f);
        }
    }

    /**
     * Become a walking carrier for thirty seconds: everything that strikes you, or stands too close, catches your
     * sickness, and you shrug off poisons yourself.
     */
    public static final class Carrier extends Ability {
        Carrier() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SKULL, COLOR).cooldown(900).stamina(20).staminaPerTick(0.08f).maxActive(600)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 2.4f, Kind.SMOKE, Vfx.RING);
            ctx.sound(SoundEvents.WITCH_AMBIENT, 1.2f, 0.5f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WITHER);
            if (ctx.activeTicks() % 20 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 4)) {
                    sicken(target, 60, 0);
                }
                Vfx.burst(ctx.level(), player, player.position(), MIASMA_COLOR, 4f, Kind.SMOKE, Vfx.GROUND);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != ctx.player()) {
                sicken(attacker, 120, 1);
            }
        }
    }
}
