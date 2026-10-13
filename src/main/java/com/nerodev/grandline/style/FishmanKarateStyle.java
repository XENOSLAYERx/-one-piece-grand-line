package com.nerodev.grandline.style;

import static com.nerodev.grandline.style.GLStyles.ability;
import static com.nerodev.grandline.style.GLStyles.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Fish-Man Karate: Jinbe's art of striking the water in and around the body. Blows travel through flesh and armour
 * alike, droplets fly like bullets, and the shockwaves carry through water. Every technique is stronger in the water
 * or the rain, and in the hands of a Fish-Man.
 */
public class FishmanKarateStyle extends FightingStyle {
    public static final int COLOR = 0x3A8AC8;
    public static final int WATER = 0x6AC8F0;
    static final String M = "style/grandline:fishman_karate";
    static final ResourceLocation FISHMAN = GrandLine.id("fishman");

    public static final DeferredHolder<Ability, BrickFist> BRICK_FIST = ability("fishman_karate_brick_fist", BrickFist::new);
    public static final DeferredHolder<Ability, Uchimizu> UCHIMIZU = ability("fishman_karate_uchimizu", Uchimizu::new);
    public static final DeferredHolder<Ability, SharkSkinPalm> SHARK_SKIN_PALM = ability("fishman_karate_shark_skin_palm",
            SharkSkinPalm::new);
    public static final DeferredHolder<Ability, Buraikan> BURAIKAN = ability("fishman_karate_buraikan", Buraikan::new);
    public static final DeferredHolder<Ability, VineBrickFist> VINE_BRICK_FIST = ability("fishman_karate_vine_brick_fist",
            VineBrickFist::new);

    public static final ResourceLocation DROPLET = GrandLine.id("fishman_karate_droplet");

    public FishmanKarateStyle() {
        super(COLOR, FishmanKarateStyle::seiken, BRICK_FIST, UCHIMIZU, SHARK_SKIN_PALM, BURAIKAN, VINE_BRICK_FIST);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(DROPLET, Projectiles.piercing(AbilityDamage.STRIKE, null));
    }

    /** Water strengthens the art (in it or in the rain), and so does being a Fish-Man. */
    static float boost(ServerPlayer user) {
        float boost = 1f;
        if (user.isInWaterOrRain()) {
            boost *= 1.3f;
        }
        if (FISHMAN.equals(GLPlayer.data(user).race())) {
            boost *= 1.2f;
        }
        return boost;
    }

    /** Hits {@code target} through its armour: the shock travels inside the body. */
    static void seikenHit(ServerPlayer user, LivingEntity target, float damage) {
        Kit.hit(user, target, AbilityDamage.STRIKE, damage + target.getArmorValue() * 0.5f);
    }

    /** Combo finisher: a straight punch whose shock bursts out of the far side of the foe and throws it back. */
    static void seiken(ServerPlayer attacker, LivingEntity target, int combo) {
        AbilityDamage.knockback(target, attacker.position(), 2.0, 0.4);
        Vec3 through = target.getBoundingBox().getCenter().add(target.position().subtract(attacker.position()).normalize().scale(0.8));
        Vfx.burst(attacker.serverLevel(), attacker, through, WATER, 1.8f, Kind.DROP, Vfx.RING | Vfx.FLASH);
        Fx.animate(attacker, GLEffects.ANIM_CHARGE_PUNCH);
    }

    /** Bare-handed blows strike through the body: harder, and more so in the water. */
    @Override
    public void onUnarmedHit(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        event.setAmount((event.getAmount() + 2f) * boost(user));
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * A straight punch that travels through the foe in front of you, armour and all.
     *
     * @name Thousand-Brick Fist
     */
    public static final class BrickFist extends Ability {
        BrickFist() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4).ifPresent(target -> {
                seikenHit(player, target, 13f * boost(player) * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.4, 0.3);
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), WATER, 1.6f, Kind.SHOCKWAVE);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1f, 1f);
        }
    }

    /** Flick three drops of water at bullet speed; each goes straight through whatever it hits. */
    public static final class Uchimizu extends Ability {
        Uchimizu() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, WATER).cooldown(50).stamina(9).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (int i = -1; i <= 1; i++) {
                AbilityProjectile.spawn(player, DROPLET, ctx.handPos(), look.yRot(i * 0.08f).scale(2.6), 0.25f, WATER,
                        6f * boost(player) * ctx.power(), 20).withPower(ctx.power());
            }
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.PLAYER_SPLASH, 0.8f, 1.6f);
        }
    }

    /**
     * Hold your palms out, rough as sharkskin. Blows that reach you lose most of their force, and
     * whoever strikes you is thrown back.
     *
     * @name Shark-Skin Palm
     */
    public static final class SharkSkinPalm extends Ability {
        SharkSkinPalm() {
            super(props(ActivationType.HOLD, AbilityIcon.HAND, COLOR).cooldown(140).stamina(8).staminaPerTick(0.25f).maxActive(60)
                    .mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.PLAYER_SPLASH, 0.8f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.2f);
            if (event.getSource().getEntity() instanceof LivingEntity attacker && event.getSource().getDirectEntity() == attacker
                    && attacker.distanceToSqr(ctx.player()) < 25) {
                Kit.hit(ctx.player(), attacker, AbilityDamage.STRIKE, 4f * boost(ctx.player()) * ctx.power());
                AbilityDamage.knockback(attacker, ctx.player().position(), 1.6, 0.4);
                Vfx.impact(ctx.level(), ctx.player(), attacker.getBoundingBox().getCenter(), WATER, 1.2f, Kind.DROP);
            }
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Strike the moisture in the air ahead and send a shockwave through everything in a long line. */
    public static final class Buraikan extends Ability {
        Buraikan() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, WATER).cooldown(120).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double length = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 2.0)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 12f * boost(player) * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.8, 0.35);
            }
            Vec3 look = player.getLookAngle();
            for (int i = 1; i <= 4; i++) {
                Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(i * length / 4)), WATER, 1.2f + i * 0.3f,
                        Kind.SHOCKWAVE, 0);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.2f, 0.7f);
        }
    }

    /**
     * Drive a fist into the ground and send the shock out in every direction.
     *
     * @name Vine-Pattern Brick Fist
     */
    public static final class VineBrickFist extends Ability {
        VineBrickFist() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(240).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 6 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                seikenHit(player, target, 14f * boost(player) * ctx.power());
                AbilityDamage.knockback(target, player.position(), 2.2, 0.5);
            }
            Vfx.burst(ctx.level(), player, player.position(), WATER, (float) radius, Kind.DROP, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.2f, 1.1f);
        }
    }
}
