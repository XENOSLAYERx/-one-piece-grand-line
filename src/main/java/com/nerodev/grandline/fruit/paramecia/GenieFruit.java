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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Hoya Hoya no Mi. Rub your belly and a giant genie of smoke pours out of you to fight at your side: it punches,
 * throws its spear, shields you and grabs your foes.
 */
public class GenieFruit extends DevilFruit {
    public static final int COLOR = 0x8A6AE8;
    public static final int GOLD = 0xF0C040;
    static final String M = "fruit/grandline:genie";

    public static final DeferredHolder<Ability, GeniePunch> GENIE_PUNCH = ability("genie_punch", GeniePunch::new);
    public static final DeferredHolder<Ability, GenieSpear> GENIE_SPEAR = ability("genie_spear", GenieSpear::new);
    public static final DeferredHolder<Ability, GenieGuard> GENIE_GUARD = ability("genie_guard", GenieGuard::new);
    public static final DeferredHolder<Ability, GenieSlam> GENIE_SLAM = ability("genie_slam", GenieSlam::new);
    public static final DeferredHolder<Ability, GenieGrab> GENIE_GRAB = ability("genie_grab", GenieGrab::new);
    public static final DeferredHolder<Ability, GeniesFury> GENIES_FURY = ability("genie_fury", GeniesFury::new);
    public static final DeferredHolder<Ability, LampSmoke> LAMP_SMOKE = ability("genie_lamp_smoke", LampSmoke::new);

    public static final ResourceLocation SPEAR = GrandLine.id("genie_spear");
    public static final ResourceLocation SMOKE = GrandLine.id("genie_smoke");

    public GenieFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Hoya Hoya no Mi", COLOR)
                .abilities(GENIE_PUNCH, GENIE_SPEAR, GENIE_GUARD, GENIE_SLAM, GENIE_GRAB)
                .awakened(GENIES_FURY, LAMP_SMOKE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SPEAR, Projectiles.piercing(AbilityDamage.SLASH, null));
        ProjectileBehavior.Registry.register(SMOKE, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.STRIKE, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
        }, null));
    }

    /** Where the genie looms: beside and above the user's shoulder. */
    static Vec3 genie(AbilityContext ctx) {
        return ctx.player().position().add(ctx.right().scale(-1.2)).add(0, 2.4, 0);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * The genie throws a long punch over your shoulder that knocks down everything in a line.
     *
     * @name Genie Punch
     */
    public static final class GeniePunch extends Ability {
        GeniePunch() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(40).stamina(9).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (LivingEntity target : AbilityDamage.line(player, 7 * ctx.power(), 1.4)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * ctx.power());
                Kit.launch(target, look.scale(1.1).add(0, 0.3, 0));
            }
            Vfx.beam(ctx.level(), player, genie(ctx), player.getEyePosition().add(look.scale(7 * ctx.power())), COLOR, 1.2f, Kind.SMOKE,
                    Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1f, 0.8f);
        }
    }

    /**
     * The genie hurls its spear: it runs through everything in its path.
     *
     * @name Genie Spear
     */
    public static final class GenieSpear extends Ability {
        GenieSpear() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, GOLD).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.spawn(player, SPEAR, genie(ctx), player.getLookAngle().scale(2.0), 0.6f, GOLD, 8f * ctx.power(), 25);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 1f, 0.7f);
        }
    }

    /**
     * The genie wraps its arms around you: much tougher and hard to knock back.
     *
     * @name Genie Guard
     */
    public static final class GenieGuard extends Buffs.Toggle {
        GenieGuard() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(500).mastery(M, 10).masteryGain(0.7),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("genie_guard_armor"), 8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("genie_guard_kb"), 0.5, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 2f, Kind.SMOKE, Vfx.RING);
            ctx.sound(GLSounds.SMOKE_WHOOSH.get(), 1f, 0.8f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 6 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.4f, Kind.SMOKE);
            }
            return true;
        }
    }

    /**
     * The genie brings both fists down on the spot you point at, flattening and throwing everything there.
     *
     * @name Genie Slam
     */
    public static final class GenieSlam extends Ability {
        GenieSlam() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 14), 6);
            for (LivingEntity target : AbilityDamage.around(player, at, 4 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 12f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.9, 0));
            }
            Vfx.burst(ctx.level(), player, at, COLOR, 4f, Kind.SMOKE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 0.9f);
        }
    }

    /**
     * The genie snatches a foe and drags it to you, holding it fast for a moment.
     *
     * @name Genie Grab
     */
    public static final class GenieGrab extends Ability {
        GenieGrab() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, GOLD).cooldown(180).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Vec3 to = player.position().add(ctx.horizontalLook().scale(1.6));
                Vfx.beam(ctx.level(), player, genie(ctx), target.getBoundingBox().getCenter(), COLOR, 0.8f, Kind.SMOKE, Vfx.BEAM_PARTICLES);
                target.teleportTo(to.x, to.y, to.z);
                Kit.root(target, Vfx.StatusLook.SMOKE, 50);
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * ctx.power());
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.SMOKE_BIND.get(), 1f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * The genie flies into a fury: one crushing blow lands on each of the six nearest foes.
     *
     * @name Genie's Fury
     */
    public static final class GeniesFury extends Ability {
        GeniesFury() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, GOLD).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return AbilityDamage.around(player, player.position(), 12).isEmpty() ? Optional.of(Component.literal("No foes nearby"))
                    : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            List<LivingEntity> foes = AbilityDamage.around(player, player.position(), 12 * ctx.power());
            for (LivingEntity target : foes.subList(0, Math.min(6, foes.size()))) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(0.9).add(0, 0.5, 0));
                Vfx.beam(ctx.level(), player, genie(ctx), target.getBoundingBox().getCenter(), COLOR, 0.9f, Kind.SMOKE,
                        Vfx.BEAM_PARTICLES);
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), GOLD, 1.4f, Kind.SHOCKWAVE);
            }
            ctx.animate(GLEffects.ANIM_GATLING);
            ctx.sound(GLSounds.GATLING.get(), 1.2f, 0.7f);
        }
    }

    /** Pour the genie out as a choking cloud of lamp smoke around you: foes inside are blinded and pummelled. */
    public static final class LampSmoke extends Ability {
        LampSmoke() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, SMOKE);
            AbilityProjectile.zone(player, SMOKE, player.position(), 8 * ctx.power(), COLOR, 4f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 8f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SMOKE_WHOOSH.get(), 1.6f, 0.6f);
        }
    }
}
