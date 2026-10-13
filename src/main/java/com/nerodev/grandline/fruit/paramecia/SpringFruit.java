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
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Bane Bane no Mi. Legs and arms become springs: leaps that cross a street, punches that uncoil from ten blocks
 * away, and a body that rebounds off walls faster each time.
 */
public class SpringFruit extends DevilFruit {
    public static final int COLOR = 0xC8C8D0;
    public static final int BLUE = 0x4A8AC8;
    static final String M = "fruit/grandline:spring";
    static final String PINBALL = "grandline_spring_pinball";

    public static final DeferredHolder<Ability, Hopper> HOPPER = ability("spring_hopper", Hopper::new);
    public static final DeferredHolder<Ability, Sniper> SNIPER = ability("spring_sniper", Sniper::new);
    public static final DeferredHolder<Ability, SpringLegs> SPRING_LEGS = ability("spring_legs", SpringLegs::new);
    public static final DeferredHolder<Ability, DeathKnock> DEATH_KNOCK = ability("spring_death_knock", DeathKnock::new);
    public static final DeferredHolder<Ability, Pinball> PINBALL_MOVE = ability("spring_pinball", Pinball::new);
    public static final DeferredHolder<Ability, SpringField> SPRING_FIELD = ability("spring_field", SpringField::new);
    public static final DeferredHolder<Ability, Cannonball> CANNONBALL = ability("spring_cannonball", Cannonball::new);

    public static final ResourceLocation FIELD = GrandLine.id("spring_field");

