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
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Kage Kage no Mi. Shadows can be cut from their owners' feet and put to use: as fighters, as bats, as spikes rising
 * from the ground, or swallowed whole to swell the user to monstrous size.
 */
public class ShadowFruit extends DevilFruit {
    public static final int COLOR = 0x3A2A4A;
    public static final int VIOLET = 0x9A5AC8;
    static final String M = "fruit/grandline:shadow";
    static final String SHADOWS = "grandline_shadow_count";
    static final String SHADOWLESS = "grandline_shadowless_until";
    static final int MAX_SHADOWS = 10;

    public static final DeferredHolder<Ability, StealShadow> STEAL_SHADOW = ability("shadow_steal", StealShadow::new);
    public static final DeferredHolder<Ability, BrickBat> BRICK_BAT = ability("shadow_brick_bat", BrickBat::new);
    public static final DeferredHolder<Ability, Doppelman> DOPPELMAN = ability("shadow_doppelman", Doppelman::new);
    public static final DeferredHolder<Ability, Revolution> REVOLUTION = ability("shadow_revolution", Revolution::new);
    public static final DeferredHolder<Ability, TsunoTokage> TSUNO_TOKAGE = ability("shadow_tsuno_tokage", TsunoTokage::new);
    public static final DeferredHolder<Ability, Asgard> ASGARD = ability("shadow_asgard", Asgard::new);
    public static final DeferredHolder<Ability, Nightmare> NIGHTMARE = ability("shadow_nightmare", Nightmare::new);

    public static final ResourceLocation BAT = GrandLine.id("shadow_bat");
    public static final ResourceLocation DOPPEL = GrandLine.id("shadow_doppelman");

