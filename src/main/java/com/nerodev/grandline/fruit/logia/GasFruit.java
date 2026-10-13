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
import com.nerodev.grandline.fruit.LogiaFruit;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Gasu Gasu no Mi. A body of gas: poison clouds, explosive mixtures, and the power to pull the oxygen out of the air
 * around someone. Explosions and poison cannot hurt it.
 */
public class GasFruit extends LogiaFruit {
    public static final int COLOR = 0x9A7AD8;
    public static final int TOXIC = 0xB8E06A;
    static final String M = "fruit/grandline:gas";

    public static final DeferredHolder<Ability, Gastanet> GASTANET = ability("gastanet", Gastanet::new);
    public static final DeferredHolder<Ability, Shinokuni> SHINOKUNI = ability("shinokuni", Shinokuni::new);
    public static final DeferredHolder<Ability, OxygenTheft> OXYGEN_THEFT = ability("oxygen_theft", OxygenTheft::new);
    public static final DeferredHolder<Ability, GasRocket> GAS_ROCKET = ability("gas_rocket", GasRocket::new);
    public static final DeferredHolder<Ability, GasBombs> GAS_BOMBS = ability("gas_bombs", GasBombs::new);
    public static final DeferredHolder<Ability, NoxiousDomain> NOXIOUS_DOMAIN = ability("noxious_domain", NoxiousDomain::new);
    public static final DeferredHolder<Ability, GammaKnife> GAMMA_KNIFE = ability("gamma_knife", GammaKnife::new);

    public static final ResourceLocation CLOUD = GrandLine.id("poison_cloud");
    public static final ResourceLocation CANISTER = GrandLine.id("gas_bomb");
    public static final ResourceLocation DOMAIN = GrandLine.id("noxious_domain");

    public GasFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.EPIC, "Gasu Gasu no Mi", COLOR)
                .abilities(GASTANET, SHINOKUNI, OXYGEN_THEFT, GAS_ROCKET, GAS_BOMBS)
                .awakened(NOXIOUS_DOMAIN, GAMMA_KNIFE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior poison = Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.POISON, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
        }, null);
        ProjectileBehavior.Registry.register(CLOUD, poison);
        ProjectileBehavior.Registry.register(DOMAIN, poison);
        ProjectileBehavior.Registry.register(CANISTER, Projectiles.blast(1.5, AbilityDamage.POISON, null, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                AbilityProjectile.zone(owner, CLOUD, p.position(), 3, TOXIC, 1.5f, 80);
                Vfx.burst(owner.serverLevel(), p, p.position(), TOXIC, 2.5f, Kind.SMOKE, 0);
                owner.serverLevel().playSound(null, p.blockPosition(), GLSounds.GAS_HISS.get(), SoundSource.PLAYERS, 1f, 1.3f);
            }
        }));
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return source.is(AbilityDamage.POISON) || source.is(DamageTypeTags.IS_EXPLOSION);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** A pocket of gas where you look, then a snap of your fingers: it explodes. */
    public static final class Gastanet extends Ability {
        Gastanet() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(60).stamina(14).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.aimPoint(player, 20);
            for (LivingEntity target : AbilityDamage.around(player, at, 3 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.BLAST, 10f * ctx.power());
                AbilityDamage.knockback(target, at, 1.0, 0.4);
            }
            Vfx.burst(ctx.level(), player, at, 0xFFB050, 3f, Kind.FLAME, Vfx.SHOCKWAVE | Vfx.FLASH);
            Vfx.burst(ctx.level(), player, at, COLOR, 2f, Kind.SMOKE, 0);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(GLSounds.GAS_BOOM.get(), 1.4f, 1f);
        }
    }

    /** Release a spreading cloud of poison gas: anyone inside is poisoned and slowed. */
    public static final class Shinokuni extends Ability {
        Shinokuni() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, TOXIC).cooldown(220).stamina(24).mastery(M, 10)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            AbilityProjectile.zone(player, CLOUD, at, 6 * ctx.power(), TOXIC, 2f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, at.add(0, 1, 0), TOXIC, 5f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GAS_HISS.get(), 1.6f, 0.8f);
        }
    }

    /** Hold: take the oxygen from around your target. It chokes, reels and weakens while you keep it up. */
    public static final class OxygenTheft extends Ability {
        OxygenTheft() {
            super(props(ActivationType.HOLD, AbilityIcon.HAND, COLOR).cooldown(200).stamina(14).staminaPerTick(0.35f)
                    .maxActive(80).mastery(M, 20).masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GAS_HISS.get(), 1f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Optional<LivingEntity> target = Kit.target(player, 14);
            if (target.isEmpty()) {
                return false;
            }
            LivingEntity victim = target.get();
            victim.setAirSupply(Math.max(-20, victim.getAirSupply() - 12));
            if (ctx.activeTicks() % 10 == 0) {
                Kit.hit(player, victim, AbilityDamage.POISON, 2f * ctx.power());
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                Vfx.beam(ctx.level(), player, ctx.handPos(), victim.getBoundingBox().getCenter(), COLOR, 0.6f, Kind.SMOKE,
                        Vfx.BEAM_PARTICLES);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Ignite the gas beneath you and rocket away, leaving an explosion behind. */
    public static final class GasRocket extends Ability {
        GasRocket() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(70).stamina(12).mastery(M, 35)
                    .masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            for (LivingEntity target : AbilityDamage.around(player, from, 3)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 6f * ctx.power());
            }
            Kit.dash(player, 2.0, 0.6);
            Vfx.burst(ctx.level(), player, from.add(0, 0.5, 0), 0xFFB050, 2.5f, Kind.FLAME, Vfx.SHOCKWAVE);
            ctx.sound(GLSounds.GAS_BOOM.get(), 1f, 1.4f);
        }
    }

    /** Throw three canisters of gas that burst into small poison clouds. */
    public static final class GasBombs extends Ability {
        GasBombs() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, TOXIC).cooldown(140).stamina(16).mastery(M, 50)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = look.add(ctx.right().scale(i * 0.25)).add(0, 0.25, 0).normalize();
                AbilityProjectile.spawn(player, CANISTER, ctx.handPos(), dir.scale(1.0), 0.5f, TOXIC, 3f * ctx.power(), 60)
                        .withGravity(0.05);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GAS_HISS.get(), 0.8f, 1.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Become a poison storm: a huge cloud that follows you and poisons everyone in it. */
    public static final class NoxiousDomain extends Ability {
        NoxiousDomain() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SKULL, TOXIC).cooldown(600).stamina(30).staminaPerTick(0.25f)
                    .maxActive(200).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, DOMAIN, player.position(), 14 * ctx.power(), TOXIC, 3f * ctx.power(), 210)
                    .following(Vec3.ZERO);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GAS_HISS.get(), 2f, 0.5f);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.discard(ctx.player(), DOMAIN);
        }
    }

    /** A beam of lethal radiation-laced gas that pierces everyone in a line, poisoning them deeply. */
    public static final class GammaKnife extends Ability {
        GammaKnife() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, TOXIC).cooldown(500).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 18, 0.8)) {
                Kit.hit(player, target, AbilityDamage.POISON, 14f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 2));
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), AbilityDamage.lookTarget(player, 18), TOXIC, 0.8f, Kind.SMOKE,
                    Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(GLSounds.GAS_HISS.get(), 1.6f, 1.4f);
        }
    }
}
