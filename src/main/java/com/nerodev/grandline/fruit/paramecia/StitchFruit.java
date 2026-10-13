package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.props;

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
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Nui Nui no Mi. Sew anything to anything with a needle and thread: a foe's feet to the floor, two foes to each other,
 * your own wounds shut, and a quilted wall out of thin air.
 */
public class StitchFruit extends DevilFruit {
    public static final int COLOR = 0xF0D0A0;
    public static final int THREAD = 0x8A4A2A;
    static final String M = "fruit/grandline:stitch";

    public static final DeferredHolder<Ability, SewFeet> SEW_FEET = ability("stitch_sew_feet", SewFeet::new);
    public static final DeferredHolder<Ability, NeedleVolley> NEEDLE_VOLLEY = ability("stitch_needle_volley", NeedleVolley::new);
    public static final DeferredHolder<Ability, SewTogether> SEW_TOGETHER = ability("stitch_sew_together", SewTogether::new);
    public static final DeferredHolder<Ability, MendWounds> MEND_WOUNDS = ability("stitch_mend_wounds", MendWounds::new);
    public static final DeferredHolder<Ability, QuiltWall> QUILT_WALL = ability("stitch_quilt_wall", QuiltWall::new);
    public static final DeferredHolder<Ability, Patchwork> PATCHWORK = ability("stitch_patchwork", Patchwork::new);
    public static final DeferredHolder<Ability, StitchNet> STITCH_NET = ability("stitch_net", StitchNet::new);

    public static final ResourceLocation NEEDLE = GrandLine.id("stitch_needle");

    public StitchFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Nui Nui no Mi", COLOR)
                .abilities(SEW_FEET, NEEDLE_VOLLEY, SEW_TOGETHER, MEND_WOUNDS, QUILT_WALL)
                .awakened(PATCHWORK, STITCH_NET));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(NEEDLE, Projectiles.bolt(AbilityDamage.SLASH,
                (p, target) -> Kit.root(target, Vfx.StatusLook.STITCHES, 25)));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Stitch a foe's feet to the ground; it cannot take a step until the thread gives. */
    public static final class SewFeet extends Ability {
        SewFeet() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, THREAD).cooldown(80).stamina(10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.STITCHES, 80);
                Vfx.trail(ctx.level(), player, ctx.handPos(), target.position(), THREAD, 0.2f, Kind.SPARK);
            });
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.WOOL_PLACE, 1f, 1.6f);
        }
    }

    /** Throw three needles trailing thread; each one stitches its target in place for a moment. */
    public static final class NeedleVolley extends Ability {
        NeedleVolley() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.1)).normalize();
                AbilityProjectile.spawn(player, NEEDLE, ctx.handPos(), dir.scale(2.2), 0.2f, COLOR, 4f * ctx.power(), 20);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ARROW_SHOOT, 0.8f, 1.8f);
        }
    }

    /** Sew the two foes nearest the spot you point at to each other: they are dragged together and held there. */
    public static final class SewTogether extends Ability {
        SewTogether() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, THREAD).cooldown(160).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.aimPoint(player, 20);
            List<LivingEntity> near = new ArrayList<>(AbilityDamage.around(player, at, 8));
            near.sort((a, b) -> Double.compare(a.distanceToSqr(at), b.distanceToSqr(at)));
            if (near.size() >= 2) {
                LivingEntity a = near.get(0);
                LivingEntity b = near.get(1);
                Vec3 mid = a.position().add(b.position()).scale(0.5);
                Kit.pull(a, mid, 1.0);
                Kit.pull(b, mid, 1.0);
                Kit.root(a, Vfx.StatusLook.STITCHES, 60);
                Kit.root(b, Vfx.StatusLook.STITCHES, 60);
                Kit.hit(player, a, AbilityDamage.CRUSH, 5f * ctx.power());
                Kit.hit(player, b, AbilityDamage.CRUSH, 5f * ctx.power());
                Vfx.beam(ctx.level(), player, a.getBoundingBox().getCenter(), b.getBoundingBox().getCenter(), THREAD, 0.08f, Kind.SPARK,
                        Vfx.BEAM_GLOW);
            } else if (near.size() == 1) {
                Kit.root(near.get(0), Vfx.StatusLook.STITCHES, 60);
            }
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.WOOL_PLACE, 1.4f, 1.2f);
        }
    }

    /** Stitch your own wounds closed: an instant patch-up and a little regeneration. */
    public static final class MendWounds extends Ability {
        MendWounds() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(400).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.heal(8f * ctx.power());
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            Vfx.status(ctx.level(), player, Vfx.StatusLook.STITCHES, 20);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.WOOL_PLACE, 1f, 1.8f);
        }
    }

    /** Quilt a soft but sturdy wall in front of you out of thin air and thread. */
    public static final class QuiltWall extends Ability {
        QuiltWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(200).stamina(14).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(3));
            Kit.place(ctx.level(), Kit.wall(center, dir, 7, 4), Blocks.WHITE_WOOL.defaultBlockState(), 200);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), COLOR, 3f, Kind.FEATHER, 0);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.WOOL_PLACE, 1.6f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Sew every foe around you to the ground at once. */
    public static final class Patchwork extends Ability {
        Patchwork() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, THREAD).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.root(target, Vfx.StatusLook.STITCHES, 80);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 1));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), THREAD, (float) radius, Kind.SPARK, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.WOOL_PLACE, 2f, 0.8f);
        }
    }

    /** Throw a great stitched net over the spot you point at; everyone under it is bound tight and crushed. */
    public static final class StitchNet extends Ability {
        StitchNet() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 6 * ctx.power())) {
                Kit.encase(target, Vfx.StatusLook.STITCHES, 60);
                Kit.hit(player, target, AbilityDamage.CRUSH, 10f * ctx.power());
            }
            Vfx.burst(ctx.level(), player, at.add(0, 3, 0), THREAD, 6f, Kind.SPARK, Vfx.RING);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.WOOL_PLACE, 2f, 0.6f);
        }
    }
}
