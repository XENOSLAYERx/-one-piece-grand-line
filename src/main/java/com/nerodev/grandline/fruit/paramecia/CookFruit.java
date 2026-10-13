package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.zoan.ZoanKit;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Kuku Kuku no Mi. Cook anything into food, as Big Mom's head chef Streusen does: whip up a meal out of thin air,
 * carve foes like ingredients, bake them in dough and season the air with pepper that blinds.
 */
public class CookFruit extends DevilFruit {
    public static final int COLOR = 0xE8B87A;
    public static final int CRUST = 0x8A4A2A;
    public static final int SPICE = 0xC85A2A;
    static final String M = "fruit/grandline:cook";

    public static final DeferredHolder<Ability, ChefsCleaver> CHEFS_CLEAVER = ability("cook_chefs_cleaver", ChefsCleaver::new);
    public static final DeferredHolder<Ability, HotMeal> HOT_MEAL = ability("cook_hot_meal", HotMeal::new);
    public static final DeferredHolder<Ability, Julienne> JULIENNE = ability("cook_julienne", Julienne::new);
    public static final DeferredHolder<Ability, BakeInDough> BAKE = ability("cook_bake_in_dough", BakeInDough::new);
    public static final DeferredHolder<Ability, SeasoningStorm> SEASONING = ability("cook_seasoning_storm", SeasoningStorm::new);
    public static final DeferredHolder<Ability, FullCourse> FULL_COURSE = ability("cook_full_course", FullCourse::new);
    public static final DeferredHolder<Ability, KitchenFrenzy> KITCHEN_FRENZY = ability("cook_kitchen_frenzy", KitchenFrenzy::new);

    public CookFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Kuku Kuku no Mi", COLOR)
                .abilities(CHEFS_CLEAVER, HOT_MEAL, JULIENNE, BAKE, SEASONING)
                .awakened(FULL_COURSE, KITCHEN_FRENZY));
    }

    static void init() {}

    static void behaviors() {}

    /** Drops a freshly cooked dish at {@code at}. */
    static void serve(ServerPlayer player, Vec3 at, ItemStack dish) {
        ItemEntity item = new ItemEntity(player.serverLevel(), at.x, at.y, at.z, dish);
        item.setDefaultPickUpDelay();
        player.serverLevel().addFreshEntity(item);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Chop through everything in front of you with a chef's cleaver.
     *
     * @name Chef's Cleaver
     */
    public static final class ChefsCleaver extends Ability {
        ChefsCleaver() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, CRUST).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4, 60)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 11f * ctx.power());
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(2)), ctx.right(), 0xD8D8E0, 2.4f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        }
    }

    /** Cook a hot meal out of whatever is lying around: you heal on the spot, and dishes are served for your friends. */
    public static final class HotMeal extends Ability {
        HotMeal() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(600).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.heal(6f * ctx.power());
            player.getFoodData().eat(6, 0.8f);
            Vec3 at = player.position().add(ctx.horizontalLook().scale(1.5)).add(0, 0.5, 0);
            serve(player, at, new ItemStack(Items.COOKED_BEEF, 2));
            serve(player, at, new ItemStack(Items.BREAD, 2));
            Vfx.burst(ctx.level(), player, at, COLOR, 1.4f, Kind.SMOKE, 0);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.GENERIC_EAT, 1f, 1f);
        }
    }

    /** Julienne the foe in front of you: five quick cuts in a blur of knives. */
    public static final class Julienne extends Ability {
        Julienne() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, 0xD8D8E0).cooldown(80).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                for (int i = 0; i < 5; i++) {
                    Kit.hit(player, target, AbilityDamage.SLASH, 3.5f * ctx.power());
                }
                Vfx.slash(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.right(), 0xD8D8E0, 1.6f, 4);
            });
            ctx.animate(GLEffects.ANIM_GATLING);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.6f);
        }
    }

    /**
     * Wrap the foe you face in dough and bake it where it stands: sealed and scorched for a few seconds.
     *
     * @name Bake in Dough
     */
    public static final class BakeInDough extends Ability {
        BakeInDough() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, CRUST).cooldown(220).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 10).isPresent() ? Optional.empty() : Optional.of(Component.literal("No one to bake"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 10).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.BISCUIT, 50);
                Kit.hit(player, target, AbilityDamage.FIRE, 12f * ctx.power());
                Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), 0xF08A2A, 1.6f, Kind.FLAME, 0);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 1f, 1.2f);
        }
    }

    /** Fling a storm of pepper and spice ahead of you: foes are blinded, sneezing and burning. */
    public static final class SeasoningStorm extends Ability {
        SeasoningStorm() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, SPICE).cooldown(200).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double reach = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, reach, 55)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 7f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 0));
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(reach)), SPICE, 2.4f,
                    Kind.SMOKE, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SAND_BREAK, 1.4f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Serve a full-course feast: you and every friend nearby are healed, fed and regenerating, and a banquet is laid out.
     *
     * @name Full-Course Feast
     */
    public static final class FullCourse extends Ability {
        FullCourse() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(1200).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity friend : ZoanKit.friends(player, 10)) {
                friend.heal(12f);
                friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                if (friend instanceof Player eater) {
                    eater.getFoodData().eat(10, 1.0f);
                }
            }
            Vec3 at = player.position().add(0, 0.5, 0);
            serve(player, at, new ItemStack(Items.CAKE));
            serve(player, at, new ItemStack(Items.PUMPKIN_PIE, 3));
            serve(player, at, new ItemStack(Items.GOLDEN_CARROT, 2));
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 10f, Kind.STAR, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.PLAYER_BURP, 1f, 1f);
        }
    }

    /** A frenzy of knives in every direction: everything around you is chopped again and again. */
    public static final class KitchenFrenzy extends Ability {
        KitchenFrenzy() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, CRUST).cooldown(700).stamina(26).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                for (int i = 0; i < 4; i++) {
                    Kit.hit(player, target, AbilityDamage.SLASH, 5.5f * ctx.power());
                }
                Vfx.slash(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.right(), 0xD8D8E0, 1.4f, 3);
            }
            Vfx.spiral(ctx.level(), player, player.position(), 0xD8D8E0, 8f, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_FRENZY);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 1.4f);
        }
    }
}
