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
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Choki Choki no Mi. The hands become scissors that cut anything, stone and steel included, as easily as paper:
 * slice foes, cut the ground into sheets and stand them up as walls.
 */
public class SnipFruit extends DevilFruit {
    public static final int COLOR = 0xC8C8D8;
    public static final int RED = 0xE04A4A;
    static final String M = "fruit/grandline:snip";

    public static final DeferredHolder<Ability, Snip> SNIP = ability("snip_snip", Snip::new);
    public static final DeferredHolder<Ability, PaperWall> PAPER_WALL = ability("snip_paper_wall", PaperWall::new);
    public static final DeferredHolder<Ability, Thrust> THRUST = ability("snip_thrust", Thrust::new);
    public static final DeferredHolder<Ability, GroundFlip> GROUND_FLIP = ability("snip_ground_flip", GroundFlip::new);
    public static final DeferredHolder<Ability, Shear> SHEAR = ability("snip_shear", Shear::new);
    public static final DeferredHolder<Ability, PaperStorm> PAPER_STORM = ability("snip_paper_storm", PaperStorm::new);
    public static final DeferredHolder<Ability, GreatShears> GREAT_SHEARS = ability("snip_great_shears", GreatShears::new);

    public static final ResourceLocation SHEET = GrandLine.id("snip_sheet");

    public SnipFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Choki Choki no Mi", COLOR)
                .abilities(SNIP, PAPER_WALL, THRUST, GROUND_FLIP, SHEAR)
                .awakened(PAPER_STORM, GREAT_SHEARS));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SHEET, Projectiles.piercing(AbilityDamage.SLASH, null));
    }

    /** The block the player stands on (the stuff the ground is made of), for paper walls. */
    static BlockState groundBlock(ServerPlayer player) {
        BlockPos pos = player.blockPosition().below();
        BlockState below = player.level().getBlockState(pos);
        boolean plain = below.isCollisionShapeFullBlock(player.level(), pos) && !below.hasBlockEntity()
                && below.getFluidState().isEmpty();
        return plain ? below : Blocks.DIRT.defaultBlockState();
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Snap your scissor-hands shut on everyone in front of you. */
    public static final class Snip extends Ability {
        Snip() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 10f * ctx.power());
            }
            Vec3 at = ctx.handPos().add(ctx.horizontalLook().scale(1.5));
            Vfx.slash(ctx.level(), player, at, player.getLookAngle().add(ctx.right().scale(0.5)), COLOR, 1.5f, 1);
            Vfx.slash(ctx.level(), player, at, player.getLookAngle().add(ctx.right().scale(-0.5)), COLOR, 1.5f, 1);
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.SHEEP_SHEAR, 1.2f, 0.8f);
        }
    }

    /** Cut a sheet out of the ground in front of you and stand it upright as a wall. */
    public static final class PaperWall extends Ability {
        PaperWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(180).stamina(14).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(3));
            Kit.place(ctx.level(), Kit.wall(center, dir, 5, 4), groundBlock(player), 200);
            Vfx.burst(ctx.level(), player, center, COLOR, 3f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.SHEEP_SHEAR, 1.4f, 0.5f);
        }
    }

    /** Thrust your scissors forward like a spear, piercing a whole line of foes. */
    public static final class Thrust extends Ability {
        Thrust() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, RED).cooldown(70).stamina(12).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 8 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 0.7)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 12f * ctx.power());
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), AbilityDamage.lookTarget(player, range), COLOR, 0.25f, Kind.SPARK,
                    Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.SHEEP_SHEAR, 1f, 1.4f);
        }
    }

    /** Cut the ground under the spot you point at and flip it like paper: everyone standing there is thrown up. */
    public static final class GroundFlip extends Ability {
        GroundFlip() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(140).stamina(16).mastery(M, 20)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 4 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 8f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.1, 0));
            }
            Kit.place(ctx.level(), Kit.wall(at.add(ctx.horizontalLook().scale(-2.5)), ctx.horizontalLook(), 5, 3), groundBlock(player), 100);
            Vfx.burst(ctx.level(), player, at, COLOR, 4f, Kind.SHARD, Vfx.GROUND | Vfx.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SHEEP_SHEAR, 1.6f, 0.5f);
        }
    }

    /** Grab a foe in your scissors and shear: a deep cut that leaves it limping. */
    public static final class Shear extends Ability {
        Shear() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, RED).cooldown(160).stamina(16).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.SLASH, 14f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2));
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), RED, 1.2f, Kind.SHARD, 0);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.SHEEP_SHEAR, 1.4f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Cut the ground into thin sheets and send them spinning out in a fan, slicing through everything. */
    public static final class PaperStorm extends Ability {
        PaperStorm() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = ctx.horizontalLook().add(ctx.right().scale(i * 0.25)).normalize();
                AbilityProjectile.spawn(player, SHEET, player.position().add(0, 1, 0), dir.scale(1.4), 1.2f, COLOR, 10f * ctx.power(),
                        30);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.SHEEP_SHEAR, 2f, 0.7f);
        }
    }

    /** Your scissors swell to giant size and close on everything in front of you. */
    public static final class GreatShears extends Ability {
        GreatShears() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, RED).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 8 * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, range, 40)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 20f * ctx.power());
            }
            Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(range * 0.5));
            Vfx.slash(ctx.level(), player, at, player.getLookAngle().add(ctx.right()), COLOR, (float) range, 1);
            Vfx.slash(ctx.level(), player, at, player.getLookAngle().subtract(ctx.right()), COLOR, (float) range, 1);
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.SAND_SLASH.get(), 1.8f, 0.6f);
        }
    }
}
