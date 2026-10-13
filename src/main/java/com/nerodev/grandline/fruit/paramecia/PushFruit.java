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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Oshi Oshi no Mi. Push and knead the ground like clay: shove it into a surge, sink foes into it, ride it like a wave,
 * and raise it into walls.
 */
public class PushFruit extends DevilFruit {
    public static final int COLOR = 0xA88A5A;
    public static final int DARK = 0x5A4A2A;
    static final String M = "fruit/grandline:push";

    public static final DeferredHolder<Ability, ClayPush> CLAY_PUSH = ability("push_clay_push", ClayPush::new);
    public static final DeferredHolder<Ability, KneadDown> KNEAD_DOWN = ability("push_knead_down", KneadDown::new);
    public static final DeferredHolder<Ability, EarthSurf> EARTH_SURF = ability("push_earth_surf", EarthSurf::new);
    public static final DeferredHolder<Ability, ClayPillar> CLAY_PILLAR = ability("push_clay_pillar", ClayPillar::new);
    public static final DeferredHolder<Ability, EarthWave> EARTH_WAVE = ability("push_earth_wave", EarthWave::new);
    public static final DeferredHolder<Ability, ClayFortress> CLAY_FORTRESS = ability("push_clay_fortress", ClayFortress::new);
    public static final DeferredHolder<Ability, Landslide> LANDSLIDE = ability("push_landslide", Landslide::new);

    public static final ResourceLocation WAVE = GrandLine.id("push_wave");

    public PushFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Oshi Oshi no Mi", COLOR)
                .abilities(CLAY_PUSH, KNEAD_DOWN, EARTH_SURF, CLAY_PILLAR, EARTH_WAVE)
                .awakened(CLAY_FORTRESS, LANDSLIDE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(WAVE, Projectiles.zone(4, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.STRIKE, p.damage());
                Kit.launch(target, p.getDeltaMovement().scale(1.4).add(0, 0.6, 0));
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Shove the ground forward in a surge that bowls over everything in front of you. */
    public static final class ClayPush extends Ability {
        ClayPush() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 6 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * ctx.power());
                Kit.launch(target, look.scale(1.5).add(0, 0.4, 0));
            }
            for (int i = 1; i <= 3; i++) {
                Vfx.burst(ctx.level(), player, player.position().add(look.scale(i * 2)), COLOR, 1.2f + i * 0.4f, Kind.SHARD, Vfx.GROUND);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.MUD_BREAK, 1.4f, 0.6f);
        }
    }

    /** Knead the ground under a foe soft as clay: it sinks to the knees and is stuck. */
    public static final class KneadDown extends Ability {
        KneadDown() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, DARK).cooldown(160).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.MUD, 60);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                Kit.hit(player, target, AbilityDamage.CRUSH, 4f * ctx.power());
                Vfx.burst(ctx.level(), player, target.position(), DARK, 1.6f, Kind.SHARD, Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.MUD_PLACE, 1.6f, 0.6f);
        }
    }

    /** Push the ground beneath you into a rolling wave and ride it forward, bowling over everything in the way. */
    public static final class EarthSurf extends Ability {
        EarthSurf() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(100).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            for (LivingEntity target : AbilityDamage.line(player, 9 * ctx.power(), 1.4)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.8, 0));
            }
            Kit.dash(player, 1.8, 0.2);
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(9)), COLOR, 1.6f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.ROOTED_DIRT_BREAK, 1.6f, 0.7f);
        }
    }

    /** Squeeze the ground under a foe so it shoots up as a pillar of clay and launches it skyward. */
    public static final class ClayPillar extends Ability {
        ClayPillar() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, DARK).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.4, 0));
                Vfx.eruption(ctx.level(), player, target.position(), COLOR, 4f, Kind.SHARD);
            });
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 1.1f);
        }
    }

    /** Send a wave of rolling earth across the ground: it throws everything in its path into the air. */
    public static final class EarthWave extends Ability {
        EarthWave() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, DARK).cooldown(160).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            AbilityProjectile.spawn(player, WAVE, player.position().add(dir.scale(2)).add(0, 0.6, 0), dir.scale(0.6), 5f * ctx.power(),
                    COLOR, 10f * ctx.power(), 35);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ROOTED_DIRT_BREAK, 2f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Push the ground up into a ring of thick clay walls around you for fifteen seconds. */
    public static final class ClayFortress extends Ability {
        ClayFortress() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(900).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.place(ctx.level(), Kit.ring(player.blockPosition(), 5, 4), Blocks.MUD_BRICKS.defaultBlockState(), 300);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 5.5f, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.MUD_BRICKS_PLACE, 2f, 0.5f);
        }
    }

    /** Push the whole hillside forward: a landslide that crushes and buries everything in front of you. */
    public static final class Landslide extends Ability {
        Landslide() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, DARK).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 14 * ctx.power(), 40)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 16f * ctx.power());
                Kit.launch(target, look.scale(1.3).add(0, 0.5, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            }
            for (int i = 1; i <= 5; i++) {
                Vfx.burst(ctx.level(), player, Kit.ground(ctx.level(), player.position().add(look.scale(i * 2.8)), 4), COLOR,
                        1.6f + i * 0.6f, Kind.SHARD, Vfx.GROUND);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.4f);
        }
    }
}
