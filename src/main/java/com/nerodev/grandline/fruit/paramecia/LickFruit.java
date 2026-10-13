package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

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
 * Pero Pero no Mi. Lick your lips and make candy: spears, walls, cages and floors of hard, sticky sugar that is very
 * hard to break out of.
 */
public class LickFruit extends DevilFruit {
    public static final int COLOR = 0xF06A8A;
    public static final int WHITE = 0xFFFFFF;
    static final String M = "fruit/grandline:lick";

    public static final DeferredHolder<Ability, CandySpear> CANDY_SPEAR = ability("lick_candy_spear", CandySpear::new);
    public static final DeferredHolder<Ability, CandyWall> CANDY_WALL = ability("lick_candy_wall", CandyWall::new);
    public static final DeferredHolder<Ability, CandyCage> CANDY_CAGE = ability("lick_candy_cage", CandyCage::new);
    public static final DeferredHolder<Ability, CandyFloor> CANDY_FLOOR = ability("lick_candy_floor", CandyFloor::new);
    public static final DeferredHolder<Ability, CandyMaiden> CANDY_MAIDEN = ability("lick_candy_maiden", CandyMaiden::new);
    public static final DeferredHolder<Ability, CandyLand> CANDY_LAND = ability("lick_candy_land", CandyLand::new);
    public static final DeferredHolder<Ability, CandyWave> CANDY_WAVE = ability("lick_candy_wave", CandyWave::new);

    public static final ResourceLocation SPEAR = GrandLine.id("lick_spear");
    public static final ResourceLocation FLOOR = GrandLine.id("lick_floor");
    public static final ResourceLocation LAND = GrandLine.id("lick_land");
    public static final ResourceLocation WAVE = GrandLine.id("lick_wave");

    public LickFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Pero Pero no Mi", COLOR)
                .abilities(CANDY_SPEAR, CANDY_WALL, CANDY_CAGE, CANDY_FLOOR, CANDY_MAIDEN)
                .awakened(CANDY_LAND, CANDY_WAVE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SPEAR, Projectiles.piercing(AbilityDamage.SLASH,
                (p, target) -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1))));
        ProjectileBehavior.Registry.register(FLOOR, Projectiles.zone(10, (p, target) -> {
            if (!stuck(target)) {
                Kit.root(target, Vfx.StatusLook.CANDY, 30);
            }
        }, null));
        ProjectileBehavior.Registry.register(LAND, Projectiles.zone(40, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && !stuck(target)) {
                Kit.encase(target, Vfx.StatusLook.CANDY, 40);
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
            }
        }, null));
        ProjectileBehavior.Registry.register(WAVE, Projectiles.zone(4, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && !stuck(target)) {
                Kit.encase(target, Vfx.StatusLook.CANDY, 70);
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
            }
        }, null));
    }

    /** Already held fast (so lingering candy doesn't keep re-coating the same creature). */
    static boolean stuck(LivingEntity target) {
        return target.hasEffect(GLMobEffects.ENCASED) || target.hasEffect(GLMobEffects.ROOTED);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Throw a spear of hard candy that runs through everything in its path and leaves it sticky. */
    public static final class CandySpear extends Ability {
        CandySpear() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SPEAR, 2.0, 0.4f, COLOR, 8f, 25).withGravity(0.01);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 1f, 1.3f);
        }
    }

    /** Raise a wall of pink candy in front of you. */
    public static final class CandyWall extends Ability {
        CandyWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(160).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(3));
            Kit.place(ctx.level(), Kit.wall(center, dir, 7, 4), Blocks.PINK_STAINED_GLASS.defaultBlockState(), 200);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), COLOR, 3f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_PLACE, 1.4f, 1.2f);
        }
    }

    /** Coat a foe in candy that sets hard: it cannot move or act for four seconds. */
    public static final class CandyCage extends Ability {
        CandyCage() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(220).stamina(16).mastery(M, 10).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.CANDY, 80);
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), COLOR, 0.3f, Kind.STAR,
                        Vfx.BEAM_PARTICLES);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.HONEY_BLOCK_PLACE, 1.4f, 0.8f);
        }
    }

    /** Pour a floor of sticky candy where you point: anything that walks onto it is stuck by the feet. */
    public static final class CandyFloor extends Ability {
        CandyFloor() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, WHITE).cooldown(240).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            AbilityProjectile.zone(player, FLOOR, at, 5 * ctx.power(), COLOR, 0f, 180);
            Vfx.burst(ctx.level(), player, at, COLOR, 5f, Kind.STAR, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.HONEY_BLOCK_SLIDE, 1.4f, 0.8f);
        }
    }

    /** Seal a foe in a candy maiden and squeeze: it is held fast and crushed. */
    public static final class CandyMaiden extends Ability {
        CandyMaiden() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, WHITE).cooldown(300).stamina(20).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.CANDY, 60);
                Kit.hit(player, target, AbilityDamage.CRUSH, 12f * ctx.power());
                Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), COLOR, 2f, Kind.SHARD, Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_BREAK, 1.6f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Turn the land around you to candy: every two seconds, anything inside is coated and squeezed. */
    public static final class CandyLand extends Ability {
        CandyLand() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(900).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, LAND);
            AbilityProjectile.zone(player, LAND, player.position(), 12 * ctx.power(), COLOR, 4f * ctx.power(), 240);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 12f, Kind.STAR, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.6f);
        }
    }

    /** Send a rolling wave of molten candy forward that sets hard around everything it catches. */
    public static final class CandyWave extends Ability {
        CandyWave() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, WHITE).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            AbilityProjectile.spawn(player, WAVE, player.position().add(dir.scale(2)).add(0, 1, 0), dir.scale(0.5), 7f * ctx.power(),
                    COLOR, 8f * ctx.power(), 50);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.HONEY_BLOCK_SLIDE, 2f, 0.5f);
        }
    }
}
