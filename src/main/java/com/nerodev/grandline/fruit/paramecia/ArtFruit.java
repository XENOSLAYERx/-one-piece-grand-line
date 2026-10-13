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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Ato Ato no Mi. Turn people and places into abstract art: bodies twist into shapes that can barely walk straight or
 * hold a weapon, and the ground itself becomes a confusing canvas.
 */
public class ArtFruit extends DevilFruit {
    public static final int COLOR = 0x4AC8C8;
    public static final int PINK = 0xF04A8A;
    public static final int[] PALETTE = {0x4AC8C8, 0xF04A8A, 0xF0D040, 0x6A4AE8, 0x6AE06A};
    static final String M = "fruit/grandline:art";

    public static final DeferredHolder<Ability, BrushStroke> BRUSH_STROKE = ability("art_brush_stroke", BrushStroke::new);
    public static final DeferredHolder<Ability, PaintSplash> PAINT_SPLASH = ability("art_paint_splash", PaintSplash::new);
    public static final DeferredHolder<Ability, Canvas> CANVAS = ability("art_canvas", Canvas::new);
    public static final DeferredHolder<Ability, Frame> FRAME = ability("art_frame", Frame::new);
    public static final DeferredHolder<Ability, Masterpiece> MASTERPIECE = ability("art_masterpiece", Masterpiece::new);
    public static final DeferredHolder<Ability, Gallery> GALLERY = ability("art_gallery", Gallery::new);
    public static final DeferredHolder<Ability, Sculpture> SCULPTURE = ability("art_sculpture", Sculpture::new);

    public static final ResourceLocation PAINT = GrandLine.id("art_paint");
    public static final ResourceLocation CANVAS_ZONE = GrandLine.id("art_canvas");

    public ArtFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Ato Ato no Mi", COLOR)
                .abilities(BRUSH_STROKE, PAINT_SPLASH, CANVAS, FRAME, MASTERPIECE)
                .awakened(GALLERY, SCULPTURE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(PAINT, Projectiles.bolt(AbilityDamage.STRIKE, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }));
        ProjectileBehavior.Registry.register(CANVAS_ZONE, Projectiles.zone(10, (p, target) -> {
            abstractify(target, 40);
            SlipFruit.slide(target, 0.3);
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.damage() > 0 && p.tickCount % 20 == 0) {
                Kit.hit(owner, target, AbilityDamage.DRAIN, p.damage());
            }
        }, null));
    }

    /** Turns a body into abstract art: it staggers, can't see straight and can barely swing. */
    static void abstractify(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks + 40, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        if (target.level() instanceof ServerLevel level) {
            int color = PALETTE[target.getRandom().nextInt(PALETTE.length)];
            Vfx.spiral(level, target, target.getBoundingBox().getCenter(), color, 0.8f, Kind.STAR);
        }
    }

    static boolean isArt(LivingEntity target) {
        return target.hasEffect(MobEffects.CONFUSION);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** One sweeping brush stroke turns the foe in front of you into a twisted work of art. */
    public static final class BrushStroke extends Ability {
        BrushStroke() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(60).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5 * ctx.power(), 40)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 5f * ctx.power());
                abstractify(target, 100);
            }
            for (int i = 0; i < 3; i++) {
                Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.5 + i * 0.4)), player.getLookAngle(),
                        PALETTE[i], 1.8f, 1);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.BOOK_PAGE_TURN, 1.4f, 0.8f);
        }
    }

    /** Fling three blots of paint; whoever they splash is blinded and slowed. */
    public static final class PaintSplash extends Ability {
        PaintSplash() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, PINK).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.12)).normalize();
                AbilityProjectile.spawn(player, PAINT, ctx.handPos(), dir.scale(1.5), 0.4f, PALETTE[i + 1], 3f * ctx.power(), 25)
                        .withGravity(0.03);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SLIME_SQUISH_SMALL, 1.2f, 1.2f);
        }
    }

    /** Turn the ground where you point into an abstract canvas: anyone on it staggers in confusion. */
    public static final class Canvas extends Ability {
        Canvas() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(200).stamina(16).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            AbilityProjectile.zone(player, CANVAS_ZONE, at, 5 * ctx.power(), COLOR, 0f, 160);
            Vfx.burst(ctx.level(), player, at, COLOR, 5f, Kind.STAR, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BOOK_PAGE_TURN, 1.6f, 0.5f);
        }
    }

    /** Hang a foe in a picture frame: it is held fast inside the painting for three seconds. */
    public static final class Frame extends Ability {
        Frame() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, PINK).cooldown(200).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.CANDY, 60);
                abstractify(target, 60);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.PAINTING_PLACE, 1.4f, 1f);
        }
    }

    /** Finish your work: every foe you have turned into art nearby takes a blow straight to the soul. */
    public static final class Masterpiece extends Ability {
        Masterpiece() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(200).stamina(18).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 12)) {
                if (isArt(target)) {
                    Kit.hit(player, target, AbilityDamage.DRAIN, 12f * ctx.power());
                    Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), PINK, 1.6f, Kind.STAR, Vfx.FLASH);
                }
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.PAINTING_BREAK, 1.6f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Turn the whole place around you into an abstract gallery: everyone in it is twisted, lost and slowly drained. */
    public static final class Gallery extends Ability {
        Gallery() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, PINK).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            AbilityProjectile.zone(player, CANVAS_ZONE, player.position(), radius, PINK, 3f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), PINK, (float) radius, Kind.STAR, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BOOK_PAGE_TURN, 2f, 0.4f);
        }
    }

    /** A giant abstract sculpture crashes down where you point, flattening and twisting everyone beneath. */
    public static final class Sculpture extends Ability {
        Sculpture() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 26), 8);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 4 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 18f * ctx.power());
                abstractify(target, 120);
            }
            for (int i = 0; i < PALETTE.length; i++) {
                Vfx.trail(ctx.level(), player, at.add(i - 2, 14, 0), at, PALETTE[i], 1.2f, Kind.STAR);
            }
            Vfx.burst(ctx.level(), player, at, PINK, 4f, Kind.SHARD, Vfx.SHOCKWAVE | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ANVIL_LAND, 1.6f, 0.6f);
        }
    }
}
