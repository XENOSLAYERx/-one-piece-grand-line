package com.nerodev.grandline.fruit.logia;

import static com.nerodev.grandline.fruit.logia.LogiaBatch.ability;
import static com.nerodev.grandline.fruit.logia.LogiaBatch.props;

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
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mori Mori no Mi. A body of living wood: roots that seize, vines that lash, and a hunger that drinks the life out of
 * everything around it. Standing on living ground in the sun feeds it; fire is its enemy.
 */
public class ForestFruit extends LogiaFruit {
    public static final int COLOR = 0x4E9A3A;
    public static final int BARK = 0x6A4A2A;
    public static final int LIFE = 0xA8E070;
    static final String M = "fruit/grandline:forest";

    public static final DeferredHolder<Ability, RootSnare> ROOT_SNARE = ability("root_snare", RootSnare::new);
    public static final DeferredHolder<Ability, VineLash> VINE_LASH = ability("vine_lash", VineLash::new);
    public static final DeferredHolder<Ability, NutrientDrain> NUTRIENT_DRAIN = ability("nutrient_drain", NutrientDrain::new);
    public static final DeferredHolder<Ability, TimberWall> TIMBER_WALL = ability("timber_wall", TimberWall::new);
    public static final DeferredHolder<Ability, ThornField> THORN_FIELD = ability("thorn_field", ThornField::new);
    public static final DeferredHolder<Ability, GreatForest> GREAT_FOREST = ability("great_forest", GreatForest::new);
    public static final DeferredHolder<Ability, GiantTree> GIANT_TREE = ability("giant_tree", GiantTree::new);

    public static final ResourceLocation THORNS = GrandLine.id("thorn_field");
    public static final ResourceLocation WILDWOOD = GrandLine.id("great_forest");

