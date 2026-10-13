package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.props;

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
 * Hana Hana no Mi. Copies of the user's limbs bloom from any surface they can see, even other people's bodies:
 * arms that grab and bend, eyes that watch, wings of arms, and finally a giant made entirely of limbs.
 */
public class FlowerFruit extends DevilFruit {
    public static final int COLOR = 0xE07AB0;
    public static final int SKIN = 0xE8B898;
    public static final int PETAL = 0xF4A8D0;
    static final String M = "fruit/grandline:flower";

    public static final DeferredHolder<Ability, Clutch> CLUTCH = ability("flower_clutch", Clutch::new);
    public static final DeferredHolder<Ability, Grab> GRAB = ability("flower_grab", Grab::new);
    public static final DeferredHolder<Ability, Ojos> OJOS = ability("flower_ojos", Ojos::new);
    public static final DeferredHolder<Ability, Wing> WING = ability("flower_wing", Wing::new);
    public static final DeferredHolder<Ability, Gigantesco> GIGANTESCO = ability("flower_gigantesco", Gigantesco::new);
    public static final DeferredHolder<Ability, Campanula> CAMPANULA = ability("flower_campanula", Campanula::new);
    public static final DeferredHolder<Ability, Demonio> DEMONIO = ability("flower_demonio", Demonio::new);

    public static final ResourceLocation FIELD = GrandLine.id("flower_campanula");

    public FlowerFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Hana Hana no Mi", COLOR)
                .abilities(CLUTCH, GRAB, OJOS, WING, GIGANTESCO)
                .awakened(CAMPANULA, DEMONIO));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(20, (p, target) -> {
            Kit.root(target, Vfx.StatusLook.HANDS, 25);
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
            }
        }, null));
    }

    /** Arms bloom out of a creature's body. */
    static void bloom(AbilityContext ctx, LivingEntity target) {
        Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), PETAL, 1.2f, Kind.LEAF, 0);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Sprout arms on a foe's body and bend it backwards until something gives.
     *
     * @name Dos Fleur: Clutch
     */
    public static final class Clutch extends Ability {
        Clutch() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(70).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 26).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 26).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.HANDS, 30);
                Kit.hit(player, target, AbilityDamage.CRUSH, 10f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                bloom(ctx, target);
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.SKELETON_HURT, 0.8f, 0.6f);
        }
    }

    /**
     * Arms burst out of the ground around a spot and hold everyone there by the legs.
     *
     * @name Treinta Fleur: Grab
     */
    public static final class Grab extends Ability {
        Grab() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, SKIN).cooldown(140).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 6);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 4 * ctx.power())) {
                Kit.root(target, Vfx.StatusLook.HANDS, 70);
                Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
            }
            Vfx.eruption(ctx.level(), player, at, SKIN, 1.5f, Kind.LEAF);
            Vfx.burst(ctx.level(), player, at, PETAL, 4f, Kind.LEAF, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.FOREST_ROOTS.get(), 1f, 1.4f);
        }
    }

    /**
     * Eyes bloom all around you: every creature within thirty blocks is outlined, even through walls.
     *
     * @name Ojos Fleur
     */
    public static final class Ojos extends Ability {
        Ojos() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, PETAL).cooldown(100).stamina(6).staminaPerTick(0.08f)
                    .maxActive(1200).mastery(M, 10).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.HAKI_SENSE.get(), 0.8f, 1.4f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 20 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(30),
                        e -> e != player && e.isAlive())) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                }
            }
            return true;
        }
    }

    /**
     * Grow wings of arms from your back and fly.
     *
     * @name Cien Fleur: Wing
     */
    public static final class Wing extends Ability {
        Wing() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, COLOR).cooldown(160).stamina(12).staminaPerTick(0.22f)
                    .maxActive(600).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1.3, 0), PETAL, 1.8f, Kind.LEAF, 0);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.resetFallDistance();
            if (ctx.activeTicks() % 8 == 0 && !player.onGround()) {
                Vec3 back = player.position().add(0, 1.3, 0).subtract(ctx.horizontalLook().scale(0.4));
                Vfx.impact(ctx.level(), player, back.add(ctx.right().scale(0.7)), SKIN, 0.6f, Kind.LEAF);
                Vfx.impact(ctx.level(), player, back.add(ctx.right().scale(-0.7)), SKIN, 0.6f, Kind.LEAF);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
        }
    }

    /**
     * Two giant hands rise from the ground where you point and clap together.
     *
     * @name Mil Fleur: Gigantesco Mano
     */
    public static final class Gigantesco extends Ability {
        Gigantesco() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, SKIN).cooldown(200).stamina(22).mastery(M, 35)
                    .masteryGain(1.1));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 3.5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 15f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            Vec3 side = ctx.right().scale(2.2);
            Vfx.eruption(ctx.level(), player, at.add(side), SKIN, 5f, Kind.LEAF);
            Vfx.eruption(ctx.level(), player, at.subtract(side), SKIN, 5f, Kind.LEAF);
            Vfx.burst(ctx.level(), player, at.add(0, 2.5, 0), PETAL, 3f, Kind.LEAF, Vfx.SHOCKWAVE | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * A field of a thousand arms blooms around you, seizing and crushing everyone in it.
     *
     * @name Mil Fleur: Campanula
     */
    public static final class Campanula extends Ability {
        Campanula() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, PETAL).cooldown(700).stamina(36).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            AbilityProjectile.zone(player, FIELD, player.position(), radius, PETAL, 4f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.3, 0), PETAL, (float) radius, Kind.LEAF,
                    Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.FOREST_ROOTS.get(), 2f, 0.9f);
        }
    }

    /**
     * Cloak yourself in countless arms and tower over the battlefield as a giant.
     *
     * @name Demonio Fleur
     */
    public static final class Demonio extends Buffs.Toggle {
        Demonio() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(800).stamina(30).staminaPerTick(0.3f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("flower_demonio_scale"), 1.5, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("flower_demonio_damage"), 8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("flower_demonio_armor"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ENTITY_INTERACTION_RANGE, GrandLine.id("flower_demonio_reach"), 3,
                                    Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), PETAL, 3f, Kind.LEAF, Vfx.FLASH | Vfx.HEAVY);
            ctx.sound(GLSounds.INFLATE.get(), 1.2f, 0.6f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), PETAL, 1.2f, Kind.LEAF);
            }
            return true;
        }
    }
}
