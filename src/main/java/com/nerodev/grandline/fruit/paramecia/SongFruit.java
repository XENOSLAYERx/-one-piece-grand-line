package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

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
import com.nerodev.grandline.fx.Fx;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Uta Uta no Mi. Sing, and everyone who hears pulls into a world made of song: lullabies that put beasts to sleep,
 * notes that strike, ribbons of staff lines that bind, and the demon king of music.
 */
public class SongFruit extends DevilFruit {
    public static final int COLOR = 0xF0F0F0;
    public static final int RED = 0xE84A6A;
    static final String M = "fruit/grandline:song";

    public static final DeferredHolder<Ability, MusicalNote> MUSICAL_NOTE = ability("song_musical_note", MusicalNote::new);
    public static final DeferredHolder<Ability, Lullaby> LULLABY = ability("song_lullaby", Lullaby::new);
    public static final DeferredHolder<Ability, Encore> ENCORE = ability("song_encore", Encore::new);
    public static final DeferredHolder<Ability, RibbonBind> RIBBON_BIND = ability("song_ribbon_bind", RibbonBind::new);
    public static final DeferredHolder<Ability, Fortissimo> FORTISSIMO = ability("song_fortissimo", Fortissimo::new);
    public static final DeferredHolder<Ability, UtaWorld> UTA_WORLD = ability("song_uta_world", UtaWorld::new);
    public static final DeferredHolder<Ability, TotMusica> TOT_MUSICA = ability("song_tot_musica", TotMusica::new);

    public static final ResourceLocation NOTE = GrandLine.id("song_note");
    public static final ResourceLocation WORLD = GrandLine.id("song_world");

    public SongFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Uta Uta no Mi", COLOR)
                .abilities(MUSICAL_NOTE, LULLABY, ENCORE, RIBBON_BIND, FORTISSIMO)
                .awakened(UTA_WORLD, TOT_MUSICA));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(NOTE, Projectiles.homing(16, 0.25, Projectiles.bolt(AbilityDamage.LIGHT, null)));
        ProjectileBehavior.Registry.register(WORLD, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null) {
                return;
            }
            Kit.hit(owner, target, AbilityDamage.LIGHT, p.damage());
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            if (p.tickCount % 60 == 0) {
                sleep(target, target instanceof Player ? 30 : 50);
            }
        }, null));
    }

    /** The song takes hold: a beast falls asleep where it stands; a person drifts off for a moment. */
    static void sleep(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, ticks, 0, false, true, true));
        if (target.level() instanceof net.minecraft.server.level.ServerLevel level) {
            Vfx.impact(level, target, target.getEyePosition().add(0, 0.6, 0), COLOR, 0.9f, Kind.STAR);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Sing a bright note that flies at the nearest foe and strikes it. */
    public static final class MusicalNote extends Ability {
        MusicalNote() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(25).stamina(6).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, NOTE, 1.2, 0.4f, RED, 6f, 40);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1.2f + ctx.level().random.nextFloat() * 0.6f);
        }
    }

    /** Hold to sing a lullaby: foes around you slow, and every two seconds the beasts among them fall asleep. */
    public static final class Lullaby extends Ability {
        Lullaby() {
            super(props(ActivationType.HOLD, AbilityIcon.MOON, COLOR).cooldown(240).stamina(12).staminaPerTick(0.25f).maxActive(100)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FOCUS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 10 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                    if (ctx.activeTicks() > 0 && ctx.activeTicks() % 40 == 0) {
                        if (target instanceof Player) {
                            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        } else {
                            sleep(target, 60);
                        }
                    }
                }
                Vfx.burst(ctx.level(), player, player.position().add(0, 1.6, 0), COLOR, 2f, Kind.STAR, 0);
                ctx.sound(SoundEvents.NOTE_BLOCK_HARP.value(), 0.8f, 0.6f + (ctx.activeTicks() % 40) / 40f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Sing an encore: you and your friends nearby move faster, hit harder and heal for ten seconds. */
    public static final class Encore extends Ability {
        Encore() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, RED).cooldown(500).stamina(16).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : ClothesFruit.friends(ctx.player(), 8)) {
                friend.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
                Vfx.impact(ctx.level(), friend, friend.getEyePosition().add(0, 0.5, 0), RED, 0.9f, Kind.STAR);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.4f, 1.2f);
        }
    }

    /** Lash a foe with ribbons of staff lines: it is bound fast and stung. */
    public static final class RibbonBind extends Ability {
        RibbonBind() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, COLOR).cooldown(200).stamina(14).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.STRINGS, 80);
                Kit.hit(player, target, AbilityDamage.LIGHT, 6f * ctx.power());
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), RED, 0.15f, Kind.STAR,
                        Vfx.BEAM_GLOW);
            });
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.NOTE_BLOCK_GUITAR.value(), 1.2f, 1f);
        }
    }

    /** Hit the loudest note you can: a wall of sound that blasts everything in front of you away. */
    public static final class Fortissimo extends Ability {
        Fortissimo() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, RED).cooldown(160).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 10 * ctx.power(), 35)) {
                Kit.hit(player, target, AbilityDamage.LIGHT, 12f * ctx.power());
                Kit.launch(target, look.scale(1.4).add(0, 0.35, 0));
            }
            for (int i = 1; i <= 3; i++) {
                Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(i * 3)), COLOR, 1.2f + i, Kind.RING, 0);
            }
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.NOTE_BLOCK_BASS.value(), 2f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Pull everyone around you into the world of song for ten seconds: inside, foes are stung and slowed, and every
     * three seconds they drift off to sleep.
     */
    public static final class UtaWorld extends Ability {
        UtaWorld() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, RED).cooldown(1000).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, WORLD);
            AbilityProjectile.zone(player, WORLD, player.position(), 12 * ctx.power(), RED, 3f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 12f, Kind.STAR, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 2f, 0.8f);
        }
    }

    /** Sing the forbidden score and call the demon king of music: a crushing blast of darkness around you. */
    public static final class TotMusica extends Ability {
        TotMusica() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, 0x2A1A2A).cooldown(1000).stamina(36).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.DARK, 18f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.2).add(0, 0.6, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), 0x2A1A2A, 10f, Kind.SMOKE, Vfx.RING | Vfx.HEAVY);
            Vfx.eruption(ctx.level(), player, player.position(), RED, 8f, Kind.STAR);
            ctx.animate(GLEffects.ANIM_CONQUEROR_RELEASE);
            ctx.sound(SoundEvents.WARDEN_SONIC_BOOM, 1.4f, 0.6f);
        }
    }
}
