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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mira Mira no Mi. Make mirrors that reflect attacks and open into the Mirro-World, a world behind the glass that only
 * the user can enter and leave at will.
 */
public class MirrorFruit extends DevilFruit {
    public static final int COLOR = 0xC8E0F0;
    public static final int VIOLET = 0x8A6AC8;
    static final String M = "fruit/grandline:mirror";

    public static final DeferredHolder<Ability, Reflect> REFLECT = ability("mirror_reflect", Reflect::new);
    public static final DeferredHolder<Ability, MirrorStep> MIRROR_STEP = ability("mirror_step", MirrorStep::new);
    public static final DeferredHolder<Ability, Shards> SHARDS = ability("mirror_shards", Shards::new);
    public static final DeferredHolder<Ability, Dazzle> DAZZLE = ability("mirror_dazzle", Dazzle::new);
    public static final DeferredHolder<Ability, MirroWorld> MIRRO_WORLD = ability("mirror_mirro_world", MirroWorld::new);
    public static final DeferredHolder<Ability, HallOfMirrors> HALL_OF_MIRRORS = ability("mirror_hall", HallOfMirrors::new);
    public static final DeferredHolder<Ability, GlassRain> GLASS_RAIN = ability("mirror_glass_rain", GlassRain::new);

    public static final ResourceLocation SHARD = GrandLine.id("mirror_shard");
    public static final ResourceLocation HALL = GrandLine.id("mirror_hall");
    public static final ResourceLocation RAIN = GrandLine.id("mirror_rain");

    public MirrorFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Mira Mira no Mi", COLOR)
                .abilities(REFLECT, MIRROR_STEP, SHARDS, DAZZLE, MIRRO_WORLD)
                .awakened(HALL_OF_MIRRORS, GLASS_RAIN));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SHARD, Projectiles.bolt(AbilityDamage.SLASH, null));
        // In the Hall of Mirrors, foes see a hundred of you, and every enemy projectile is thrown back.
        ProjectileBehavior.Registry.register(HALL, Projectiles.zone(20, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
        }, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.level() instanceof ServerLevel level) {
                reflect(level, owner, p.getBoundingBox().inflate(p.size() / 2.0));
            }
        }));
        ProjectileBehavior.Registry.register(RAIN, Projectiles.zone(8, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.SLASH, p.damage());
            }
        }, null));
    }

    /** Turns every enemy projectile in {@code box} back the way it came, now belonging to {@code owner}. */
    static void reflect(ServerLevel level, ServerPlayer owner, net.minecraft.world.phys.AABB box) {
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, e -> e.getOwner() != owner
                && !(e instanceof AbilityProjectile ap && ap.extra().getBoolean("mirror_reflected")))) {
            p.setDeltaMovement(p.getDeltaMovement().scale(-1));
            p.setOwner(owner);
            p.hurtMarked = true;
            if (p instanceof AbilityProjectile ap) {
                ap.extra().putBoolean("mirror_reflected", true);
            }
            Vfx.impact(level, owner, p.position(), COLOR, 0.8f, Kind.STAR);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Hold a mirror up in front of you: projectiles that reach it fly straight back at their senders. */
    public static final class Reflect extends Ability {
        Reflect() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, COLOR).cooldown(160).stamina(10).staminaPerTick(0.25f).maxActive(100)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_PLACE, 1f, 1.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(1.5));
            reflect(ctx.level(), player, new net.minecraft.world.phys.AABB(front, front).inflate(2));
            if (ctx.activeTicks() % 6 == 0) {
                Vfx.impact(ctx.level(), player, front, COLOR, 1.4f, Kind.STAR);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Step into one mirror and out of another up to twelve blocks ahead, walls or not. */
    public static final class MirrorStep extends Ability {
        MirrorStep() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, VIOLET).cooldown(60).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 to = DoorFruit.farthestFree(player, player.position(), ctx.horizontalLook(), 12);
            if (to == null) {
                return;
            }
            Vec3 from = player.position();
            player.teleportTo(to.x, to.y, to.z);
            Vfx.blink(ctx.level(), player, from, to, COLOR, Kind.STAR);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.4f);
        }
    }

    /** Shatter a mirror and fling the shards in a fan. */
    public static final class Shards extends Ability {
        Shards() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(50).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -3; i <= 2; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale((i + 0.5) * 0.12)).normalize();
                AbilityProjectile.spawn(player, SHARD, ctx.handPos(), dir.scale(2.0), 0.3f, COLOR, 4f * ctx.power(), 20);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.GLASS_BREAK, 1.2f, 1.2f);
        }
    }

    /** Flash sunlight off a mirror into the eyes of everyone in front of you. */
    public static final class Dazzle extends Ability {
        Dazzle() {
            super(props(ActivationType.INSTANT, AbilityIcon.SUN, COLOR).cooldown(160).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 10 * ctx.power(), 40)) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(player.getLookAngle()), 0xFFFFFF, 2f, Kind.STAR, Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 2f);
        }
    }

    /**
     * Pull a foe through a mirror into the world behind the glass: gone for four seconds.
     *
     * @name Mirro-World
     */
    public static final class MirroWorld extends Ability {
        MirroWorld() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, VIOLET).cooldown(260).stamina(18).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 8).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.target(ctx.player(), 8).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.BARRIER, 80);
                target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false));
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), VIOLET, 1.6f, Kind.STAR, Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Surround yourself with a hall of mirrors: foes inside are lost and dizzy, and every projectile is thrown back. */
    public static final class HallOfMirrors extends Ability {
        HallOfMirrors() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, VIOLET).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            AbilityProjectile.zone(player, HALL, player.position(), radius, COLOR, 0f, 240);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, (float) radius, Kind.STAR, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.8f);
        }
    }

    /** Shatter a sky full of mirrors over the spot you point at: a rain of glass shreds everyone below. */
    public static final class GlassRain extends Ability {
        GlassRain() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 26), 8);
            AbilityProjectile.zone(player, RAIN, at, 6 * ctx.power(), COLOR, 3f * ctx.power(), 80);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.GLASS_BREAK, 2f, 0.6f);
        }
    }
}