    public ForestFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.LEGENDARY, "Mori Mori no Mi", COLOR)
                .abilities(ROOT_SNARE, VINE_LASH, NUTRIENT_DRAIN, TIMBER_WALL, THORN_FIELD)
                .awakened(GREAT_FOREST, GIANT_TREE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(THORNS, Projectiles.zone(10, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 20 == 0) {
                Kit.hit(owner, target, AbilityDamage.SLASH, p.damage());
            }
        }, null));
        // The wildwood seizes whoever stands in it, and everything it takes flows back to its owner.
        ProjectileBehavior.Registry.register(WILDWOOD, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null) {
                return;
            }
            if (p.tickCount % 60 == 0) {
                Kit.root(target, Vfx.StatusLook.ROOTS, 35);
            }
            if (Kit.hit(owner, target, AbilityDamage.DRAIN, p.damage())) {
                owner.heal(p.damage() * 0.35f);
                if (owner.level() instanceof net.minecraft.server.level.ServerLevel level) {
                    Vfx.trail(level, owner, target.getBoundingBox().getCenter(), owner.position().add(0, 1, 0), LIFE, 0.5f,
                            Kind.LEAF);
                }
            }
        }, null));
    }

    /** Whether {@code player} stands on living ground (grass, dirt, moss, leaves...) open to the sky by day. */
    static boolean rooted(ServerPlayer player) {
        BlockState below = player.level().getBlockState(player.blockPosition().below());
        boolean living = below.is(BlockTags.DIRT) || below.getBlock() instanceof LeavesBlock || below.is(Blocks.MOSS_BLOCK);
        return living && player.level().isDay() && player.level().canSeeSky(player.blockPosition());
    }

    @Override
    protected float weakness(ServerPlayer user, DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) ? 1.5f : 1f;
    }

    /** Photosynthesis: on living ground under the sun the body slowly mends itself and never goes hungry. */
    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.tickCount % 40 != 0 || !rooted(user)) {
            return;
        }
        if (user.getHealth() < user.getMaxHealth()) {
            user.heal(awakened ? 2f : 1f);
        }
        user.getFoodData().setFoodLevel(Math.min(20, user.getFoodData().getFoodLevel() + 1));
    }

    @Override
    public boolean ticks() {
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Roots burst out of the ground where you point, binding the legs of everyone nearby. */
    public static final class RootSnare extends Ability {
        RootSnare() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, BARK).cooldown(120).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            double radius = 3 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
                Kit.root(target, Vfx.StatusLook.ROOTS, 70);
                Kit.hit(player, target, AbilityDamage.CRUSH, 4f * ctx.power());
            }
            Vfx.eruption(ctx.level(), player, at, BARK, 2.5f, Kind.LEAF);
            Vfx.burst(ctx.level(), player, at, BARK, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.FOREST_ROOTS.get(), 1.2f, 1f);
        }
    }

    /** A sweeping lash of thorned vines that cuts everyone in front of you and drags them in. */
    public static final class VineLash extends Ability {
        VineLash() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 8 * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, range, 40)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 6f * ctx.power());
                Kit.pull(target, player.position().add(player.getLookAngle().scale(1.5)), 0.9);
            }
            Vec3 dir = player.getLookAngle();
            Vfx.slash(ctx.level(), player, ctx.handPos().add(dir.scale(2.5)), dir, COLOR, (float) range * 0.6f, 2);
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(dir.scale(range)), COLOR, 0.5f, Kind.LEAF,
                    Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.FOREST_ROOTS.get(), 1f, 1.5f);
        }
    }

    /** Hold to drink the life from every creature around you, healing as they wither. Rooted foes give twice as much. */
    public static final class NutrientDrain extends Ability {
        NutrientDrain() {
            super(props(ActivationType.HOLD, AbilityIcon.HEART, LIFE).cooldown(200).stamina(12).staminaPerTick(0.35f)
                    .maxActive(80).mastery(M, 15).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FOREST_ROOTS.get(), 1f, 0.7f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 != 0) {
                return true;
            }
            ServerPlayer player = ctx.player();
            Vec3 chest = player.position().add(0, 1, 0);
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 7 * ctx.power())) {
                boolean held = target.hasEffect(GLMobEffects.ROOTED) || target.hasEffect(GLMobEffects.ENCASED);
                float amount = (held ? 4f : 2f) * ctx.power();
                if (Kit.hit(player, target, AbilityDamage.DRAIN, amount)) {
                    player.heal(amount * 0.5f);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                    Vfx.trail(ctx.level(), player, target.getBoundingBox().getCenter(), chest, LIFE, 0.5f, Kind.LEAF);
                }
            }
            Vfx.spiral(ctx.level(), player, chest, LIFE, 1.6f, Kind.LEAF);
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Raise a wall of living timber in front of you. */
    public static final class TimberWall extends Ability {
        TimberWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, BARK).cooldown(200).stamina(16).mastery(M, 25)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(3));
            List<BlockPos> wall = Kit.wall(center, dir, 7, 4);
            List<BlockPos> crown = new ArrayList<>();
            for (BlockPos pos : wall) {
                if (pos.getY() == BlockPos.containing(center).getY() + 3) {
                    crown.add(pos);
                }
            }
            wall.removeAll(crown);
            Kit.place(ctx.level(), wall, Blocks.OAK_LOG.defaultBlockState(), 200);
            Kit.place(ctx.level(), crown, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 200);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), COLOR, 3.5f, Kind.LEAF, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FOREST_CREAK.get(), 1.2f, 1f);
        }
    }

    /** Carpet the ground with thorns: anyone who walks through them is slowed and cut. */
    public static final class ThornField extends Ability {
        ThornField() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, COLOR).cooldown(240).stamina(20).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            AbilityProjectile.zone(player, THORNS, at, 5 * ctx.power(), COLOR, 3f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, at, COLOR, 5f, Kind.LEAF, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.FOREST_ROOTS.get(), 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Become a forest: a vast wildwood springs up around you that seizes and drains everyone in it. */
    public static final class GreatForest extends Ability {
        GreatForest() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, LIFE).cooldown(700).stamina(40).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 14 * ctx.power();
            AbilityProjectile.zone(player, WILDWOOD, player.position(), radius, COLOR, 3f * ctx.power(), 240);
            for (Vec3 at : Kit.circle(player.position(), radius * 0.6, 8, player.getRandom().nextDouble())) {
                Vfx.eruption(ctx.level(), player, Kit.ground(ctx.level(), at.add(0, 2, 0), 6), BARK, 4f, Kind.LEAF);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, (float) radius, Kind.LEAF, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FOREST_CREAK.get(), 2.2f, 0.5f);
        }
    }

    /** A giant tree erupts where you point, hurling everyone standing there into the sky. */
    public static final class GiantTree extends Ability {
        GiantTree() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, BARK).cooldown(500).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8);
            Vec3 flat = new Vec3(at.x - player.getX(), 0, at.z - player.getZ());
            if (flat.length() < 3.5) {
                // Never grow the trunk through yourself.
                at = Kit.ground(ctx.level(), player.position().add(ctx.horizontalLook().scale(4)).add(0, 2, 0), 8);
            }
            for (LivingEntity target : AbilityDamage.around(player, at, 3.5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 14f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.4, 0).add(target.position().subtract(at).normalize().scale(0.4)));
            }
            BlockPos base = BlockPos.containing(at);
            List<BlockPos> trunk = new ArrayList<>();
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    trunk.addAll(Kit.pillar(base.offset(dx, 0, dz), 9));
                }
            }
            Kit.place(ctx.level(), trunk, Blocks.OAK_LOG.defaultBlockState(), 240);
            BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            Kit.place(ctx.level(), Kit.dome(base.above(8), 4), leaves, 240);
            Kit.place(ctx.level(), Kit.disc(base.above(8), 4), leaves, 240);
            Vfx.eruption(ctx.level(), player, at, BARK, 9f, Kind.LEAF);
            Vfx.burst(ctx.level(), player, at, BARK, 4f, Kind.SHARD, Vfx.SHOCKWAVE | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FOREST_CREAK.get(), 2f, 0.6f);
        }
    }
}
