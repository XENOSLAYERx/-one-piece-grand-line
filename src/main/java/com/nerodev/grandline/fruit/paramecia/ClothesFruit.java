package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

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
import com.nerodev.grandline.fruit.kit.Disguise;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Fuku Fuku no Mi. Make clothes out of anything and dress anyone in an instant: armour for friends, disguises for
 * yourself, and cloth that strips or swaddles a foe.
 */
public class ClothesFruit extends DevilFruit {
    public static final int COLOR = 0xC84A4A;
    public static final int CREAM = 0xF0E0C0;
    static final String M = "fruit/grandline:clothes";

    public static final DeferredHolder<Ability, InstantOutfit> INSTANT_OUTFIT = ability("clothes_instant_outfit", InstantOutfit::new);
    public static final DeferredHolder<Ability, Costume> COSTUME = ability("clothes_costume", Costume::new);
    public static final DeferredHolder<Ability, Unstitch> UNSTITCH = ability("clothes_unstitch", Unstitch::new);
    public static final DeferredHolder<Ability, WrapUp> WRAP_UP = ability("clothes_wrap_up", WrapUp::new);
    public static final DeferredHolder<Ability, ClothGlider> CLOTH_GLIDER = ability("clothes_cloth_glider", ClothGlider::new);
    public static final DeferredHolder<Ability, FullRegalia> FULL_REGALIA = ability("clothes_full_regalia", FullRegalia::new);
    public static final DeferredHolder<Ability, BoltOfCloth> BOLT_OF_CLOTH = ability("clothes_bolt_of_cloth", BoltOfCloth::new);

