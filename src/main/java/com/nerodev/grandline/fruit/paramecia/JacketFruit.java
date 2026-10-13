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
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Jake Jake no Mi. Become a jacket and be worn: whoever wears you moves as you command. A beast you wrap yourself
 * around fights for you; the sleeves still lash and strangle on their own.
 */
public class JacketFruit extends DevilFruit {
    public static final int COLOR = 0x3A5A8A;
    public static final int LINING = 0xE8E8E8;
    static final String M = "fruit/grandline:jacket";
    public static final ResourceLocation PUPPET = GrandLine.id("jacket_puppet");

    public static final DeferredHolder<Ability, Possess> POSSESS = ability("jacket_possess", Possess::new);
    public static final DeferredHolder<Ability, SleeveLash> SLEEVE_LASH = ability("jacket_sleeve_lash", SleeveLash::new);
    public static final DeferredHolder<Ability, PuppetLunge> PUPPET_LUNGE = ability("jacket_puppet_lunge", PuppetLunge::new);
    public static final DeferredHolder<Ability, LinedCoat> LINED_COAT = ability("jacket_lined_coat", LinedCoat::new);
    public static final DeferredHolder<Ability, Strangle> STRANGLE = ability("jacket_strangle", Strangle::new);
    public static final DeferredHolder<Ability, Wardrobe> WARDROBE = ability("jacket_wardrobe", Wardrobe::new);
    public static final DeferredHolder<Ability, Straitjacket> STRAITJACKET = ability("jacket_straitjacket", Straitjacket::new);

    public JacketFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Jake Jake no Mi", COLOR)
                .abilities(POSSESS, SLEEVE_LASH, PUPPET_LUNGE, LINED_COAT, STRANGLE)
                .awakened(WARDROBE, STRAITJACKET));
    }

    static void init() {}

    static void behaviors() {}

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, PUPPET);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Wrap yourself around a beast like a jacket: for fifteen seconds it fights for you. Players only stagger. */
    public static final class Possess extends Ability {
        Possess() {
            super(props(ActivationType.INSTANT, AbilityIcon.TRANSFORM, COLOR).cooldown(300).stamina(16).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                if (target instanceof Mob mob && Minions.bind(player, PUPPET, mob, 300)) {
                    Vfx.status(ctx.level(), mob, Vfx.StatusLook.STRINGS, 20);
                } else if (target instanceof Player) {
                    target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), COLOR, 1.4f, Kind.SMOKE, 0);
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.2f, 0.8f);
        }
    }

    /** Your sleeves stretch out and lash everyone in front of you. */
    public static final class SleeveLash extends Ability {
        SleeveLash() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, LINING).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 7f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 0.8, 0.2);
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(2)), player.getLookAngle(), COLOR, 2.4f, 2);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 1.4f);
        }
    }

    /** Your puppets leap at the creature you point at and tear into it. */
    public static final class PuppetLunge extends Ability {
        PuppetLunge() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(100).stamina(10).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (Minions.of(player, PUPPET).isEmpty()) {
                return Optional.of(Component.literal("You have no puppets"));
            }
            return Kit.target(player, 24).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                for (Mob puppet : Minions.of(player, PUPPET)) {
                    Kit.pull(puppet, target.position(), 1.4);
                    puppet.setTarget(target);
                    Kit.hit(player, target, AbilityDamage.STRIKE, 4f * ctx.power());
                    Vfx.trail(ctx.level(), puppet, puppet.position().add(0, 1, 0), target.getBoundingBox().getCenter(), COLOR, 0.4f,
                            Kind.SMOKE);
                }
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.4f, 0.6f);
        }
    }

    /** Line yourself with thick padding: tougher and harder to knock down. */
    public static final class LinedCoat extends Buffs.Toggle {
        LinedCoat() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, LINING).cooldown(200).stamina(10).staminaPerTick(0.12f)
                            .maxActive(600).mastery(M, 20).masteryGain(0.7),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("jacket_coat_armor"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("jacket_coat_knockback"), 0.5, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            ctx.sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 0.8f);
        }
    }

    /** Wrap your collar around a foe's throat: it chokes, slows, and a beast passes out for a moment. */
    public static final class Strangle extends Ability {
        Strangle() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, COLOR).cooldown(180).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 7f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                if (!(target instanceof Player)) {
                    target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 30, 0, false, false, true));
                }
                Vfx.status(ctx.level(), target, Vfx.StatusLook.STRINGS, 30);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.2f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Split into a whole wardrobe of jackets: every beast around you is worn and turned to your side for ten seconds. */
    public static final class Wardrobe extends Ability {
        Wardrobe() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(800).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                if (target instanceof Mob mob && Minions.bind(player, PUPPET, mob, 200)) {
                    Vfx.burst(ctx.level(), mob, mob.getBoundingBox().getCenter(), COLOR, 1f, Kind.SMOKE, 0);
                }
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 8f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 2f, 0.5f);
        }
    }

    /** Clamp every foe around you in a straitjacket: none of them can move or swing for three seconds. */
    public static final class Straitjacket extends Ability {
        Straitjacket() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, LINING).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 6 * ctx.power())) {
                Kit.encase(target, Vfx.StatusLook.STRINGS, 60);
                Kit.hit(player, target, AbilityDamage.CRUSH, 6f * ctx.power());
            }
            Vec3 center = player.position().add(0, 1, 0);
            Vfx.burst(ctx.level(), player, center, LINING, 6f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 2f, 0.4f);
        }
    }
}
