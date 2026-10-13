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
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fruit.kit.TempBlocks;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Hie Hie no Mi. A body of ice: everything it touches freezes, even the sea, which it can walk across. Fire and
 * magma melt it faster than anything else.
 */
public class IceFruit extends LogiaFruit {
    public static final int COLOR = 0x9FE2FF;
    static final String M = "fruit/grandline:ice";

    public static final DeferredHolder<Ability, IceAge> ICE_AGE = ability("ice_age", IceAge::new);
    public static final DeferredHolder<Ability, IceSaber> ICE_SABER = ability("ice_saber", IceSaber::new);
    public static final DeferredHolder<Ability, Partisan> PARTISAN = ability("partisan", Partisan::new);
    public static final DeferredHolder<Ability, PheasantBeak> PHEASANT_BEAK = ability("pheasant_beak", PheasantBeak::new);
    public static final DeferredHolder<Ability, IceTime> ICE_TIME = ability("ice_time", IceTime::new);
    public static final DeferredHolder<Ability, IceWall> ICE_WALL = ability("ice_wall", IceWall::new);
    public static final DeferredHolder<Ability, GlacialWorld> GLACIAL_WORLD = ability("glacial_world", GlacialWorld::new);
    public static final DeferredHolder<Ability, TimeCapsule> TIME_CAPSULE = ability("ice_time_capsule", TimeCapsule::new);

    public static final ResourceLocation SPEAR = GrandLine.id("partisan");
    public static final ResourceLocation PHEASANT = GrandLine.id("pheasant_beak");

