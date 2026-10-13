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
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Fude Fude no Mi. Whatever you paint comes to life: ink blades, painted wolves, a crane to ride, flames, serpents,
 * and finally a great eight-headed beast.
 */
public class BrushFruit extends DevilFruit {
    public static final int COLOR = 0xE8E0D0;
    public static final int INK = 0x2A2A2A;
    static final String M = "fruit/grandline:brush";
    public static final ResourceLocation DRAWN = GrandLine.id("brush_drawn");

    public static final DeferredHolder<Ability, InkSlash> INK_SLASH = ability("brush_ink_slash", InkSlash::new);
    public static final DeferredHolder<Ability, PaintedWolves> PAINTED_WOLVES = ability("brush_painted_wolves", PaintedWolves::new);
    public static final DeferredHolder<Ability, InkCrane> INK_CRANE = ability("brush_ink_crane", InkCrane::new);
    public static final DeferredHolder<Ability, PaintedFlames> PAINTED_FLAMES = ability("brush_painted_flames", PaintedFlames::new);
    public static final DeferredHolder<Ability, InkSerpent> INK_SERPENT = ability("brush_ink_serpent", InkSerpent::new);
    public static final DeferredHolder<Ability, Orochi> OROCHI = ability("brush_orochi", Orochi::new);
    public static final DeferredHolder<Ability, InkStorm> INK_STORM = ability("brush_ink_storm", InkStorm::new);

    public static final ResourceLocation SERPENT = GrandLine.id("brush_serpent");
    public static final ResourceLocation STORM = GrandLine.id("brush_storm");

    public BrushFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Fude Fude no Mi", COLOR)
                .abilities(INK_SLASH, PAINTED_WOLVES, INK_CRANE, PAINTED_FLAMES, INK_SERPENT)
                .awakened(OROCHI, INK_STORM));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SERPENT, Projectiles.homing(16, 0.2, Projectiles.piercing(AbilityDamage.DARK, null)));
        ProjectileBehavior.Registry.register(STORM, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.DARK, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
            }
        }, null));
    }

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, DRAWN);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Paint a blade in one stroke and cut with it before the ink dries. */
    public static final class InkSlash extends Ability {
        InkSlash() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, INK).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5, 55)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 8f * ctx.power());
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.6)), player.getLookAngle(), INK, 2f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.BOOK_PAGE_TURN, 1.2f, 1.4f);
        }
    }

    /** Paint two wolves that leap off the page and hunt for you for fifteen seconds. */
    public static final class PaintedWolves extends Ability {
        PaintedWolves() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(400).stamina(16).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Vec3 at : Kit.circle(player.position().add(ctx.horizontalLook().scale(1.5)), 1.2, 2, 0)) {
                Minions.spawn(player, DRAWN, EntityType.WOLF, at, 300,
                        wolf -> wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1)));
                Vfx.burst(ctx.level(), player, at.add(0, 0.5, 0), INK, 1.2f, Kind.SMOKE, 0);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.WOLF_HOWL, 0.8f, 1.2f);
        }
    }

    /** Paint a crane and ride it: you are carried up and forward, then drift gently down. */
    public static final class InkCrane extends Ability {
        InkCrane() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(120).stamina(10).mastery(M, 10)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            Kit.launch(player, look.scale(1.6).add(0, 1.1, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 1.6f, Kind.FEATHER, 0);
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1f, 1.2f);
        }
    }

    /** Paint a wall of flames in front of you: they burn as hot as real ones. */
    public static final class PaintedFlames extends Ability {
        PaintedFlames() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, 0xF0602A).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 8 * ctx.power(), 40)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 9f * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
            }
            Vec3 look = ctx.horizontalLook();
            for (int i = 1; i <= 3; i++) {
                Vfx.burst(ctx.level(), player, player.position().add(look.scale(i * 2.5)).add(0, 1, 0), 0xF0602A, 1.2f + i * 0.5f,
                        Kind.FLAME, 0);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.FIRE_GUN.get(), 1f, 0.9f);
        }
    }

    /** Paint an ink serpent that slithers after the nearest foe and bites through everything in its way. */
    public static final class InkSerpent extends Ability {
        InkSerpent() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, INK).cooldown(100).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SERPENT, 1.0, 0.7f, INK, 10f, 60);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.EVOKER_CAST_SPELL, 1f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Paint your masterpiece, a great serpent beast, and set it loose on your foes for fifteen seconds. */
    public static final class Orochi extends Ability {
        Orochi() {
            super(props(ActivationType.INSTANT, AbilityIcon.TRANSFORM, INK).cooldown(1000).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = player.position().add(ctx.horizontalLook().scale(3));
            Minions.spawn(player, DRAWN, EntityType.RAVAGER, at, 300,
                    beast -> beast.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0)));
            Vfx.burst(ctx.level(), player, at.add(0, 1, 0), INK, 4f, Kind.SMOKE, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.2f, 0.8f);
        }
    }

    /** Fling ink across the sky over the spot you point at: a black rain that blinds and burns for six seconds. */
    public static final class InkStorm extends Ability {
        InkStorm() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, INK).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8);
            AbilityProjectile.zone(player, STORM, at, 8 * ctx.power(), INK, 4f * ctx.power(), 120);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.WEATHER_RAIN, 1.4f, 0.6f);
        }
    }
}
