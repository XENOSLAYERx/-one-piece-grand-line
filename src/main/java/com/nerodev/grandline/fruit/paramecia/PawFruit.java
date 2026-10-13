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
import com.nerodev.grandline.sound.GLSounds;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Nikyu Nikyu no Mi. Paw pads that repel anything they touch: air into paw-shaped cannon blasts, attacks back at
 * their senders, pain and fatigue out of a body, and people (or the user) clear across the island.
 */
public class PawFruit extends DevilFruit {
    public static final int COLOR = 0x8A5AA8;
    public static final int PAD = 0xF0C8E0;
    static final String M = "fruit/grandline:paw";

    public static final DeferredHolder<Ability, PadCannon> PAD_CANNON = ability("paw_pad_cannon", PadCannon::new);
    public static final DeferredHolder<Ability, Repel> REPEL = ability("paw_repel", Repel::new);
    public static final DeferredHolder<Ability, Travel> TRAVEL = ability("paw_travel", Travel::new);
    public static final DeferredHolder<Ability, PainExtraction> PAIN_EXTRACTION = ability("paw_pain_extraction", PainExtraction::new);
    public static final DeferredHolder<Ability, UrsusShock> URSUS_SHOCK = ability("paw_ursus_shock", UrsusShock::new);
    public static final DeferredHolder<Ability, PawLeap> PAW_LEAP = ability("paw_leap", PawLeap::new);
    public static final DeferredHolder<Ability, PainBubble> PAIN_BUBBLE = ability("paw_pain_bubble", PainBubble::new);

    public static final ResourceLocation PAW = GrandLine.id("paw_pad");
    public static final ResourceLocation URSUS = GrandLine.id("paw_ursus");