    public ClothesFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Fuku Fuku no Mi", COLOR)
                .abilities(INSTANT_OUTFIT, COSTUME, UNSTITCH, WRAP_UP, CLOTH_GLIDER)
                .awakened(FULL_REGALIA, BOLT_OF_CLOTH));
    }

    static void init() {}

    static void behaviors() {}

    @Override
    public void onLost(ServerPlayer player) {
        Disguise.clear(player);
    }

    /** The user, allied players and the user's minions within {@code radius}. */
    static List<LivingEntity> friends(ServerPlayer player, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        out.add(player);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            if ((e instanceof Player && !AbilityDamage.canHarm(player, e)) || Minions.isAlly(player, e)) {
                out.add(e);
            }
        }
        return out;
    }

    /** Dresses {@code target} in tough clothes: extra armour for {@code ticks}. */
    static void dress(LivingEntity target, double armor, double toughness, int ticks) {
        Buffs.timed(target, Buffs.mod(Attributes.ARMOR, GrandLine.id("clothes_outfit_armor"), armor, Operation.ADD_VALUE), ticks);
        Buffs.timed(target, Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("clothes_outfit_toughness"), toughness,
                Operation.ADD_VALUE), ticks);
        if (target.level() instanceof net.minecraft.server.level.ServerLevel level) {
            Vfx.burst(level, target, target.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.FEATHER, 0);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Dress yourself and every friend beside you in a tough outfit in an instant: extra armour for thirty seconds. */
    public static final class InstantOutfit extends Ability {
        InstantOutfit() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(400).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : friends(ctx.player(), 6)) {
                dress(friend, 6, 2, 600);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.4f, 1f);
        }
    }

    /**
     * Sew yourself a costume of the creature you're looking at (a zombie if nothing): its kind won't attack you until
     * you strike one of them.
     */
    public static final class Costume extends Ability {
        Costume() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, CREAM).cooldown(300).stamina(12).staminaPerTick(0.05f)
                    .maxActive(1200).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Optional<LivingEntity> target = Kit.target(player, 16);
            if (target.isPresent() && target.get() instanceof Player other) {
                Disguise.asPlayer(player, other.getUUID());
            } else if (target.isPresent() && target.get() instanceof Mob mob) {
                Disguise.as(player, mob.getType());
            } else {
                Disguise.as(player, EntityType.ZOMBIE);
            }
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1.6f, Kind.FEATHER, Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_SIDESTEP);
            ctx.sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.4f, 0.8f);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Disguise.clear(ctx.player());
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), CREAM, 1.4f, Kind.FEATHER, 0);
        }
    }

    /** Unpick a foe's clothes and armour at the seams: its armour counts for far less for ten seconds. */
    public static final class Unstitch extends Ability {
        Unstitch() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(200).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Buffs.timed(target, Buffs.mod(Attributes.ARMOR, GrandLine.id("clothes_unstitch_armor"), -0.6,
                        Operation.ADD_MULTIPLIED_TOTAL), 200);
                Buffs.timed(target, Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("clothes_unstitch_toughness"), -0.6,
                        Operation.ADD_MULTIPLIED_TOTAL), 200);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                Kit.hit(player, target, AbilityDamage.SLASH, 4f * ctx.power());
                Vfx.slash(ctx.level(), player, target.getBoundingBox().getCenter(), player.getLookAngle(), CREAM, 1.2f, 2);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.SHEEP_SHEAR, 1.2f, 1.2f);
        }
    }

    /** Wrap a foe head to foot in a bolt of cloth: it can't move or act for three and a half seconds. */
    public static final class WrapUp extends Ability {
        WrapUp() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, CREAM).cooldown(220).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.STRINGS, 70);
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), CREAM, 0.4f, Kind.FEATHER,
                        Vfx.BEAM_PARTICLES);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.WOOL_PLACE, 1.6f, 0.8f);
        }
    }

    /** Spread a cloak into a wide glider: fall slowly, drift forward, and land softly. */
    public static final class ClothGlider extends Buffs.Toggle {
        ClothGlider() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, COLOR).cooldown(100).stamina(8).staminaPerTick(0.08f)
                            .maxActive(600).mastery(M, 35).masteryGain(0.6),
                    List.of(Buffs.mod(Attributes.SAFE_FALL_DISTANCE, GrandLine.id("clothes_glider_fall"), 40, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            ctx.sound(SoundEvents.ELYTRA_FLYING, 0.6f, 1.4f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 v = player.getDeltaMovement();
            if (!player.onGround() && v.y < -0.12) {
                Vec3 push = ctx.horizontalLook().scale(0.06);
                player.setDeltaMovement(v.x * 0.98 + push.x, -0.12, v.z * 0.98 + push.z);
                player.hurtMarked = true;
                player.resetFallDistance();
                if (ctx.activeTicks() % 6 == 0) {
                    Vfx.impact(ctx.level(), player, player.position().add(0, 2.2, 0), COLOR, 0.8f, Kind.FEATHER);
                }
            }
            return true;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Dress yourself and all your friends in full regalia: heavy armour and quicker feet for a minute. */
    public static final class FullRegalia extends Ability {
        FullRegalia() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(1200).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : friends(ctx.player(), 10)) {
                dress(friend, 10, 4, 1200);
                Buffs.timed(friend, Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("clothes_regalia_speed"), 0.2,
                        Operation.ADD_MULTIPLIED_TOTAL), 1200);
                friend.heal(4f);
            }
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), COLOR, 10f, Kind.FEATHER, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.4f, 1f);
        }
    }

    /** Unroll a great bolt of cloth around you: every foe nearby is wrapped up tight for three seconds. */
    public static final class BoltOfCloth extends Ability {
        BoltOfCloth() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, CREAM).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                Kit.encase(target, Vfx.StatusLook.STRINGS, 60);
                Kit.hit(player, target, AbilityDamage.CRUSH, 6f * ctx.power());
            }
            Vfx.spiral(ctx.level(), player, player.position().add(0, 1, 0), CREAM, 8f, Kind.FEATHER);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.WOOL_PLACE, 2f, 0.5f);
        }
    }
}
