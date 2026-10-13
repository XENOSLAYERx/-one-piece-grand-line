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
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Gabu Gabu no Mi. Turn the drink in your belly into fire: breathe it, belch it, throw it. After a good long guzzle,
 * every flame you make burns far hotter for fifteen seconds.
 */
public class GuzzleFruit extends DevilFruit {
    public static final int COLOR = 0xC8783A;
    public static final int FLAME = 0xF0D070;
    static final String M = "fruit/grandline:guzzle";
    static final String BOOZE = "grandline_booze_until";

    public static final DeferredHolder<Ability, Guzzle> GUZZLE = ability("guzzle_guzzle", Guzzle::new);
    public static final DeferredHolder<Ability, FireBreath> FIRE_BREATH = ability("guzzle_fire_breath", FireBreath::new);
    public static final DeferredHolder<Ability, BoozeBomb> BOOZE_BOMB = ability("guzzle_booze_bomb", BoozeBomb::new);
    public static final DeferredHolder<Ability, FlamingBelch> FLAMING_BELCH = ability("guzzle_flaming_belch", FlamingBelch::new);
    public static final DeferredHolder<Ability, DrunkenBrawl> DRUNKEN_BRAWL = ability("guzzle_drunken_brawl", DrunkenBrawl::new);
    public static final DeferredHolder<Ability, InfernoBreath> INFERNO_BREATH = ability("guzzle_inferno_breath", InfernoBreath::new);
    public static final DeferredHolder<Ability, SeaOfBooze> SEA_OF_BOOZE = ability("guzzle_sea_of_booze", SeaOfBooze::new);

    public static final ResourceLocation BOMB = GrandLine.id("guzzle_bomb");
    public static final ResourceLocation SEA = GrandLine.id("guzzle_sea");

    public GuzzleFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Gabu Gabu no Mi", COLOR)
                .abilities(GUZZLE, FIRE_BREATH, BOOZE_BOMB, FLAMING_BELCH, DRUNKEN_BRAWL)
                .awakened(INFERNO_BREATH, SEA_OF_BOOZE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BOMB, Projectiles.blast(3, AbilityDamage.FIRE,
                (p, target) -> target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100)),
                p -> Vfx.burst((net.minecraft.server.level.ServerLevel) p.level(), p, p.position(), FLAME, 3f, Kind.FLAME, Vfx.FLASH)));
        ProjectileBehavior.Registry.register(SEA, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.FIRE, p.damage());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
            }
        }, null));
    }

    /** Flames burn 60% hotter for a while after a guzzle. */
    static float booze(ServerPlayer player) {
        return player.getPersistentData().getLong(BOOZE) >= player.level().getGameTime() ? 1.6f : 1f;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Take a long guzzle: you heal, shrug off fire, hit harder, and your flames burn far hotter for fifteen seconds. */
    public static final class Guzzle extends Ability {
        Guzzle() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, COLOR).cooldown(400).stamina(6).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.heal(4f);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
            player.getPersistentData().putLong(BOOZE, ctx.level().getGameTime() + 300);
            Vfx.burst(ctx.level(), player, player.getEyePosition(), COLOR, 1.2f, Kind.DROP, 0);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.GENERIC_DRINK, 1.4f, 0.8f);
        }
    }

    /** Hold to breathe a stream of fire over everything in front of you. */
    public static final class FireBreath extends Ability {
        FireBreath() {
            super(props(ActivationType.HOLD, AbilityIcon.FLAME, FLAME).cooldown(120).stamina(10).staminaPerTick(0.3f).maxActive(60)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_ROAR);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                ServerPlayer player = ctx.player();
                float hot = booze(player);
                for (LivingEntity target : AbilityDamage.cone(player, 7 * ctx.power(), 25)) {
                    Kit.hit(player, target, AbilityDamage.FIRE, 2f * hot * ctx.power());
                    target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
                }
                Vfx.beam(ctx.level(), player, player.getEyePosition().add(player.getLookAngle().scale(0.6)),
                        player.getEyePosition().add(player.getLookAngle().scale(7 * ctx.power())), FLAME, 0.8f * hot, Kind.FLAME,
                        Vfx.BEAM_PARTICLES);
                ctx.sound(GLSounds.FIRE_GUN.get(), 0.6f, 0.8f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Spit a ball of flaming liquor that bursts into fire on whatever it hits. */
    public static final class BoozeBomb extends Ability {
        BoozeBomb() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, COLOR).cooldown(70).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, BOMB, 1.3, 0.6f, FLAME, 10f * booze(ctx.player()), 40).withGravity(0.03);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.FIRE_FIST.get(), 0.8f, 1.2f);
        }
    }

    /** Let out a flaming belch: a ring of fire bursts out around you and throws foes back. */
    public static final class FlamingBelch extends Ability {
        FlamingBelch() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, FLAME).cooldown(160).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float hot = booze(player);
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 6 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.FIRE, 8f * hot * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 80));
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.0).add(0, 0.4, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), FLAME, 6f, Kind.FLAME, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 1.2f, 0.9f);
        }
    }

    /** Fight like a drunk: wild, heavy blows, and you sway out of the way of one hit in four. */
    public static final class DrunkenBrawl extends Buffs.Toggle {
        DrunkenBrawl() {
            super(props(ActivationType.TOGGLE, AbilityIcon.FIST, COLOR).cooldown(240).stamina(12).staminaPerTick(0.15f)
                            .maxActive(500).mastery(M, 35).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("guzzle_brawl_damage"), 5, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("guzzle_brawl_kb"), 0.5, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_SPEED, GrandLine.id("guzzle_brawl_speed"), -0.5, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            ctx.sound(SoundEvents.GENERIC_DRINK, 1f, 0.6f);
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            if ((event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK))
                    && ctx.level().random.nextFloat() < 0.25f) {
                event.setCanceled(true);
                ctx.sound(GLSounds.EVADE.get(), 1f, 0.8f);
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1f, Kind.SMOKE);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Breathe out every drop at once: a roaring inferno that engulfs everything in front of you. */
    public static final class InfernoBreath extends Ability {
        InfernoBreath() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, COLOR).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float hot = booze(player);
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 14 * ctx.power(), 35)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 18f * hot * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 160));
            }
            for (int i = 1; i <= 4; i++) {
                Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(i * 3.2)), FLAME, 1.6f + i * 0.8f, Kind.FLAME, 0);
            }
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 1.8f, 0.6f);
        }
    }

    /** Spill a sea of burning liquor around you: for ten seconds, everything standing in it burns. */
    public static final class SeaOfBooze extends Ability {
        SeaOfBooze() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, FLAME).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, SEA);
            AbilityProjectile.zone(player, SEA, player.position(), 10 * ctx.power(), FLAME, 4f * booze(player) * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 10f, Kind.FLAME, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FLAME_WALL.get(), 1.6f, 0.7f);
        }
    }
}
