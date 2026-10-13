package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
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
 * Toge Toge no Mi. Spikes grow from anywhere on the body: fingertips that stab, hair that whips, a sea-urchin body
 * that punishes every touch.
 */
public class SpikeFruit extends DevilFruit {
    public static final int COLOR = 0xB84A8A;
    public static final int DARK = 0x4A2A3A;
    static final String M = "fruit/grandline:spike";

    public static final DeferredHolder<Ability, Sting> STING = ability("spike_sting", Sting::new);
    public static final DeferredHolder<Ability, Volley> VOLLEY = ability("spike_volley", Volley::new);
    public static final DeferredHolder<Ability, Urchin> URCHIN = ability("spike_urchin", Urchin::new);
    public static final DeferredHolder<Ability, GroundSpikes> GROUND_SPIKES = ability("spike_ground_spikes", GroundSpikes::new);
    public static final DeferredHolder<Ability, HairFlail> HAIR_FLAIL = ability("spike_hair_flail", HairFlail::new);
    public static final DeferredHolder<Ability, SpikeForest> SPIKE_FOREST = ability("spike_forest", SpikeForest::new);
    public static final DeferredHolder<Ability, RollingUrchin> ROLLING = ability("spike_rolling_urchin", RollingUrchin::new);

    public static final ResourceLocation NEEDLE = GrandLine.id("spike_needle");
    public static final ResourceLocation RING = GrandLine.id("spike_ring");

    public SpikeFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Toge Toge no Mi", COLOR)
                .abilities(STING, VOLLEY, URCHIN, GROUND_SPIKES, HAIR_FLAIL)
                .awakened(SPIKE_FOREST, ROLLING));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(NEEDLE, Projectiles.bolt(AbilityDamage.SLASH, null));
        // A ring of spikes racing outward: everyone the ring's edge passes over is impaled once.
        ProjectileBehavior.Registry.register(RING, Projectiles.zone(Integer.MAX_VALUE, (p, target) -> {}, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null || p.tickCount % 2 != 0) {
                return;
            }
            double max = p.size() / 2.0;
            double edge = Math.min(max, 1.5 + p.tickCount * 0.6);
            for (LivingEntity target : AbilityDamage.around(owner, p.position(), max)) {
                double d = Math.hypot(target.getX() - p.getX(), target.getZ() - p.getZ());
                if (Math.abs(d - edge) < 0.7) {
                    Kit.hit(owner, target, AbilityDamage.SLASH, p.damage());
                    Kit.launch(target, new Vec3(0, 0.7, 0));
                }
            }
            for (Vec3 at : Kit.circle(p.position(), edge, Math.max(6, (int) (edge * 2)), p.tickCount)) {
                Vfx.impact(owner.serverLevel(), p, at, COLOR, 0.7f, Kind.SHARD);
            }
            if (edge >= max) {
                p.discard();
            }
        }));
    }

    /** Spikes push out wherever the body is struck: bare-handed attackers are pricked (badly, in Urchin form). */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        boolean melee = event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK);
        if (melee && event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != user) {
            boolean urchin = AbilityState.of(user).isActive(URCHIN.getId());
            Kit.hit(user, attacker, AbilityDamage.SLASH, urchin ? 4f : 2f);
            Vfx.impact(user.serverLevel(), user, attacker.getBoundingBox().getCenter(), COLOR, 0.6f, Kind.SHARD);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Grow a long spike from your fingertip and stab straight through whatever is in front of you. */
    public static final class Sting extends Ability {
        Sting() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(40).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 8 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 0.6)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 9f * ctx.power());
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), AbilityDamage.lookTarget(player, range), COLOR, 0.2f, Kind.SHARD,
                    Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 0.8f, 1.6f);
        }
    }

    /** Fire a fan of five spikes from your arm. */
    public static final class Volley extends Ability {
        Volley() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, DARK).cooldown(60).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.12)).normalize();
                AbilityProjectile.spawn(player, NEEDLE, ctx.handPos(), dir.scale(2.0), 0.25f, COLOR, 4f * ctx.power(), 20)
                        .withGravity(0.01);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ARROW_SHOOT, 1f, 1.4f);
        }
    }

    /**
     * Bristle all over with spikes: anything close is pricked, and hitting you hurts twice as much.
     *
     * @name Sea Urchin
     */
    public static final class Urchin extends Buffs.Toggle {
        Urchin() {
            super(props(ActivationType.TOGGLE, AbilityIcon.STAR, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(400).mastery(M, 10).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("spike_urchin_armor"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("spike_urchin_speed"), -0.2,
                                    Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.5f, Kind.SHARD, 0);
            ctx.sound(SoundEvents.PUFFER_FISH_BLOW_UP, 1f, 0.8f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.around(player, player.getBoundingBox().getCenter(), 1.8)) {
                    Kit.hit(player, target, AbilityDamage.SLASH, 3f * ctx.power());
                }
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1f, Kind.SHARD);
            }
            return true;
        }

        @Override
        protected void stopped(AbilityContext ctx) {
            ctx.sound(SoundEvents.PUFFER_FISH_BLOW_OUT, 1f, 0.8f);
        }
    }

    /** Drive spikes up out of the ground where you point, impaling and tossing everyone standing there. */
    public static final class GroundSpikes extends Ability {
        GroundSpikes() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, DARK).cooldown(120).stamina(16).mastery(M, 20)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 0.5, 0), 2.6 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.SLASH, 10f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.9, 0));
            }
            Vfx.eruption(ctx.level(), player, at, COLOR, 3f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.POINTED_DRIPSTONE_LAND, 1.2f, 0.8f);
        }
    }

    /** Whip your hair, now a mass of needles, across everything in front of you. */
    public static final class HairFlail extends Ability {
        HairFlail() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(80).stamina(14).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 6 * ctx.power(), 60)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 8f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(2)), player.getLookAngle(), DARK, 3f, 3);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.9f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A ring of spikes bursts out of the ground around you and races outward, impaling everyone it passes. */
    public static final class SpikeForest extends Ability {
        SpikeForest() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, RING, player.position(), 14 * ctx.power(), COLOR, 14f * ctx.power(), 60);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.POINTED_DRIPSTONE_LAND, 2f, 0.5f);
        }
    }

    /** Hold to curl into a giant spiked ball and roll forward, flattening everything in your path. */
    public static final class RollingUrchin extends Ability {
        RollingUrchin() {
            super(props(ActivationType.HOLD, AbilityIcon.ORB, DARK).cooldown(400).stamina(24).staminaPerTick(0.35f).maxActive(50)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_TEKKAI);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Kit.launch(player, new Vec3(dir.x * 0.7, player.getDeltaMovement().y, dir.z * 0.7));
            if (ctx.activeTicks() % 4 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.getBoundingBox().getCenter(), 2.2 * ctx.power())) {
                    Kit.hit(player, target, AbilityDamage.SLASH, 5f * ctx.power());
                    AbilityDamage.knockback(target, player.position(), 1.0, 0.4);
                }
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1.6f, Kind.SHARD);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }
}
