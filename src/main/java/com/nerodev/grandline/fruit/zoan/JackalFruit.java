package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.forms;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Inu Inu no Mi, Model: Jackal. Become a jackal with fearsome jaws: a nose that finds every creature nearby, a lunge
 * that closes any gap and a bite that will not let go. Jackals hunt in packs.
 */
public class JackalFruit extends ZoanFruit {
    public static final int COLOR = 0x6A5A3A;
    public static final int GOLD = 0xE0C870;
    static final String M = "fruit/grandline:jackal";
    public static final ResourceLocation PACK = GrandLine.id("jackal_pack");

    /**
     * @hybrid Jackal-Man | Grow a jackal's head, ears and jaws: quicker, tougher and harder-biting.
     * @beast Jackal Form | Become a jackal: very fast on four legs and light on its feet.
     */
    static final ZoanForms FORMS = forms("jackal", COLOR, () -> SoundEvents.WOLF_GROWL, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.45, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.15, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2, Operation.ADD_VALUE)
                    .dimensions(0.8f, 1.0f, 0.85f));

    public static final DeferredHolder<Ability, JackalFang> JACKAL_FANG = ability("jackal_fang", JackalFang::new);
    public static final DeferredHolder<Ability, HuntersNose> HUNTERS_NOSE = ability("jackal_hunters_nose", HuntersNose::new);
    public static final DeferredHolder<Ability, PackCall> PACK_CALL = ability("jackal_pack_call", PackCall::new);
    public static final DeferredHolder<Ability, Lunge> LUNGE = ability("jackal_lunge", Lunge::new);
    public static final DeferredHolder<Ability, JawLock> JAW_LOCK = ability("jackal_jaw_lock", JawLock::new);
    public static final DeferredHolder<Ability, AnubisJaws> ANUBIS_JAWS = ability("jackal_anubis_jaws", AnubisJaws::new);
    public static final DeferredHolder<Ability, DesertPack> DESERT_PACK = ability("jackal_desert_pack", DesertPack::new);

    public JackalFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Inu Inu no Mi, Model: Jackal", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), JACKAL_FANG, HUNTERS_NOSE, PACK_CALL, LUNGE, JAW_LOCK)
                .awakened(ANUBIS_JAWS, DESERT_PACK), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    @Override
    public void onLost(ServerPlayer player) {
        super.onLost(player);
        Minions.dismiss(player, PACK);
    }

    static void callPack(ServerPlayer player, int count, int ticks, int strength) {
        for (Vec3 at : Kit.circle(player.position(), 2, count, player.getYRot() * Math.PI / 180)) {
            Minions.spawn(player, PACK, EntityType.WOLF, at, ticks, wolf -> {
                if (strength > 0) {
                    wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, strength - 1));
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Snap your jaws on whatever is in front of you: deep bites that keep bleeding. Needs a form.
     *
     * @name Jackal Fang
     */
    public static final class JackalFang extends Ability {
        JackalFang() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(25).stamina(7).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.4, 45)) {
                ZoanKit.maul(player, target, 9f * FORMS.boost(player) * ctx.power(), 4, 0);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.of(player.getEyePosition().add(player.getLookAngle().scale(1.5)), player.getLookAngle())
                    .color(GOLD));
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.WOLF_GROWL, 0.9f, 1.3f);
        }
    }

    /**
     * Catch every scent around you: creatures within thirty blocks stand out for fifteen seconds, and you see in the dark.
     *
     * @name Hunter's Nose
     */
    public static final class HuntersNose extends Ability {
        HuntersNose() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, GOLD).cooldown(400).stamina(6).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0));
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 30)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), GOLD, 2f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.WOLF_AMBIENT, 1f, 1.2f);
        }
    }

    /** Howl for the pack: two jackals come running to hunt with you for fifteen seconds. */
    public static final class PackCall extends Ability {
        PackCall() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(500).stamina(16).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Minions.dismiss(ctx.player(), PACK);
            callPack(ctx.player(), 2, 300, 0);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.WOLF_HOWL, 1.2f, 1.2f);
        }
    }

    /** Lunge at a foe in sight, closing the gap in an instant and biting on arrival. Needs a form. */
    public static final class Lunge extends Ability {
        Lunge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, GOLD).cooldown(80).stamina(10).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No prey in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 to = target.position().subtract(target.position().subtract(from).normalize().scale(1.1));
                player.teleportTo(to.x, to.y, to.z);
                ZoanKit.maul(player, target, 10f * FORMS.boost(player) * ctx.power(), 4, 0);
                Vfx.trail(ctx.level(), player, from.add(0, 0.6, 0), to.add(0, 0.6, 0), COLOR, 0.8f, Kind.SMOKE);
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(GLSounds.CLAW.get(), 1f, 1.1f);
        }
    }

    /** Bite down and hold: the foe is pinned in your jaws and bleeds heavily. Needs a form. */
    public static final class JawLock extends Ability {
        JawLock() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, COLOR).cooldown(200).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.ROOTS, 50);
                ZoanKit.maul(player, target, 8f * FORMS.boost(player) * ctx.power(), 6, 1);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.WOLF_GROWL, 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** The jaws of the god of the dead: a great bite in front of you that tears, bleeds and withers. Needs a form. */
    public static final class AnubisJaws extends Ability {
        AnubisJaws() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, GOLD).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 6 * ctx.power(), 50)) {
                ZoanKit.maul(player, target, 16f * ctx.power(), 6, 2);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(player.getLookAngle().scale(3)), GOLD, 3f, Kind.SMOKE, Vfx.HEAVY);
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.at(player.position().add(player.getLookAngle().scale(2)).add(0, 1, 0)).color(GOLD)
                    .variant(1).scale(4f));
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.WOLF_GROWL, 1.6f, 0.6f);
        }
    }

    /** Call the desert pack: four strong jackals hunt with you for twenty seconds. */
    public static final class DesertPack extends Ability {
        DesertPack() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, GOLD).cooldown(900).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Minions.dismiss(ctx.player(), PACK);
            callPack(ctx.player(), 4, 400, 2);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.WOLF_HOWL, 2f, 0.9f);
        }
    }
}
