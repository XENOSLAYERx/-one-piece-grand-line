package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
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
 * Shibo Shibo no Mi. Wring anything like a wet rag, people included, and drink what comes out: it heals you, and enough
 * of it makes you a giant.
 */
public class WringFruit extends DevilFruit {
    public static final int COLOR = 0x8AC8A8;
    public static final int DARK = 0x2A6A5A;
    static final String M = "fruit/grandline:wring";

    public static final DeferredHolder<Ability, Wring> WRING = ability("wring_wring", Wring::new);
    public static final DeferredHolder<Ability, JuiceJet> JUICE_JET = ability("wring_juice_jet", JuiceJet::new);
    public static final DeferredHolder<Ability, Swell> SWELL = ability("wring_swell", Swell::new);
    public static final DeferredHolder<Ability, DryOut> DRY_OUT = ability("wring_dry_out", DryOut::new);
    public static final DeferredHolder<Ability, TwistCut> TWIST_CUT = ability("wring_twist_cut", TwistCut::new);
    public static final DeferredHolder<Ability, DesertWring> DESERT_WRING = ability("wring_desert", DesertWring::new);
    public static final DeferredHolder<Ability, GiantPour> GIANT_POUR = ability("wring_giant_pour", GiantPour::new);

    public WringFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Shibo Shibo no Mi", COLOR)
                .abilities(WRING, JUICE_JET, SWELL, DRY_OUT, TWIST_CUT)
                .awakened(DESERT_WRING, GIANT_POUR));
    }

    static void init() {}

    static void behaviors() {}

    /** Wrings {@code target} dry: damage it can't armour against, it weakens and thirsts, and the user drinks. */
    static void wringOut(ServerPlayer player, LivingEntity target, float damage, float heal, int ticks) {
        if (Kit.hit(player, target, AbilityDamage.DRAIN, damage)) {
            player.heal(heal);
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks, 1));
        Vfx.impact(player.serverLevel(), player, target.getBoundingBox().getCenter(), COLOR, 1.2f,
                Kind.DROP);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Grab a foe and wring it like a rag: you drink what drips out and heal, and it is left weak. */
    public static final class Wring extends Ability {
        Wring() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(80).stamina(10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 6).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6).ifPresent(target -> {
                wringOut(player, target, 10f * ctx.power(), 4f, 100);
                Vfx.beam(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.handPos(), COLOR, 0.25f, Kind.DROP,
                        Vfx.BEAM_PARTICLES);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.SAND_DRAIN.get(), 1f, 1.3f);
        }
    }

    /** Squeeze out the juice you've collected in a high-pressure jet that bowls over everything in a line. */
    public static final class JuiceJet extends Ability {
        JuiceJet() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, COLOR).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (LivingEntity target : AbilityDamage.line(player, 12 * ctx.power(), 1.2)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 7f * ctx.power());
                Kit.launch(target, look.scale(1.2).add(0, 0.25, 0));
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(look.scale(12 * ctx.power())), COLOR, 0.8f,
                    Kind.DROP, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.GENERIC_SPLASH, 1.2f, 0.9f);
        }
    }

    /** Drink down everything you've wrung out and swell into a giant: bigger, harder-hitting, and tougher. */
    public static final class Swell extends Buffs.Toggle {
        Swell() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, DARK).cooldown(300).stamina(16).staminaPerTick(0.2f)
                            .maxActive(600).mastery(M, 10).masteryGain(0.9),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("wring_swell_scale"), 0.8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("wring_swell_damage"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MAX_HEALTH, GrandLine.id("wring_swell_health"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ENTITY_INTERACTION_RANGE, GrandLine.id("wring_swell_reach"), 1.5,
                                    Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), COLOR, 3f, Kind.DROP, Vfx.RING);
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 1.4f, 0.6f);
        }

        @Override
        protected void stopped(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), COLOR, 2f, Kind.DROP, 0);
        }
    }

    /** Wring the moisture out of everyone around you: they shrivel, starving and slow, and you drink. */
    public static final class DryOut extends Ability {
        DryOut() {
            super(props(ActivationType.INSTANT, AbilityIcon.SUN, DARK).cooldown(240).stamina(18).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 6 * ctx.power())) {
                wringOut(player, target, 6f * ctx.power(), 2f, 140);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 1));
                Vfx.beam(ctx.level(), player, target.getBoundingBox().getCenter(), player.getBoundingBox().getCenter(), COLOR, 0.15f,
                        Kind.DROP, Vfx.BEAM_PARTICLES);
            }
            Vfx.burst(ctx.level(), player, player.position(), DARK, 6f, Kind.DROP, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SAND_DRAIN.get(), 1.4f, 0.8f);
        }
    }

    /** Wind your arm round like a wrung rag and let it whip loose in a twisting sword cut. */
    public static final class TwistCut extends Ability {
        TwistCut() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, COLOR).cooldown(100).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5 * ctx.power(), 60)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 12f * ctx.power());
            }
            Vec3 at = ctx.handPos().add(ctx.horizontalLook().scale(1.8));
            Vfx.slash(ctx.level(), player, at, player.getLookAngle(), COLOR, 2.6f, 2);
            Vfx.spiral(ctx.level(), player, at, DARK, 1.6f, Kind.DROP);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Wring the whole battlefield dry: everyone around you is drained at once, and every drop heals you.
     *
     * @name Desert Wring
     */
    public static final class DesertWring extends Ability {
        DesertWring() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, DARK).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                wringOut(player, target, 9f * ctx.power(), 3f, 200);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2));
                Vfx.beam(ctx.level(), player, target.getBoundingBox().getCenter(), player.getBoundingBox().getCenter(), COLOR, 0.2f,
                        Kind.DROP, Vfx.BEAM_PARTICLES);
            }
            Vfx.spiral(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 10f, Kind.DROP);
            Vfx.burst(ctx.level(), player, player.position(), DARK, 10f, Kind.SMOKE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SAND_STORM.get(), 1.6f, 0.8f);
        }
    }

    /** Pour out a giant's store of wrung juice in a crushing torrent that blasts everything in front of you away. */
    public static final class GiantPour extends Ability {
        GiantPour() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 14 * ctx.power(), 35)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 16f * ctx.power());
                Kit.launch(target, look.scale(1.8).add(0, 0.4, 0));
            }
            for (int i = 1; i <= 4; i++) {
                Vfx.burst(ctx.level(), player, player.position().add(look.scale(i * 3.2)).add(0, 0.5, 0), COLOR, 2f + i * 0.6f, Kind.DROP,
                        Vfx.GROUND);
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(look.scale(14 * ctx.power())), COLOR, 2.5f,
                    Kind.DROP, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.GENERIC_SPLASH, 2f, 0.5f);
        }
    }
}
