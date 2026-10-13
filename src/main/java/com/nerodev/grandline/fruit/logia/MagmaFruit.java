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
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Magu Magu no Mi. A body of magma, hotter than any flame: it burns fire itself and melts ice. Fists of molten
 * rock, falling magma, pools that scorch anyone who stands in them.
 */
public class MagmaFruit extends LogiaFruit {
    public static final int COLOR = 0xD8381E;
    public static final int GLOW = 0xFF9A2A;
    static final String M = "fruit/grandline:magma";

    public static final DeferredHolder<Ability, GreatEruption> GREAT_ERUPTION = ability("great_eruption", GreatEruption::new);
    public static final DeferredHolder<Ability, HoundBlaze> HOUND_BLAZE = ability("hound_blaze", HoundBlaze::new);
    public static final DeferredHolder<Ability, MeteorVolcano> METEOR_VOLCANO = ability("meteor_volcano", MeteorVolcano::new);
    public static final DeferredHolder<Ability, MagmaPool> MAGMA_POOL = ability("magma_pool", MagmaPool::new);
    public static final DeferredHolder<Ability, VolcanicPillars> VOLCANIC_PILLARS = ability("volcanic_pillars", VolcanicPillars::new);
    public static final DeferredHolder<Ability, VolcanoField> VOLCANO_FIELD = ability("volcano_field", VolcanoField::new);
    public static final DeferredHolder<Ability, MagmaTsunami> MAGMA_TSUNAMI = ability("magma_tsunami", MagmaTsunami::new);

    public static final ResourceLocation FIST = GrandLine.id("magma_fist");
    public static final ResourceLocation POOL = GrandLine.id("magma_pool");
    public static final ResourceLocation FIELD = GrandLine.id("volcano_field");
    public static final ResourceLocation WAVE = GrandLine.id("magma_tsunami");