    public IceFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.LEGENDARY, "Hie Hie no Mi", COLOR)
                .abilities(ICE_AGE, ICE_SABER, PARTISAN, PHEASANT_BEAK, ICE_TIME, ICE_WALL)
                .awakened(GLACIAL_WORLD, TIME_CAPSULE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SPEAR, Projectiles.piercing(AbilityDamage.FREEZE, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), 160));
        }));
        ProjectileBehavior.Registry.register(PHEASANT, Projectiles.blast(4, AbilityDamage.FREEZE,
                (p, target) -> Kit.encase(target, Vfx.StatusLook.ICE, 40),
                p -> {
                    ServerPlayer owner = p.ownerPlayer();
                    if (owner != null) {
                        Vfx.burst(owner.serverLevel(), p, p.position(), COLOR, 4.5f, Kind.SNOW, Vfx.RING | Vfx.SHOCKWAVE | Vfx.HEAVY);
                        freezeWater(owner.serverLevel(), p.blockPosition(), 5, 200);
                        owner.serverLevel().playSound(null, p.blockPosition(), GLSounds.ICE_SHATTER.get(),
                                net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.8f);
                    }
                }));
    }

    /** Turns water surfaces around {@code center} into packed ice for {@code ticks}. @return blocks frozen */
    static int freezeWater(ServerLevel level, BlockPos center, int radius, int ticks) {
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        int frozen = 0;
        for (int x = -radius; x <= radius && frozen < 400; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) {
                    continue;
                }
                for (int y = -2; y <= 1; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource()
                            && level.getBlockState(pos.above()).isAir() && TempBlocks.place(level, pos, ice, ticks)) {
                        frozen++;
                    }
                }
            }
        }
        return frozen;
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FREEZING);
    }

    @Override
    protected float weakness(ServerPlayer user, DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) ? 1.5f : 1f;
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        // The sea freezes under your feet: walk across it.
        if (user.tickCount % 4 == 0 && !user.isInWater() && !user.isSpectator()) {
            BlockPos below = user.blockPosition().below();
            if (user.level().getFluidState(below).is(FluidTags.WATER)) {
                freezeWater(user.serverLevel(), user.blockPosition(), awakened ? 3 : 2, 100);
            }
        }
        user.setTicksFrozen(0);
    }

    @Override
    public boolean ticks() {
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Freeze everything in front of you: creatures are sealed in ice and the water turns solid. */
    public static final class IceAge extends Ability {
        IceAge() {
            super(props(ActivationType.INSTANT, AbilityIcon.SNOWFLAKE, COLOR).cooldown(200).stamina(24).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 7 * ctx.power();
            Vec3 center = player.position().add(ctx.horizontalLook().scale(radius * 0.8));
            for (LivingEntity target : AbilityDamage.around(player, center, radius)) {
                Kit.hit(player, target, AbilityDamage.FREEZE, 6f * ctx.power());
                Kit.encase(target, Vfx.StatusLook.ICE, 60);
            }
            freezeWater(ctx.level(), BlockPos.containing(center), (int) radius, 300);
            Vfx.burst(ctx.level(), player, center, COLOR, (float) radius, Kind.SNOW, Vfx.RING | Vfx.GROUND);
            Vfx.trail(ctx.level(), player, player.position(), center, COLOR, 2f, Kind.SNOW);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.5f, 0.9f);
        }
    }

    /** Form a sword of ice and cut in a wide arc: deep cold slows whoever it touches. */
    public static final class IceSaber extends Ability {
        IceSaber() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, COLOR).cooldown(30).stamina(8).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5, 60)) {
                Kit.hit(player, target, AbilityDamage.FREEZE, 10f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(2)).add(0, -0.3, 0);
            Vfx.slash(ctx.level(), player, front, player.getLookAngle(), COLOR, 2.2f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.ICE_SHATTER.get(), 0.8f, 1.4f);
        }
    }

    /** A fan of ice spears that pierce everything and freeze it to the bone. */
    public static final class Partisan extends Ability {
        Partisan() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(70).stamina(14).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            Vec3 side = ctx.right();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = look.add(side.scale(i * 0.12)).add(0, Math.abs(i) * 0.03, 0).normalize();
                AbilityProjectile.spawn(player, SPEAR, ctx.handPos().add(side.scale(i * 0.4)), dir.scale(2.0), 0.5f, COLOR,
                        6f * ctx.power(), 20);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1f, 1.4f);
        }
    }

    /** Charge a great bird of ice and send it diving: it bursts and freezes everything around the impact. */
    public static final class PheasantBeak extends Ability {
        PheasantBeak() {
            super(props(ActivationType.CHARGE, AbilityIcon.WAVE, COLOR).cooldown(160).stamina(22).minCharge(10)
                    .maxActive(30).mastery(M, 20).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 0.8f, 0.7f);
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            float charge = Mth.clamp(ctx.activeTicks() / 30f, 0.4f, 1f);
            Kit.shoot(ctx, PHEASANT, 1.3, 1.6f + charge * 1.2f, COLOR, 12f * charge, 30);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.4f, 1f);
        }
    }

    /** Touch a foe and seal it in ice for a long time. */
    public static final class IceTime extends Ability {
        IceTime() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(160).stamina(16).mastery(M, 35)
                    .masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.FREEZE, 4f * ctx.power());
                Kit.encase(target, Vfx.StatusLook.ICE, (int) (120 * ctx.power()));
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.2f, 1.2f);
        }
    }

    /** Raise a wall of solid ice in front of you. */
    public static final class IceWall extends Ability {
        IceWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(200).stamina(14).mastery(M, 50)
                    .masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 center = player.position().add(ctx.horizontalLook().scale(3));
            Kit.place(ctx.level(), Kit.wall(center, ctx.horizontalLook(), 7, 4), Blocks.PACKED_ICE.defaultBlockState(), 240);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), COLOR, 3f, Kind.SNOW, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** The world around you freezes over: every enemy in a huge circle is sealed in ice. */
    public static final class GlacialWorld extends Ability {
        GlacialWorld() {
            super(props(ActivationType.INSTANT, AbilityIcon.SNOWFLAKE, COLOR).cooldown(900).stamina(45).masteryGain(2.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 18 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.FREEZE, 8f * ctx.power());
                Kit.encase(target, Vfx.StatusLook.ICE, 100);
            }
            freezeWater(ctx.level(), player.blockPosition(), (int) Math.min(radius, 16), 600);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, (float) radius, Kind.SNOW,
                    Vfx.RING | Vfx.SHOCKWAVE | Vfx.HEAVY);
            Vfx.rain(ctx.level(), player, player.position(), 0xFFFFFF, (float) radius * 0.6f, Kind.SNOW);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 2.5f, 0.6f);
        }
    }

    /** Seal a foe in a capsule of ice that holds for a long time and wounds as it forms. */
    public static final class TimeCapsule extends Ability {
        TimeCapsule() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(600).stamina(35).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 24).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.FREEZE, 14f * ctx.power());
                Kit.encase(target, Vfx.StatusLook.ICE, 200);
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), COLOR, 0.8f, Kind.SNOW,
                        Vfx.BEAM_GLOW);
            });
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.8f, 0.7f);
        }
    }
}
