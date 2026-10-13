package com.nerodev.grandline.fruit.logia;

import static com.nerodev.grandline.fruit.logia.LogiaBatch.ability;
import static com.nerodev.grandline.fruit.logia.LogiaBatch.props;

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
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.LogiaFruit;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Yami Yami no Mi. Darkness with infinite pull. Unlike every other Logia it cannot let blows pass (it draws in
 * pain as well, taking more damage), but its touch seals other Devil Fruit powers and always reaches a Logia's
 * real body. Its darkness drags everything in.
 */
public class DarkFruit extends LogiaFruit {
    public static final int COLOR = 0x6A3AA8;
    public static final int VOID = 0x140A1E;
    static final String M = "fruit/grandline:dark";

    public static final DeferredHolder<Ability, BlackHole> BLACK_HOLE = ability("black_hole", BlackHole::new);
    public static final DeferredHolder<Ability, Kurouzu> KUROUZU = ability("kurouzu", Kurouzu::new);
    public static final DeferredHolder<Ability, DarkMatter> DARK_MATTER = ability("dark_matter", DarkMatter::new);
    public static final DeferredHolder<Ability, Liberation> LIBERATION = ability("liberation", Liberation::new);
    public static final DeferredHolder<Ability, Nullify> NULLIFY = ability("darkness_nullify", Nullify::new);
    public static final DeferredHolder<Ability, BlackWorld> BLACK_WORLD = ability("black_world", BlackWorld::new);
    public static final DeferredHolder<Ability, AbyssGate> ABYSS_GATE = ability("abyss_gate", AbyssGate::new);

    public static final ResourceLocation HOLE = GrandLine.id("black_hole");
    public static final ResourceLocation MATTER = GrandLine.id("dark_matter");
    public static final ResourceLocation WORLD = GrandLine.id("black_world");