    public MagmaFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.LEGENDARY, "Magu Magu no Mi", COLOR)
                .abilities(GREAT_ERUPTION, HOUND_BLAZE, METEOR_VOLCANO, MAGMA_POOL, VOLCANIC_PILLARS)
                .awakened(VOLCANO_FIELD, MAGMA_TSUNAMI));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FIST, Projectiles.blast(3, AbilityDamage.FIRE, (p, target) -> target.igniteForSeconds(6),
                p -> {
                    ServerPlayer owner = p.ownerPlayer();
                    if (owner != null) {
                        Vfx.burst(owner.serverLevel(), p, p.position(), GLOW, 3.5f, Kind.FLAME, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
                        owner.serverLevel().playSound(null, p.blockPosition(), GLSounds.MAGMA_ERUPTION.get(), SoundSource.PLAYERS,
                                1.2f, 1.1f);
                        AbilityProjectile.zone(owner, POOL, p.position(), 2.5, COLOR, p.damage() * 0.2f, 80);
                    }
                }));
        // Molten ground: burns whoever stands in it.
        ProjectileBehavior.Registry.register(POOL, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && target.onGround()) {
                Kit.hit(owner, target, AbilityDamage.FIRE, Math.max(2f, p.damage()));
                target.igniteForSeconds(4);
            }
        }, null));
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(Integer.MAX_VALUE, (p, target) -> {}, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null || p.tickCount % 6 != 0) {
                return;
            }
            double radius = p.size() / 2;
            double a = owner.getRandom().nextDouble() * Mth.TWO_PI;
            double d = 2 + owner.getRandom().nextDouble() * (radius - 2);
            Vec3 at = Kit.ground(owner.serverLevel(), p.position().add(Math.cos(a) * d, 3, Math.sin(a) * d), 8);
            erupt(owner, at, p.damage());
        }));
        // A rolling wave of magma: everything it washes over is burned and carried along.
        ProjectileBehavior.Registry.register(WAVE, Projectiles.zone(4, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.FIRE, p.damage());
                target.igniteForSeconds(5);
                Kit.launch(target, p.getDeltaMovement().scale(1.6).add(0, 0.3, 0));
            }
        }, null));
    }

    /** A column of magma bursting from the ground at {@code at}. */
    static void erupt(ServerPlayer owner, Vec3 at, float damage) {
        for (LivingEntity target : AbilityDamage.around(owner, at.add(0, 1, 0), 2.2)) {
            Kit.hit(owner, target, AbilityDamage.FIRE, damage);
            target.igniteForSeconds(5);
            Kit.launch(target, target.getDeltaMovement().add(0, 0.9, 0));
        }
        Vfx.eruption(owner.serverLevel(), owner, at, GLOW, 4f, Kind.FLAME);
        owner.serverLevel().playSound(null, at.x, at.y, at.z, GLSounds.MAGMA_ERUPTION.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE);
    }

    @Override
    protected float weakness(ServerPlayer user, DamageSource source) {
        // Magma melts ice before it can bite.
        return source.is(DamageTypeTags.IS_FREEZING) ? 0.5f : 1f;
    }

    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() == user) {
            target.igniteForSeconds(FruitManager.isAwakened(user) ? 6 : 3);
        }
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.getRemainingFireTicks() > 0 && user.tickCount % 20 == 0) {
            user.clearFire();
        }
        user.setTicksFrozen(0);
    }

    @Override
    public boolean ticks() {
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Charge a fist of magma and launch it: it explodes and leaves the ground molten. */
    public static final class GreatEruption extends Ability {
        GreatEruption() {
            super(props(ActivationType.CHARGE, AbilityIcon.FIST, GLOW).cooldown(120).stamina(20).minCharge(10)
                    .maxActive(30).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.MAGMA_SIZZLE.get(), 1f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.handPos(), GLOW, 0.8f, Kind.FLAME);
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            float charge = Mth.clamp(ctx.activeTicks() / 30f, 0.4f, 1f);
            Kit.shoot(ctx, FIST, 1.3, 1.2f + charge * 1.2f, COLOR, 14f * charge, 30);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.MAGMA_ERUPTION.get(), 1.2f, 1.3f);
        }
    }

    /** Lunge forward as a hound of magma and bite down on whatever is in front of you. */
    public static final class HoundBlaze extends Ability {
        HoundBlaze() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(70).stamina(14).mastery(M, 10)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.dash(player, 1.6, 0.2);
            for (LivingEntity target : AbilityDamage.cone(player, 5, 50)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 12f * ctx.power());
                target.igniteForSeconds(6);
            }
            Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(2));
            Vfx.burst(ctx.level(), player, front, GLOW, 2.5f, Kind.FLAME, Vfx.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.MAGMA_SIZZLE.get(), 1.2f, 1.1f);
        }
    }

    /** Hurl magma into the sky: it comes down as a rain of molten fists around where you look. */
    public static final class MeteorVolcano extends Ability {
        MeteorVolcano() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, GLOW).cooldown(260).stamina(28).mastery(M, 20)
                    .masteryGain(1.2));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.aimPoint(player, 36);
            int count = 8 + (int) (4 * (ctx.power() - 1));
            for (int i = 0; i < count; i++) {
                double a = player.getRandom().nextDouble() * Mth.TWO_PI;
                double d = player.getRandom().nextDouble() * 6;
                Vec3 spawn = at.add(Math.cos(a) * d, 14 + i * 2.5, Math.sin(a) * d);
                AbilityProjectile.spawn(player, FIST, spawn, new Vec3(0, -1.1, 0), 1.2f, COLOR, 8f * ctx.power(), 60);
            }
            Vfx.beam(ctx.level(), player, player.position().add(0, 1.5, 0), player.position().add(0, 18, 0), GLOW, 1.4f,
                    Kind.FLAME, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.MAGMA_ERUPTION.get(), 1.6f, 0.8f);
        }
    }

    /** Melt the ground around you into a pool of magma that burns anyone standing in it. */
    public static final class MagmaPool extends Ability {
        MagmaPool() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, COLOR).cooldown(200).stamina(20).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, POOL, player.position(), 5 * ctx.power(), COLOR, 3f * ctx.power(), 140);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), GLOW, 5f, Kind.FLAME, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.MAGMA_SIZZLE.get(), 1.5f, 0.8f);
        }
    }

    /** Pillars of magma burst out of the ground in a line toward where you look. */
    public static final class VolcanicPillars extends Ability {
        VolcanicPillars() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, GLOW).cooldown(150).stamina(20).mastery(M, 50)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Set<Integer> hit = new HashSet<>();
            for (int i = 1; i <= 5; i++) {
                Vec3 at = Kit.ground(ctx.level(), player.position().add(dir.scale(i * 3)).add(0, 2, 0), 6);
                for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 2.2)) {
                    if (hit.add(target.getId())) {
                        Kit.hit(player, target, AbilityDamage.FIRE, 9f * ctx.power());
                        target.igniteForSeconds(5);
                        Kit.launch(target, target.getDeltaMovement().add(0, 0.9, 0));
                    }
                }
                Vfx.eruption(ctx.level(), player, at, GLOW, 3f + i * 0.5f, Kind.FLAME);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.MAGMA_ERUPTION.get(), 1.4f, 1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** The ground around you becomes a volcano: eruptions keep bursting at random for several seconds. */
    public static final class VolcanoField extends Ability {
        VolcanoField() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(700).stamina(40).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, FIELD, player.position(), 12 * ctx.power(), COLOR, 9f * ctx.power(), 100)
                    .following(Vec3.ZERO);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.MAGMA_ERUPTION.get(), 2.5f, 0.6f);
        }
    }

    /** A wall of magma rolls forward, burning and sweeping away everything in its path. */
    public static final class MagmaTsunami extends Ability {
        MagmaTsunami() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(600).stamina(38).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            AbilityProjectile.spawn(player, WAVE, player.position().add(dir.scale(2)), dir.scale(0.6), 9f * ctx.power(), COLOR,
                    8f * ctx.power(), 50);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.MAGMA_ERUPTION.get(), 2f, 0.7f);
        }
    }
}