    public SpringFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Bane Bane no Mi", COLOR)
                .abilities(HOPPER, SNIPER, SPRING_LEGS, DEATH_KNOCK, PINBALL_MOVE)
                .awakened(SPRING_FIELD, CANNONBALL));
    }

    static void init() {}

    static void behaviors() {
        // Springy ground bounces everyone standing on it high into the air.
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(20, (p, target) -> {
            if (target.onGround()) {
                Kit.launch(target, target.getDeltaMovement().add(0, 1.0, 0));
                ServerPlayer owner = p.ownerPlayer();
                if (owner != null) {
                    Kit.hit(owner, target, AbilityDamage.STRIKE, p.damage());
                }
            }
        }, null));
    }

    /** Spring legs soak up most of a fall. */
    @Override
    public void onFall(ServerPlayer user, LivingFallEvent event) {
        event.setDamageMultiplier(event.getDamageMultiplier() * 0.3f);
    }

    /** While Pinball is running, every wall you hit sends you back the other way, faster. */
    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        long until = user.getPersistentData().getLong(PINBALL);
        if (until == 0) {
            return;
        }
        if (user.level().getGameTime() > until) {
            user.getPersistentData().remove(PINBALL);
            return;
        }
        Vec3 motion = user.getDeltaMovement();
        if (user.horizontalCollision) {
            Vec3 back = new Vec3(-motion.x, Math.max(0.3, motion.y), -motion.z);
            if (back.horizontalDistanceSqr() < 0.25) {
                back = user.getLookAngle().scale(-1.4).add(0, 0.3, 0);
            }
            Kit.launch(user, back.scale(1.15));
            user.resetFallDistance();
            Vfx.burst(user.serverLevel(), user, user.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.RING, 0);
            user.level().playSound(null, user.getX(), user.getY(), user.getZ(), GLSounds.BOUNCE.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.2f);
        }
        for (LivingEntity target : AbilityDamage.around(user, user.getBoundingBox().getCenter(), 1.8)) {
            if (target.invulnerableTime <= 0) {
                Kit.hit(user, target, AbilityDamage.STRIKE, 7f + (float) motion.length() * 4f);
                AbilityDamage.knockback(target, user.position(), 1.2, 0.4);
            }
        }
    }

    @Override
    public boolean ticks() {
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Coil your legs and spring far in the direction you face.
     *
     * @name Spring Hopper
     */
    public static final class Hopper extends Ability {
        Hopper() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.dash(ctx.player(), 2.0, 0.6);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 1.4f, Kind.RING, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(GLSounds.BOUNCE.get(), 1f, 1f);
        }
    }

    /**
     * Uncoil a spring-loaded punch that hits ten blocks away.
     *
     * @name Spring Sniper
     */
    public static final class Sniper extends Ability {
        Sniper() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, BLUE).cooldown(50).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 0.9)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.3, 0.3);
            }
            Vec3 end = AbilityDamage.lookTarget(player, range);
            Vfx.beam(ctx.level(), player, ctx.handPos(), end, COLOR, 0.4f, Kind.RING, Vfx.BEAM_PARTICLES);
            Vfx.impact(ctx.level(), player, end, BLUE, 1.2f, Kind.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.RUBBER_SNAP.get(), 1f, 1.3f);
        }
    }

    /** Keep your legs as springs: you jump far higher and land without a scratch. */
    public static final class SpringLegs extends Buffs.Toggle {
        SpringLegs() {
            super(props(ActivationType.TOGGLE, AbilityIcon.JUMP, BLUE).cooldown(100).stamina(8).staminaPerTick(0.1f)
                            .maxActive(1200).mastery(M, 10).masteryGain(0.6),
                    List.of(Buffs.mod(Attributes.JUMP_STRENGTH, GrandLine.id("spring_legs_jump"), 0.45, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.SAFE_FALL_DISTANCE, GrandLine.id("spring_legs_fall"), 20, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("spring_legs_speed"), 0.12,
                                    Operation.ADD_MULTIPLIED_BASE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            ctx.sound(GLSounds.BOUNCE.get(), 0.8f, 1.4f);
        }
    }

    /**
     * Ricochet from foe to foe, striking up to four of them in a blink.
     *
     * @name Spring Death Knock
     */
    public static final class DeathKnock extends Ability {
        DeathKnock() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(140).stamina(16).mastery(M, 20)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            List<LivingEntity> hit = new ArrayList<>();
            Vec3 from = player.position().add(0, 1, 0);
            for (int i = 0; i < 4; i++) {
                LivingEntity next = null;
                double best = 10 * 10;
                for (LivingEntity e : AbilityDamage.around(player, from, 10)) {
                    double d = e.position().distanceToSqr(from);
                    if (!hit.contains(e) && d < best) {
                        best = d;
                        next = e;
                    }
                }
                if (next == null) {
                    break;
                }
                hit.add(next);
                Kit.hit(player, next, AbilityDamage.STRIKE, 8f * ctx.power());
                AbilityDamage.knockback(next, from, 0.8, 0.3);
                Vec3 to = next.getBoundingBox().getCenter();
                Vfx.trail(ctx.level(), player, from, to, BLUE, 0.5f, Kind.RING);
                from = to;
            }
            if (!hit.isEmpty()) {
                Vec3 last = hit.get(hit.size() - 1).position();
                Vec3 land = last.add(last.subtract(player.position()).normalize().scale(-1.5));
                player.teleportTo(land.x, last.y, land.z);
                player.resetFallDistance();
            }
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(GLSounds.BOUNCE.get(), 1.2f, 1.4f);
        }
    }

    /**
     * Launch yourself forward; for two seconds every wall bounces you back faster, bowling over foes.
     *
     * @name Pinball
     */
    public static final class Pinball extends Ability {
        Pinball() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, BLUE).cooldown(160).stamina(16).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.getPersistentData().putLong(PINBALL, ctx.level().getGameTime() + 40);
            Kit.dash(player, 1.8, 0.2);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.BOUNCE.get(), 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Turn the ground around you into springs: anyone standing on it is flung into the air again and again. */
    public static final class SpringField extends Ability {
        SpringField() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            AbilityProjectile.zone(player, FIELD, player.position(), radius, COLOR, 3f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.1, 0), COLOR, (float) radius, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.BOUNCE.get(), 2f, 0.5f);
        }
    }

    /**
     * Compress every spring in your body and fire yourself through a line of foes.
     *
     * @name Spring Cannonball
     */
    public static final class Cannonball extends Ability {
        Cannonball() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, BLUE).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 20 * ctx.power(), 1.6)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 16f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.8, 0.6);
            }
            Vec3 start = player.position().add(0, 1, 0);
            Kit.dash(player, 3.0, 0.3);
            Vfx.trail(ctx.level(), player, start, start.add(player.getLookAngle().scale(20)), BLUE, 1.2f, Kind.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.6f);
        }
    }
}
