package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.props;

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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Bata Bata no Mi. Make butter: soft and slippery so foes can't keep their footing, or hardened into glue, spears and
 * casts that hold them fast.
 */
public class ButterFruit extends DevilFruit {
    public static final int COLOR = 0xF8E880;
    public static final int GOLD = 0xE8C040;
    static final String M = "fruit/grandline:butter";

    public static final DeferredHolder<Ability, ButterShot> BUTTER_SHOT = ability("butter_shot", ButterShot::new);
    public static final DeferredHolder<Ability, ButterSlick> BUTTER_SLICK = ability("butter_slick", ButterSlick::new);
    public static final DeferredHolder<Ability, ButterGlue> BUTTER_GLUE = ability("butter_glue", ButterGlue::new);
    public static final DeferredHolder<Ability, ButterCoat> BUTTER_COAT = ability("butter_coat", ButterCoat::new);
    public static final DeferredHolder<Ability, ButterSpear> BUTTER_SPEAR = ability("butter_spear", ButterSpear::new);
    public static final DeferredHolder<Ability, ButterFlood> BUTTER_FLOOD = ability("butter_flood", ButterFlood::new);
    public static final DeferredHolder<Ability, ButterCast> BUTTER_CAST = ability("butter_cast", ButterCast::new);

    public static final ResourceLocation BLOB = GrandLine.id("butter_blob");
    public static final ResourceLocation SLICK = GrandLine.id("butter_slick");
    public static final ResourceLocation SPEAR = GrandLine.id("butter_spear");
    public static final ResourceLocation FLOOD = GrandLine.id("butter_flood");

    public ButterFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Bata Bata no Mi", COLOR)
                .abilities(BUTTER_SHOT, BUTTER_SLICK, BUTTER_GLUE, BUTTER_COAT, BUTTER_SPEAR)
                .awakened(BUTTER_FLOOD, BUTTER_CAST));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BLOB, Projectiles.bolt(AbilityDamage.STRIKE, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }));
        ProjectileBehavior.Registry.register(SLICK, Projectiles.zone(10, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
            double a = p.level().random.nextDouble() * Math.PI * 2;
            target.setDeltaMovement(target.getDeltaMovement().add(Math.cos(a) * 0.35, 0, Math.sin(a) * 0.35));
            target.hurtMarked = true;
        }, null));
        ProjectileBehavior.Registry.register(SPEAR, Projectiles.piercing(AbilityDamage.SLASH, null));
        ProjectileBehavior.Registry.register(FLOOD, Projectiles.zone(4, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.STRIKE, p.damage());
                Kit.launch(target, p.getDeltaMovement().scale(1.4).add(0, 0.2, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Fling a lump of butter: it smacks, sticks, and leaves the target slow and clumsy.
     *
     * @name Butter Shot
     */
    public static final class ButterShot extends Ability {
        ButterShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, COLOR).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, BLOB, 1.4, 0.5f, COLOR, 5f, 30).withGravity(0.03);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SLIME_SQUISH_SMALL, 1f, 1.2f);
        }
    }

    /**
     * Spread slippery butter where you point: nobody can keep their footing on it for ten seconds.
     *
     * @name Butter Slick
     */
    public static final class ButterSlick extends Ability {
        ButterSlick() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, GOLD).cooldown(200).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            AbilityProjectile.zone(player, SLICK, at, 5 * ctx.power(), COLOR, 0f, 200);
            Vfx.burst(ctx.level(), player, at, COLOR, 5f, Kind.DROP, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.HONEY_BLOCK_SLIDE, 1.4f, 1f);
        }
    }

    /**
     * Harden butter into glue around a foe's legs: it is stuck where it stands for four seconds.
     *
     * @name Butter Glue
     */
    public static final class ButterGlue extends Ability {
        ButterGlue() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, GOLD).cooldown(180).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.GLUE, 80);
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.position(), COLOR, 0.3f, Kind.DROP, Vfx.BEAM_PARTICLES);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.HONEY_BLOCK_PLACE, 1.4f, 0.8f);
        }
    }

    /**
     * Coat yourself in slippery butter: blows slide off you and do half the harm.
     *
     * @name Butter Coat
     */
    public static final class ButterCoat extends Ability {
        ButterCoat() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                    .maxActive(500).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.6f, Kind.DROP, 0);
            ctx.sound(SoundEvents.HONEY_BLOCK_PLACE, 1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 0.8f, Kind.DROP);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            if (event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK)) {
                event.setAmount(event.getAmount() * 0.5f);
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), GOLD, 1f, Kind.DROP);
            }
        }
    }

    /**
     * Harden butter into a spear and hurl it: it runs through everything in its way.
     *
     * @name Butter Spear
     */
    public static final class ButterSpear extends Ability {
        ButterSpear() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, GOLD).cooldown(80).stamina(12).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SPEAR, 2.0, 0.4f, GOLD, 11f, 25).withGravity(0.01);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 1f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Pour out a rolling flood of melted butter: everything in front of you is swept off its feet and left sliding.
     *
     * @name Butter Flood
     */
    public static final class ButterFlood extends Ability {
        ButterFlood() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            AbilityProjectile.spawn(player, FLOOD, player.position().add(dir.scale(2)).add(0, 0.8, 0), dir.scale(0.55), 7f * ctx.power(),
                    COLOR, 8f * ctx.power(), 50);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.HONEY_BLOCK_SLIDE, 2f, 0.5f);
        }
    }

    /**
     * Set every foe around you in a hard cast of butter: none can move or act for three and a half seconds.
     *
     * @name Butter Cast
     */
    public static final class ButterCast extends Ability {
        ButterCast() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, GOLD).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                Kit.encase(target, Vfx.StatusLook.GLUE, 70);
                Kit.hit(player, target, AbilityDamage.CRUSH, 6f * ctx.power());
            }
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 8f, Kind.DROP, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.HONEY_BLOCK_PLACE, 2f, 0.5f);
        }
    }
}
