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
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Noro Noro no Mi. Noro Noro photons slow down anything they touch, people, animals, even flying cannonballs, to a
 * thirtieth of their speed for thirty seconds.
 */
public class SlowFruit extends DevilFruit {
    public static final int COLOR = 0xE8E070;
    public static final int GLOW = 0xFFF8B0;
    static final String M = "fruit/grandline:slow";
    static final int THIRTY_SECONDS = 600;

    public static final DeferredHolder<Ability, SlowBeam> SLOW_BEAM = ability("slow_beam", SlowBeam::new);
    public static final DeferredHolder<Ability, BeamSword> BEAM_SWORD = ability("slow_beam_sword", BeamSword::new);
    public static final DeferredHolder<Ability, SlowCombo> SLOW_COMBO = ability("slow_combo", SlowCombo::new);
    public static final DeferredHolder<Ability, SlowField> SLOW_FIELD = ability("slow_field", SlowField::new);
    public static final DeferredHolder<Ability, SlowShield> SLOW_SHIELD = ability("slow_shield", SlowShield::new);
    public static final DeferredHolder<Ability, SlowWorld> SLOW_WORLD = ability("slow_world", SlowWorld::new);
    public static final DeferredHolder<Ability, FullBeam> FULL_BEAM = ability("slow_full_beam", FullBeam::new);

    public static final ResourceLocation FIELD = GrandLine.id("slow_field");

    public SlowFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Noro Noro no Mi", COLOR)
                .abilities(SLOW_BEAM, BEAM_SWORD, SLOW_COMBO, SLOW_FIELD, SLOW_SHIELD)
                .awakened(SLOW_WORLD, FULL_BEAM));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(10, (p, target) -> slow(target, 30, 3), p -> {
            if (p.level() instanceof ServerLevel level) {
                slowProjectiles(level, p.getBoundingBox().inflate(p.size() / 2.0), p.ownerPlayer(), 0.6);
            }
        }));
    }

    /** Slows a creature to a crawl for {@code ticks} (strength {@code level} 0-4). */
    static void slow(LivingEntity target, int ticks, int level) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, level));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, Math.min(2, level)));
    }

    static boolean slowed(LivingEntity target) {
        return target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    /** Projectiles (not the owner's) inside {@code box} lose most of their speed. */
    static int slowProjectiles(ServerLevel level, AABB box, ServerPlayer owner, double keep) {
        int count = 0;
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, e -> owner == null || e.getOwner() != owner)) {
            p.setDeltaMovement(p.getDeltaMovement().scale(keep));
            p.hurtMarked = true;
            count++;
        }
        return count;
    }

    /** Fires the photon beam along the player's look, slowing everything it touches. */
    static void beam(AbilityContext ctx, double range, double width, int ticks, int level, float damage) {
        ServerPlayer player = ctx.player();
        for (LivingEntity target : AbilityDamage.line(player, range, width)) {
            slow(target, ticks, level);
            if (damage > 0) {
                Kit.hit(player, target, AbilityDamage.LIGHT, damage);
            }
            Vfx.impact(ctx.level(), target, target.getBoundingBox().getCenter(), GLOW, 1f, Kind.GLOW);
        }
        Vec3 end = AbilityDamage.lookTarget(player, range);
        Vec3 from = player.getEyePosition();
        AABB box = new AABB(from, end).inflate(width);
        slowProjectiles(ctx.level(), box, player, 1.0 / 30);
        Vfx.beam(ctx.level(), player, ctx.handPos(), end, COLOR, (float) width * 0.6f, Kind.GLOW, Vfx.BEAM_GLOW);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Fire slowing photons from your hands: everything they touch crawls for thirty seconds.
     *
     * @name Noro Noro Beam
     */
    public static final class SlowBeam extends Ability {
        SlowBeam() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, COLOR).cooldown(120).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            beam(ctx, 22 * ctx.power(), 0.9, THIRTY_SECONDS, 4, 0f);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BEACON_AMBIENT, 1.5f, 1.8f);
        }
    }

    /**
     * Swing a sword-shaped beam of photons: whoever it cuts is slowed.
     *
     * @name Noro Noro Beam Sword
     */
    public static final class BeamSword extends Ability {
        BeamSword() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, GLOW).cooldown(60).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5 * ctx.power(), 55)) {
                Kit.hit(player, target, AbilityDamage.LIGHT, 6f * ctx.power());
                slow(target, 200, 2);
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.6)), player.getLookAngle(), COLOR, 2.2f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.6f);
        }
    }

    /**
     * Rain blows on a slowed foe faster than it can react: six hits in an instant.
     *
     * @name Slow-Slow Combo
     */
    public static final class SlowCombo extends Ability {
        SlowCombo() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(100).stamina(14).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 5).filter(SlowFruit::slowed).isPresent() ? Optional.empty()
                    : Optional.of(Component.literal("Slow a foe within reach first"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 5).filter(SlowFruit::slowed).ifPresent(target -> {
                for (int i = 0; i < 6; i++) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
                }
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), GLOW, 1.4f, Kind.STAR, Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_GATLING);
            ctx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 1.2f);
        }
    }

    /** Bathe an area in Noro Noro photons: creatures and projectiles inside it slow to a crawl. */
    public static final class SlowField extends Ability {
        SlowField() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, GLOW).cooldown(240).stamina(18).mastery(M, 20)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            AbilityProjectile.zone(player, FIELD, at, 6 * ctx.power(), COLOR, 0f, 160);
            Vfx.burst(ctx.level(), player, at, COLOR, 6f, Kind.GLOW, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 1f, 1.8f);
        }
    }

    /** Hold up your hands: every projectile flying near you crawls to a stop in mid-air. */
    public static final class SlowShield extends Ability {
        SlowShield() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(10).staminaPerTick(0.25f)
                    .maxActive(100).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_PALMS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (slowProjectiles(ctx.level(), player.getBoundingBox().inflate(6), player, 0.05) > 0 && ctx.activeTicks() % 4 == 0) {
                Vfx.impact(ctx.level(), player, ctx.handPos().add(player.getLookAngle().scale(1.5)), GLOW, 1f, Kind.GLOW);
            }
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 3f, Kind.GLOW, Vfx.RING);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Flood everything around you with photons: the whole battlefield moves in slow motion except you. */
    public static final class SlowWorld extends Ability {
        SlowWorld() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(700).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 14 * ctx.power();
            AbilityProjectile.zone(player, FIELD, player.position(), radius, COLOR, 0f, 200);
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                slow(target, THIRTY_SECONDS / 2, 4);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, (float) radius, Kind.GLOW,
                    Vfx.RING | Vfx.HEAVY | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 2f, 0.6f);
        }
    }

    /**
     * A huge photon beam that slows and weakens everything in its path for thirty seconds.
     *
     * @name Noro Noro Beam: Full Power
     */
    public static final class FullBeam extends Ability {
        FullBeam() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, GLOW).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            beam(ctx, 32 * ctx.power(), 3.0, THIRTY_SECONDS, 4, 8f * ctx.power());
            for (LivingEntity target : AbilityDamage.line(player, 32 * ctx.power(), 3.0)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, THIRTY_SECONDS, 1));
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 2f, 1.6f);
        }
    }
}