    public PawFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Nikyu Nikyu no Mi", COLOR)
                .abilities(PAD_CANNON, REPEL, TRAVEL, PAIN_EXTRACTION, URSUS_SHOCK)
                .awakened(PAW_LEAP, PAIN_BUBBLE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(PAW, Projectiles.piercing(AbilityDamage.BLAST,
                (p, target) -> AbilityDamage.knockback(target, p.position().subtract(p.getDeltaMovement()), 2.0, 0.5)));
        // Ursus Shock: a compressed paw of air that drifts forward, then bursts with enormous force.
        ProjectileBehavior.Registry.register(URSUS, Projectiles.blast(6, AbilityDamage.BLAST,
                (p, target) -> AbilityDamage.knockback(target, p.position(), 2.4, 0.8), p -> {
                    if (p.level() instanceof ServerLevel level) {
                        Vfx.burst(level, p, p.position(), PAD, 6f, Kind.SHOCKWAVE, Vfx.SHOCKWAVE | Vfx.FLASH | Vfx.HEAVY);
                        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                                net.minecraft.sounds.SoundSource.PLAYERS, 3f, 0.6f);
                    }
                }));
    }

    /** Pads that repel: a third of arrows and thrown things bounce off the user. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE) && user.getRandom().nextFloat() < 0.33f) {
            event.setCanceled(true);
            Vfx.impact(user.serverLevel(), user, user.getBoundingBox().getCenter(), PAD, 1f, Kind.SHOCKWAVE);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Slap the air: a paw-shaped blast of compressed air flies out and hurls aside whatever it hits.
     *
     * @name Pad Cannon
     */
    public static final class PadCannon extends Ability {
        PadCannon() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, PAD).cooldown(40).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, PAW, 1.8, 1.2f, PAD, 9f, 25);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.HAKI_EMISSION.get(), 1f, 1.2f);
        }
    }

    /** Push with both paws: everything around you, people and projectiles alike, is repelled away. */
    public static final class Repel extends Ability {
        Repel() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(100).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 center = player.getBoundingBox().getCenter();
            for (LivingEntity target : AbilityDamage.around(player, center, 5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.BLAST, 4f * ctx.power());
                AbilityDamage.knockback(target, center, 2.2, 0.6);
            }
            for (Projectile p : ctx.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(5),
                    e -> e.getOwner() != player)) {
                Vec3 away = p.position().subtract(center).normalize().scale(Math.max(1.0, p.getDeltaMovement().length()));
                p.setDeltaMovement(away);
                p.setOwner(player);
                p.hurtMarked = true;
            }
            Vfx.burst(ctx.level(), player, center, PAD, 5f, Kind.SHOCKWAVE, Vfx.RING | Vfx.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.2f, 1.4f);
        }
    }

    /**
     * Touch a foe with a paw and send it flying far over the horizon.
     *
     * @name Where Would You Like to Travel?
     */
    public static final class Travel extends Ability {
        Travel() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, PAD).cooldown(300).stamina(18).mastery(M, 10)
                    .masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 2f);
                Kit.launch(target, ctx.horizontalLook().scale(3.6).add(0, 1.6, 0));
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), PAD, 2f, Kind.SHOCKWAVE, Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.HAKI_EMISSION.get(), 1.4f, 0.8f);
        }
    }

    /** Repel the pain and fatigue out of your own body; the pain takes shape and you hurl it into a foe. */
    public static final class PainExtraction extends Ability {
        PainExtraction() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(400).stamina(16).mastery(M, 20)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float missing = player.getMaxHealth() - player.getHealth();
            for (Holder<MobEffect> effect : new ArrayList<>(player.getActiveEffectsMap().keySet())) {
                if (!effect.value().isBeneficial()) {
                    player.removeEffect(effect);
                }
            }
            player.heal(8f);
            Kit.target(player, 14).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.DRAIN, Math.min(20f, 4f + missing * 0.6f) * ctx.power());
                Vfx.trail(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), PAD, 0.8f, Kind.SHOCKWAVE);
            });
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), PAD, 1.4f, Kind.HEART, 0);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.HAKI_SENSE.get(), 1f, 0.8f);
        }
    }

    /**
     * Charge, then release a compressed paw of air that drifts forward and explodes with crushing force.
     *
     * @name Ursus Shock
     */
    public static final class UrsusShock extends Ability {
        UrsusShock() {
            super(props(ActivationType.CHARGE, AbilityIcon.ORB, PAD).cooldown(400).stamina(26).minCharge(20).maxActive(60)
                    .mastery(M, 35).masteryGain(1.2));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(GLSounds.CONQUEROR_CHARGE.get(), 1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.handPos().add(ctx.player().getLookAngle()), PAD, 0.4f + ctx.activeTicks() * 0.02f,
                        Kind.SHOCKWAVE);
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            float charge = Mth.clamp(ctx.activeTicks() / 60f, 0.4f, 1f);
            AbilityProjectile shot = Kit.shoot(ctx, URSUS, 0.5, 1.5f + 1.5f * charge, PAD, 20f * charge, 40);
            shot.withPower(ctx.power() * (0.6f + 0.4f * charge));
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.HAKI_EMISSION.get(), 1.6f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Repel your own body: you vanish and land wherever you are looking, up to fifty blocks away. */
    public static final class PawLeap extends Ability {
        PawLeap() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(300).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 to = Kit.ground(ctx.level(), Kit.aimPoint(player, 50), 12);
            player.teleportTo(to.x, to.y, to.z);
            player.resetFallDistance();
            Vfx.blink(ctx.level(), player, from, to, PAD, Kind.SHOCKWAVE);
            ctx.sound(GLSounds.HAKI_EMISSION.get(), 1.2f, 1.6f);
        }
    }

    /** Extract a foe's own pain into a bubble and push it back in all at once: it reels, knocked out for a moment. */
    public static final class PainBubble extends Ability {
        PainBubble() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, PAD).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 6).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6).ifPresent(target -> {
                float missing = target.getMaxHealth() - target.getHealth();
                Kit.hit(player, target, AbilityDamage.DRAIN, (14f + missing * 0.25f) * ctx.power());
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 40, 0, false, false, true));
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), PAD, 2.5f, Kind.SHOCKWAVE, Vfx.FLASH | Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.4f);
        }
    }
}
