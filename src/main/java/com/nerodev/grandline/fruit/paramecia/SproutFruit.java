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
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mosa Mosa no Mi. Make plants grow explosively in an instant: whipping vines, binding brambles, trees that spear up
 * from the ground, thickets that wall you in, and a beanstalk to the sky.
 */
public class SproutFruit extends DevilFruit {
    public static final int COLOR = 0x4AA84A;
    public static final int BLOOM = 0xE0E040;
    static final String M = "fruit/grandline:sprout";

    public static final DeferredHolder<Ability, VineWhip> VINE_WHIP = ability("sprout_vine_whip", VineWhip::new);
    public static final DeferredHolder<Ability, BrambleBind> BRAMBLE_BIND = ability("sprout_bramble_bind", BrambleBind::new);
    public static final DeferredHolder<Ability, Bloom> BLOOM_MOVE = ability("sprout_bloom", Bloom::new);
    public static final DeferredHolder<Ability, TreeSpear> TREE_SPEAR = ability("sprout_tree_spear", TreeSpear::new);
    public static final DeferredHolder<Ability, Thicket> THICKET = ability("sprout_thicket", Thicket::new);
    public static final DeferredHolder<Ability, ForestSurge> FOREST_SURGE = ability("sprout_forest_surge", ForestSurge::new);
    public static final DeferredHolder<Ability, Beanstalk> BEANSTALK = ability("sprout_beanstalk", Beanstalk::new);

    public static final ResourceLocation SURGE = GrandLine.id("sprout_surge");

    public SproutFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Mosa Mosa no Mi", COLOR)
                .abilities(VINE_WHIP, BRAMBLE_BIND, BLOOM_MOVE, TREE_SPEAR, THICKET)
                .awakened(FOREST_SURGE, BEANSTALK));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SURGE, Projectiles.zone(40, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && !target.hasEffect(GLMobEffects.ROOTED)) {
                Kit.root(target, Vfx.StatusLook.ROOTS, 30);
                Kit.hit(owner, target, AbilityDamage.STRIKE, p.damage());
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Lash out with a whip of fast-growing vine: it strikes a foe and drags it to you. */
    public static final class VineWhip extends Ability {
        VineWhip() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, COLOR).cooldown(50).stamina(9).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 10).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 10).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 7f * ctx.power());
                Kit.pull(target, player.position(), 1.2);
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), COLOR, 0.25f, Kind.LEAF,
                        Vfx.BEAM_PARTICLES);
            });
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.FOREST_CREAK.get(), 1f, 1.4f);
        }
    }

    /** Brambles burst up around a foe's legs: it is held fast and pricked by thorns. */
    public static final class BrambleBind extends Ability {
        BrambleBind() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, COLOR).cooldown(160).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.ROOTS, 60);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                Vfx.burst(ctx.level(), player, target.position(), COLOR, 1.4f, Kind.LEAF, Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.FOREST_ROOTS.get(), 1.2f, 1.2f);
        }
    }

    /** Make everything around you bloom: crops and saplings leap up, and you are refreshed. */
    public static final class Bloom extends Ability {
        Bloom() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, BLOOM).cooldown(500).stamina(14).mastery(M, 10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            RipeFruit.ripenCrops(ctx.level(), player.blockPosition(), 6);
            player.heal(4f);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            Vfx.burst(ctx.level(), player, player.position(), BLOOM, 6f, Kind.LEAF, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BONE_MEAL_USE, 1.6f, 1f);
        }
    }

    /** A tree trunk spears up out of the ground beneath a foe and hurls it into the air. */
    public static final class TreeSpear extends Ability {
        TreeSpear() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 12f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.3, 0));
                Vfx.eruption(ctx.level(), player, target.position(), 0x6A4A2A, 4f, Kind.LEAF);
            });
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FOREST_CREAK.get(), 1.4f, 0.7f);
        }
    }

    /** Grow a dense thicket in a ring around you: a living wall for twelve seconds. */
    public static final class Thicket extends Ability {
        Thicket() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(300).stamina(16).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.place(ctx.level(), Kit.ring(player.blockPosition(), 4, 3),
                    Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 240);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 4.5f, Kind.LEAF, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.AZALEA_LEAVES_PLACE, 2f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Flood the land around you with wild growth for ten seconds: roots keep seizing and battering every foe. */
    public static final class ForestSurge extends Ability {
        ForestSurge() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, BLOOM).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, SURGE);
            AbilityProjectile.zone(player, SURGE, player.position(), 10 * ctx.power(), COLOR, 5f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 10f, Kind.LEAF, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FOREST_ROOTS.get(), 2f, 0.6f);
        }
    }

    /** A beanstalk erupts beneath you: you shoot up into the sky and float down, and foes beside you are flung away. */
    public static final class Beanstalk extends Ability {
        Beanstalk() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(600).stamina(26).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 4 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.2).add(0, 0.8, 0));
            }
            Kit.launch(player, new Vec3(0, 2.4, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
            Vfx.eruption(ctx.level(), player, player.position(), COLOR, 10f, Kind.LEAF);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FOREST_CREAK.get(), 2f, 0.5f);
        }
    }
}