    public ShadowFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Kage Kage no Mi", COLOR)
                .abilities(STEAL_SHADOW, BRICK_BAT, DOPPELMAN, REVOLUTION, TSUNO_TOKAGE)
                .awakened(ASGARD, NIGHTMARE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BAT, Projectiles.homing(16, 0.25, Projectiles.bolt(AbilityDamage.DARK,
                (p, target) -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0)))));
    }

    static int shadows(ServerPlayer player) {
        return player.getPersistentData().getInt(SHADOWS);
    }

    static void setShadows(ServerPlayer player, int count) {
        player.getPersistentData().putInt(SHADOWS, Math.max(0, Math.min(MAX_SHADOWS, count)));
        player.displayClientMessage(Component.literal("Shadows held: " + shadows(player)), true);
    }

    /**
     * Cuts a creature's shadow away: it is weakened and slowed for a minute and its shadow joins the user's
     * collection. A creature without a shadow has none to give. @return true if a shadow was taken
     */
    static boolean steal(ServerPlayer player, LivingEntity target) {
        long now = player.level().getGameTime();
        if (target.getPersistentData().getLong(SHADOWLESS) > now) {
            return false;
        }
        target.getPersistentData().putLong(SHADOWLESS, now + 1200);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 0));
        setShadows(player, shadows(player) + 1);
        Vfx.trail(player.serverLevel(), player, target.position().add(0, 0.1, 0), player.position().add(0, 1, 0), COLOR, 0.8f,
                Kind.SMOKE);
        return true;
    }

    @Override
    public void onLost(ServerPlayer player) {
        player.getPersistentData().remove(SHADOWS);
        Minions.dismiss(player, DOPPEL);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Cut the shadow from a foe's feet: it is left weak and slow, and the shadow is yours to use. */
    public static final class StealShadow extends Ability {
        StealShadow() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, VIOLET).cooldown(80).stamina(12).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 8).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 8).ifPresent(target -> {
                if (!steal(player, target)) {
                    player.displayClientMessage(Component.literal("That one has no shadow left"), true);
                }
                Kit.hit(player, target, AbilityDamage.DARK, 3f * ctx.power());
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.DARK_PULSE.get(), 1f, 1.3f);
        }
    }

    /**
     * Your shadow breaks into a flock of bats that hunt down your foes.
     *
     * @name Brick Bat
     */
    public static final class BrickBat extends Ability {
        BrickBat() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(70).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = 0; i < 5; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale((i - 2) * 0.25)).add(0, 0.2, 0).normalize();
                AbilityProjectile.spawn(player, BAT, ctx.handPos(), dir.scale(0.9), 0.4f, COLOR, 4f * ctx.power(), 50);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.BAT_TAKEOFF, 1.2f, 0.8f);
        }
    }

    /**
     * Your shadow peels off the ground and fights beside you for twenty seconds.
     *
     * @name Doppelman
     */
    public static final class Doppelman extends Ability {
        Doppelman() {
            super(props(ActivationType.INSTANT, AbilityIcon.TRANSFORM, VIOLET).cooldown(500).stamina(20).mastery(M, 10)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Minions.dismiss(player, DOPPEL);
            Vec3 at = player.position().add(ctx.horizontalLook().scale(-1.5));
            Minions.spawn(player, DOPPEL, EntityType.WITHER_SKELETON, at, 400, mob -> {
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
            });
            Vfx.burst(ctx.level(), player, at.add(0, 1, 0), COLOR, 1.8f, Kind.SMOKE, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 1f, 1.4f);
        }
    }

    /**
     * Trade places with your Doppelman in an instant.
     *
     * @name Shadow Revolution
     */
    public static final class Revolution extends Ability {
        Revolution() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(60).stamina(8).mastery(M, 20).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Minions.of(player, DOPPEL).isEmpty() ? Optional.of(Component.literal("Your Doppelman isn't out")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            List<Mob> doppel = Minions.of(player, DOPPEL);
            if (doppel.isEmpty()) {
                return;
            }
            Mob shadow = doppel.get(0);
            Vec3 mine = player.position();
            Vec3 theirs = shadow.position();
            player.teleportTo(theirs.x, theirs.y, theirs.z);
            shadow.teleportTo(mine.x, mine.y, mine.z);
            Vfx.blink(ctx.level(), player, mine, theirs, VIOLET, Kind.SMOKE);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1f, 0.7f);
        }
    }

    /**
     * A horned lizard of shadow bursts up out of a foe's own shadow and impales it.
     *
     * @name Tsuno Tokage
     */
    public static final class TsunoTokage extends Ability {
        TsunoTokage() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, VIOLET).cooldown(160).stamina(20).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 24).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.DARK, 14f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.2, 0));
                Vfx.eruption(ctx.level(), player, target.position(), COLOR, 4f, Kind.SMOKE);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.DARK_PULSE.get(), 1.4f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Swallow every shadow you hold (and steal those of foes beside you) to swell into a giant: the more shadows, the
     * bigger and stronger. They are spent when it ends.
     *
     * @name Shadow's Asgard
     */
    public static final class Asgard extends Ability {
        static final Buffs.Mod SCALE = Buffs.mod(Attributes.SCALE, GrandLine.id("shadow_asgard_scale"), 0, Operation.ADD_VALUE);

        Asgard() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CROWN, VIOLET).cooldown(900).stamina(30).staminaPerTick(0.25f)
                    .maxActive(600).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8)) {
                steal(player, target);
            }
            int held = Math.max(1, shadows(player));
            Buffs.apply(player, Buffs.mod(Attributes.SCALE, SCALE.id(), 0.4 + 0.15 * held, Operation.ADD_VALUE));
            Buffs.apply(player, Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("shadow_asgard_damage"), 1.5 * held, Operation.ADD_VALUE));
            Buffs.apply(player, Buffs.mod(Attributes.ARMOR, GrandLine.id("shadow_asgard_armor"), held, Operation.ADD_VALUE));
            Buffs.apply(player, Buffs.mod(Attributes.MAX_HEALTH, GrandLine.id("shadow_asgard_health"), 2 * held, Operation.ADD_VALUE));
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 3f, Kind.SMOKE, Vfx.HEAVY | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 2f, 0.5f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 2f, Kind.SMOKE);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ServerPlayer player = ctx.player();
            Buffs.remove(player, SCALE);
            Buffs.remove(player, Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("shadow_asgard_damage"), 0, Operation.ADD_VALUE));
            Buffs.remove(player, Buffs.mod(Attributes.ARMOR, GrandLine.id("shadow_asgard_armor"), 0, Operation.ADD_VALUE));
            Buffs.remove(player, Buffs.mod(Attributes.MAX_HEALTH, GrandLine.id("shadow_asgard_health"), 0, Operation.ADD_VALUE));
            player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            setShadows(player, 0);
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 3f, Kind.SMOKE, 0);
        }
    }

    /** Tear the shadow from every foe around you at once: they reel blind and weak while you gather their shadows. */
    public static final class Nightmare extends Ability {
        Nightmare() {
            super(props(ActivationType.INSTANT, AbilityIcon.MOON, COLOR).cooldown(700).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                steal(player, target);
                Kit.hit(player, target, AbilityDamage.DARK, 8f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), COLOR, (float) radius, Kind.SMOKE,
                    Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 2f, 0.4f);
        }
    }
}
