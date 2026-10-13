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
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Yuki Yuki no Mi. A body of snow: shelters and traps of packed snow, blizzards, snowfields where foes wade and you
 * glide. Cold cannot touch it; heat melts it.
 */
public class SnowFruit extends LogiaFruit {
    public static final int COLOR = 0xF4F8FF;
    public static final int SHADE = 0xB8D4F0;
    static final String M = "fruit/grandline:snow";

    public static final DeferredHolder<Ability, Kamakura> KAMAKURA = ability("kamakura", Kamakura::new);
    public static final DeferredHolder<Ability, SnowRabbits> SNOW_RABBITS = ability("snow_rabbits", SnowRabbits::new);
    public static final DeferredHolder<Ability, Blizzard> BLIZZARD = ability("blizzard", Blizzard::new);
    public static final DeferredHolder<Ability, Snowfield> SNOWFIELD = ability("snowfield", Snowfield::new);
    public static final DeferredHolder<Ability, SnowBurial> SNOW_BURIAL = ability("snow_burial", SnowBurial::new);
    public static final DeferredHolder<Ability, WhiteLand> WHITE_LAND = ability("white_land", WhiteLand::new);
    public static final DeferredHolder<Ability, Avalanche> AVALANCHE = ability("avalanche", Avalanche::new);

    public static final ResourceLocation RABBIT = GrandLine.id("snow_rabbit");
    public static final ResourceLocation FIELD = GrandLine.id("snowfield");
    public static final ResourceLocation SLIDE = GrandLine.id("avalanche");

    public SnowFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.RARE, "Yuki Yuki no Mi", COLOR)
                .abilities(KAMAKURA, SNOW_RABBITS, BLIZZARD, SNOWFIELD, SNOW_BURIAL)
                .awakened(WHITE_LAND, AVALANCHE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(RABBIT, Projectiles.homing(16, 0.22, Projectiles.bolt(AbilityDamage.FREEZE,
                (p, target) -> {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    target.setTicksFrozen(Math.max(target.getTicksFrozen(), 140));
                })));
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(10, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.damage() > 0 && p.tickCount % 20 == 0) {
                Kit.hit(owner, target, AbilityDamage.FREEZE, p.damage());
            }
        }, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 10 == 0 && owner.distanceToSqr(p) < (p.size() / 2) * (p.size() / 2)) {
                owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, false, false, true));
            }
        }));
        // A rolling avalanche that buries and carries everything along.
        ProjectileBehavior.Registry.register(SLIDE, Projectiles.zone(4, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.FREEZE, p.damage());
                Kit.launch(target, p.getDeltaMovement().scale(1.5).add(0, 0.25, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
        }, null));
    }

    /** Lays a thin layer of snow on open ground around {@code center} for {@code ticks}. */
    static void snowGround(ServerLevel level, BlockPos center, int radius, int ticks) {
        int placed = 0;
        for (BlockPos column : Kit.disc(center, radius)) {
            for (int dy = 2; dy >= -2 && placed < 300; dy--) {
                BlockPos pos = column.above(dy);
                BlockPos below = pos.below();
                if (level.getBlockState(pos).isAir() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    if (TempBlocks.place(level, pos, Blocks.SNOW.defaultBlockState(), ticks)) {
                        placed++;
                    }
                    break;
                }
            }
        }
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
        user.setTicksFrozen(0);
    }

    @Override
    public boolean ticks() {
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Raise a dome of packed snow around yourself: a shelter, or a trap for whoever is inside with you. */
    public static final class Kamakura extends Ability {
        Kamakura() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(300).stamina(20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.place(ctx.level(), Kit.dome(player.blockPosition(), 4), Blocks.SNOW_BLOCK.defaultBlockState(), 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 4f, Kind.SNOW, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SNOW_GUST.get(), 1.2f, 0.8f);
        }
    }

    /** Four rabbits of snow bound off after the nearest enemies, freezing whoever they reach. */
    public static final class SnowRabbits extends Ability {
        SnowRabbits() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, COLOR).cooldown(80).stamina(14).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(player.getYRot()) + (i - 1.5) * 0.4;
                Vec3 dir = new Vec3(-Math.sin(a), 0.25, Math.cos(a)).normalize();
                AbilityProjectile.spawn(player, RABBIT, player.position().add(0, 0.6, 0), dir.scale(0.7), 0.6f, COLOR,
                        4f * ctx.power(), 60).withGravity(0.02);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SNOW_GUST.get(), 0.8f, 1.5f);
        }
    }

    /** Hold to blast a freezing blizzard ahead of you that pushes enemies back. */
    public static final class Blizzard extends Ability {
        Blizzard() {
            super(props(ActivationType.HOLD, AbilityIcon.SNOWFLAKE, COLOR).cooldown(140).stamina(12).staminaPerTick(0.45f)
                    .maxActive(60).mastery(M, 10).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SNOW_GUST.get(), 1.4f, 1f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 != 0) {
                return true;
            }
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 8 * ctx.power(), 35)) {
                Kit.hit(player, target, AbilityDamage.FREEZE, 2f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                AbilityDamage.knockback(target, player.position(), 0.35, 0.05);
            }
            Vec3 eye = player.getEyePosition();
            Vfx.beam(ctx.level(), player, ctx.handPos(), eye.add(player.getLookAngle().scale(8)), COLOR, 1.6f, Kind.SNOW,
                    Vfx.BEAM_PARTICLES);
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Blanket the ground in snow: enemies wade through it slowly while it speeds you up. */
    public static final class Snowfield extends Ability {
        Snowfield() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, SHADE).cooldown(260).stamina(22).mastery(M, 20)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 7 * ctx.power();
            AbilityProjectile.zone(player, FIELD, player.position(), radius, COLOR, 0f, 200);
            snowGround(ctx.level(), player.blockPosition(), (int) radius, 200);
            Vfx.rain(ctx.level(), player, player.position(), COLOR, (float) radius, Kind.SNOW);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SNOW_GUST.get(), 1.4f, 0.7f);
        }
    }

    /** Bury your target in a mound of snow that holds it fast. */
    public static final class SnowBurial extends Ability {
        SnowBurial() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(180).stamina(18).mastery(M, 35)
                    .masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.SNOW, 70);
                Kit.hit(player, target, AbilityDamage.FREEZE, 5f * ctx.power());
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), COLOR, 2.5f, Kind.SNOW, Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.SNOW_GUST.get(), 1f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Turn the land around you into a frozen white waste that slows and freezes everyone in it. */
    public static final class WhiteLand extends Ability {
        WhiteLand() {
            super(props(ActivationType.INSTANT, AbilityIcon.SNOWFLAKE, SHADE).cooldown(700).stamina(40).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 14 * ctx.power();
            AbilityProjectile.zone(player, FIELD, player.position(), radius, COLOR, 3f * ctx.power(), 240);
            snowGround(ctx.level(), player.blockPosition(), (int) Math.min(radius, 12), 240);
            Vfx.rain(ctx.level(), player, player.position(), COLOR, (float) radius, Kind.SNOW);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, (float) radius, Kind.SNOW, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SNOW_GUST.get(), 2.2f, 0.5f);
        }
    }

    /** Send an avalanche rolling forward that buries and sweeps away everyone in its path. */
    public static final class Avalanche extends Ability {
        Avalanche() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(500).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            AbilityProjectile.spawn(player, SLIDE, player.position().add(dir.scale(2)), dir.scale(0.55), 8f * ctx.power(), COLOR,
                    6f * ctx.power(), 50);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SNOW_GUST.get(), 2f, 0.6f);
        }
    }
}
