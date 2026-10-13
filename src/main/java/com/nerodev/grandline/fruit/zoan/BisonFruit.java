package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.bodies;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
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
 * Ushi Ushi no Mi, Model: Bison. Become a charging bison: a thick hide, a hump of muscle and horns that toss anything
 * in the way. Every charge is longer and harder in the full beast.
 */
public class BisonFruit extends ZoanFruit {
    public static final int COLOR = 0x6A4A2A;
    public static final int DARK = 0x2A1A0A;
    static final String M = "fruit/grandline:bison";

    /**
     * @hybrid Bison-Man | Grow horns and a bison's bulk: bigger, harder-hitting, armoured and hard to push.
     * @beast Bison Form | Become a bison: a heavy, fast charger that can't be knocked back.
     */
    static final ZoanForms FORMS = bodies("bison", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.SCALE, 0.3, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.4, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.3, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 0.5, Operation.ADD_VALUE)
                    .dimensions(1.5f, 1.8f, 1.4f));

    public static final DeferredHolder<Ability, HornGore> HORN_GORE = ability("bison_horn_gore", HornGore::new);
    public static final DeferredHolder<Ability, Stampede> STAMPEDE = ability("bison_stampede", Stampede::new);
    public static final DeferredHolder<Ability, ThickHide> THICK_HIDE = ability("bison_thick_hide", ThickHide::new);
    public static final DeferredHolder<Ability, GroundPaw> GROUND_PAW = ability("bison_ground_paw", GroundPaw::new);
    public static final DeferredHolder<Ability, BullRush> BULL_RUSH = ability("bison_bull_rush", BullRush::new);
    public static final DeferredHolder<Ability, GreatStampede> GREAT_STAMPEDE = ability("bison_great_stampede", GreatStampede::new);
    public static final DeferredHolder<Ability, IronHorns> IRON_HORNS = ability("bison_iron_horns", IronHorns::new);

    public BisonFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Ushi Ushi no Mi, Model: Bison", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), HORN_GORE, STAMPEDE, THICK_HIDE, GROUND_PAW, BULL_RUSH)
                .awakened(GREAT_STAMPEDE, IRON_HORNS), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    static float range(ServerPlayer player) {
        return FORMS.beast(player) ? 1.4f : 1f;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Hook your horns through whatever is in front of you and toss it. Needs a form. */
    public static final class HornGore extends Ability {
        HornGore() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(30).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.5 * range(player), 45)) {
                ZoanKit.gore(player, target, 10f * FORMS.boost(player) * ctx.power(), ctx.horizontalLook(), 0.9, 0.7);
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.4)), 0xE0D0B0, 1.4f, Kind.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1f, 0.7f);
        }
    }

    /** Lower your head and charge: everything in your path is gored aside. Needs a form. */
    public static final class Stampede extends Ability {
        Stampede() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, DARK).cooldown(80).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 10 * range(player) * ctx.power();
            ZoanKit.charge(player, length, 1.6, 11f * FORMS.boost(player) * ctx.power(), 1.2);
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), 0x8A6A4A, 1.4f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_STEP, 1.4f, 0.7f);
        }
    }

    /** Brace your thick hide: much tougher and immovable for ten seconds. */
    public static final class ThickHide extends Ability {
        ThickHide() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(300).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
            Buffs.timed(player, Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("bison_hide_kb"), 1, Operation.ADD_VALUE), 200);
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 2f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1f, 0.7f);
        }
    }

    /** Paw the ground and stamp down: everything around you is jolted off its feet. Needs a form. */
    public static final class GroundPaw extends Ability {
        GroundPaw() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, DARK).cooldown(120).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ZoanKit.stomp(player, player.position(), 5 * range(player) * ctx.power(), 9f * FORMS.boost(player) * ctx.power());
            Vfx.burst(ctx.level(), player, player.position(), 0x8A6A4A, 5f * range(player), Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 0.8f);
        }
    }

    /** A long bull rush: everything you hit is gored, dazed and slowed. Needs a form. */
    public static final class BullRush extends Ability {
        BullRush() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(180).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 14 * range(player) * ctx.power();
            for (LivingEntity target : ZoanKit.charge(player, length, 1.8, 13f * FORMS.boost(player) * ctx.power(), 1.4)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            }
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), 0x8A6A4A, 1.8f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A thundering stampede twenty blocks long: everything in the way is gored, and the ground shakes all along it. */
    public static final class GreatStampede extends Ability {
        GreatStampede() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, DARK).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 dir = ctx.horizontalLook();
            ZoanKit.charge(player, 20 * ctx.power(), 2.4, 18f * ctx.power(), 1.6);
            for (int i = 1; i <= 5; i++) {
                Vec3 at = Kit.ground(ctx.level(), from.add(dir.scale(i * 4)), 4);
                ZoanKit.stomp(player, at, 2.5, 4f * ctx.power());
                Vfx.burst(ctx.level(), player, at, 0x8A6A4A, 2.5f, Kind.SHARD, Vfx.GROUND);
            }
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.8f, 0.6f);
        }
    }

    /** Horns hard as iron: a toss that breaks armour, sending everything in front of you flying. Needs a form. */
    public static final class IronHorns extends Ability {
        IronHorns() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, DARK).cooldown(600).stamina(26).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5 * range(player), 50)) {
                ZoanKit.gore(player, target, 20f * ctx.power(), ctx.horizontalLook(), 1.6, 1.0);
                Buffs.timed(target, Buffs.mod(Attributes.ARMOR, GrandLine.id("bison_iron_horns_armor"), -0.5,
                        Operation.ADD_MULTIPLIED_TOTAL), 200);
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(2.5)), 0xE0D0B0, 3f, Kind.SHARD,
                    Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(SoundEvents.ANVIL_LAND, 1f, 0.6f);
        }
    }
}
