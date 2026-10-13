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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Oto Oto no Mi. Turn your body into instruments, and every note is an attack: a scratch that cuts at the speed of
 * sound, drums that boom, horns that stagger, and a whole concert of destruction.
 */
public class ToneFruit extends DevilFruit {
    public static final int COLOR = 0xE8E070;
    public static final int BLUE = 0x4A4AA8;
    static final String M = "fruit/grandline:tone";

    public static final DeferredHolder<Ability, Scratch> SCRATCH = ability("tone_scratch", Scratch::new);
    public static final DeferredHolder<Ability, ChestDrum> CHEST_DRUM = ability("tone_chest_drum", ChestDrum::new);
    public static final DeferredHolder<Ability, HornBlast> HORN_BLAST = ability("tone_horn_blast", HornBlast::new);
    public static final DeferredHolder<Ability, MarchRhythm> MARCH_RHYTHM = ability("tone_march_rhythm", MarchRhythm::new);
    public static final DeferredHolder<Ability, ToneBomb> TONE_BOMB = ability("tone_bomb", ToneBomb::new);
    public static final DeferredHolder<Ability, Concert> CONCERT = ability("tone_concert", Concert::new);
    public static final DeferredHolder<Ability, GrandFinale> GRAND_FINALE = ability("tone_grand_finale", GrandFinale::new);

    public static final ResourceLocation STAGE = GrandLine.id("tone_stage");

    public ToneFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Oto Oto no Mi", COLOR)
                .abilities(SCRATCH, CHEST_DRUM, HORN_BLAST, MARCH_RHYTHM, TONE_BOMB)
                .awakened(CONCERT, GRAND_FINALE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(STAGE, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.SHOCK, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Strum the air: the note cuts a foe you can see the instant it's heard. */
    public static final class Scratch extends Ability {
        Scratch() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.SLASH, 8f * ctx.power());
                Vfx.slash(ctx.level(), player, target.getBoundingBox().getCenter(), player.getLookAngle(), COLOR, 1.4f, 1);
            });
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.NOTE_BLOCK_GUITAR.value(), 1.4f, 1.6f);
        }
    }

    /** Beat your chest like a drum: the boom throws back everyone around you. */
    public static final class ChestDrum extends Ability {
        ChestDrum() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, BLUE).cooldown(80).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.2).add(0, 0.4, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 5f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2f, 0.6f);
        }
    }

    /** Blow a horn from your arm: the blast staggers and dizzies everything in front of you. */
    public static final class HornBlast extends Ability {
        HornBlast() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(120).stamina(12).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 10 * ctx.power(), 35)) {
                Kit.hit(player, target, AbilityDamage.SHOCK, 9f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                Kit.launch(target, look.scale(0.8).add(0, 0.2, 0));
            }
            for (int i = 1; i <= 3; i++) {
                Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(i * 3)), COLOR, 1f + i, Kind.RING, 0);
            }
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.RAID_HORN.value(), 0.6f, 1.2f);
        }
    }

    /** Play a marching rhythm: you and your friends nearby move and work faster for fifteen seconds. */
    public static final class MarchRhythm extends Ability {
        MarchRhythm() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, BLUE).cooldown(400).stamina(14).mastery(M, 20).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : ClothesFruit.friends(ctx.player(), 8)) {
                friend.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
                friend.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 1));
                Vfx.impact(ctx.level(), friend, friend.getEyePosition().add(0, 0.5, 0), COLOR, 0.8f, Kind.STAR);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_SNARE.value(), 1.4f, 1f);
        }
    }

    /**
     * Pluck a note that lands on a foe and explodes there, hurting everything around it.
     *
     * @name Tone Bomb
     */
    public static final class ToneBomb extends Ability {
        ToneBomb() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(160).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 18).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 18).ifPresent(target -> {
                Vec3 at = target.getBoundingBox().getCenter();
                for (LivingEntity victim : AbilityDamage.around(player, at, 3 * ctx.power())) {
                    Kit.hit(player, victim, AbilityDamage.BLAST, 14f * ctx.power());
                    Kit.launch(victim, victim.position().subtract(target.position()).normalize().scale(0.8).add(0, 0.5, 0));
                }
                Vfx.burst(ctx.level(), player, at, COLOR, 3f, Kind.RING, Vfx.FLASH | Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.NOTE_BLOCK_BIT.value(), 1.6f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Turn the ground around you into a stage for ten seconds: every foe on it is shocked and dizzied by the music. */
    public static final class Concert extends Ability {
        Concert() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, BLUE).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, STAGE);
            AbilityProjectile.zone(player, STAGE, player.position(), 10 * ctx.power(), COLOR, 4f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 10f, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_PLING.value(), 2f, 1f);
        }
    }

    /** Play every instrument at once: a deafening wall of sound that tears through everything ahead. */
    public static final class GrandFinale extends Ability {
        GrandFinale() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 16 * ctx.power(), 35)) {
                Kit.hit(player, target, AbilityDamage.SHOCK, 18f * ctx.power());
                Kit.launch(target, look.scale(1.5).add(0, 0.4, 0));
            }
            for (int i = 1; i <= 5; i++) {
                Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(i * 3)), i % 2 == 0 ? BLUE : COLOR, 1.2f + i,
                        Kind.RING, i == 5 ? Vfx.HEAVY : 0);
            }
            ctx.animate(GLEffects.ANIM_CONQUEROR_RELEASE);
            ctx.sound(SoundEvents.WARDEN_SONIC_BOOM, 1.4f, 1f);
        }
    }
}