    public DarkFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.MYTHICAL, "Yami Yami no Mi", COLOR)
                .abilities(BLACK_HOLE, KUROUZU, DARK_MATTER, LIBERATION, NULLIFY)
                .awakened(BLACK_WORLD, ABYSS_GATE));
    }

    static void init() {}

    static void behaviors() {
        // Darkness reaches a Logia's real body.
        LogiaFruit.TOUCH.add(source -> source.is(AbilityDamage.DARK)
                || source.getEntity() instanceof Player attacker && FruitManager.fruitOf(attacker) instanceof DarkFruit);
        ProjectileBehavior.Registry.register(HOLE, darkness(3, 0.25, 20));
        ProjectileBehavior.Registry.register(WORLD, darkness(4, 0.3, 15));
        ProjectileBehavior.Registry.register(MATTER, Projectiles.blast(2.5, AbilityDamage.DARK, null, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                AbilityProjectile.zone(owner, HOLE, p.position(), 4, COLOR, p.damage() * 0.4f, 60);
                Vfx.burst(owner.serverLevel(), p, p.position(), COLOR, 3f, Kind.SMOKE, Vfx.SHOCKWAVE);
            }
        }));
    }

    /** A pulling darkness: drags toward its centre every tick, hurts and slows every {@code every} ticks. */
    private static ProjectileBehavior darkness(float damagePerHit, double pull, int every) {
        return Projectiles.zone(2, (p, target) -> {
            Kit.pull(target, p.position(), pull);
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % every == 0) {
                Kit.hit(owner, target, AbilityDamage.DARK, p.damage() > 0 ? p.damage() : damagePerHit);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
            }
        }, null);
    }

    static void seal(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(GLMobEffects.FRUIT_SEALED, ticks, 0, false, true, true));
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return false;
    }

    @Override
    protected boolean intangible(ServerPlayer user) {
        return false;
    }

    @Override
    protected float weakness(ServerPlayer user, DamageSource source) {
        // Darkness draws in everything, pain included.
        return 1.2f;
    }

    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        // Bare-handed blows nullify a Devil Fruit for a moment.
        if (event.getSource().getDirectEntity() == user && target instanceof Player other && FruitManager.hasFruit(other)) {
            seal(other, 40);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Spread darkness under your feet that drags in and crushes everything around it. */
    public static final class BlackHole extends Ability {
        BlackHole() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, COLOR).cooldown(240).stamina(24).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, HOLE, player.position(), 8 * ctx.power(), COLOR, 3f * ctx.power(), 120);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), VOID, 8f, Kind.SMOKE, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 1.5f, 0.8f);
        }
    }

    /** A whirlpool of darkness from your hand yanks your target straight into your grip. */
    public static final class Kurouzu extends Ability {
        Kurouzu() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(100).stamina(16).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 24).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                Vec3 hand = ctx.handPos();
                Vfx.beam(ctx.level(), player, target.getBoundingBox().getCenter(), hand, COLOR, 1f, Kind.SMOKE, Vfx.BEAM_DARK);
                double distance = target.position().distanceTo(player.position());
                Kit.launch(target, player.position().subtract(target.position()).normalize().scale(Math.min(3.2, distance * 0.35))
                        .add(0, 0.3, 0));
                Kit.root(target, Vfx.StatusLook.ROOTS, 40);
                Kit.hit(player, target, AbilityDamage.DARK, 4f * ctx.power());
                seal(target, 60);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 1.2f, 1.3f);
        }
    }

    /** Throw a ball of compressed darkness: where it lands, a small black hole opens. */
    public static final class DarkMatter extends Ability {
        DarkMatter() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, COLOR).cooldown(90).stamina(16).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, MATTER, 1.2, 1.1f, VOID, 8f, 30).withGravity(0.02);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.DARK_PULSE.get(), 1f, 1.2f);
        }
    }

    /** Release everything the darkness swallowed in one violent blast ahead of you. */
    public static final class Liberation extends Ability {
        Liberation() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(160).stamina(22).mastery(M, 20)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 9 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 11f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.6, 0.4);
            }
            Vec3 eye = player.getEyePosition();
            Vfx.beam(ctx.level(), player, ctx.handPos(), eye.add(player.getLookAngle().scale(9)), VOID, 2.5f, Kind.SHARD,
                    Vfx.BEAM_DARK);
            Vfx.burst(ctx.level(), player, eye.add(player.getLookAngle().scale(4)), COLOR, 4f, Kind.SHARD, Vfx.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.DARK_PULSE.get(), 1.6f, 0.8f);
        }
    }

    /** Seize a foe: darkness smothers their Devil Fruit, leaving them powerless and solid. */
    public static final class Nullify extends Ability {
        Nullify() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, COLOR).cooldown(240).stamina(18).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                seal(target, (int) (200 * ctx.power()));
                Kit.hit(player, target, AbilityDamage.DARK, 8f * ctx.power());
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), VOID, 2f, Kind.SMOKE, Vfx.SHOCKWAVE);
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.DARK_PULSE.get(), 1.2f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Drown a whole area in darkness: everyone in it is blinded, dragged and crushed. */
    public static final class BlackWorld extends Ability {
        BlackWorld() {
            super(props(ActivationType.INSTANT, AbilityIcon.MOON, COLOR).cooldown(900).stamina(45).masteryGain(2.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, WORLD, player.position(), 16 * ctx.power(), COLOR, 5f * ctx.power(), 160);
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 16 * ctx.power())) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), VOID, 16f, Kind.SMOKE,
                    Vfx.RING | Vfx.SHOCKWAVE | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 2.5f, 0.5f);
        }
    }

    /** Pull every creature around you into your hands and hold them there. */
    public static final class AbyssGate extends Ability {
        AbyssGate() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, VOID).cooldown(600).stamina(35).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 14 * ctx.power())) {
                Vec3 to = player.position().subtract(target.position());
                Kit.launch(target, to.normalize().scale(Math.min(3.0, to.length() * 0.3)).add(0, 0.25, 0));
                Kit.root(target, Vfx.StatusLook.ROOTS, 60);
                Kit.hit(player, target, AbilityDamage.DARK, 6f * ctx.power());
                Vfx.beam(ctx.level(), player, target.getBoundingBox().getCenter(), player.position().add(0, 1, 0), COLOR, 0.8f,
                        Kind.SMOKE, Vfx.BEAM_DARK);
            }
            Vfx.spiral(ctx.level(), player, player.position(), VOID, 6f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 2f, 0.7f);
        }
    }
}
