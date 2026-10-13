package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.props;

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
import com.nerodev.grandline.sound.GLSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Bomu Bomu no Mi. Any part of the body, and anything that leaves it (breath, snot, a kick), can explode. The user is
 * immune to explosions, their own and everyone else's.
 */
public class BombFruit extends DevilFruit {
    public static final int COLOR = 0x4A4A52;
    public static final int FIRE = 0xFF8A2A;
    static final String M = "fruit/grandline:bomb";
    static final String TACKLE = "grandline_bomb_tackle";

    public static final DeferredHolder<Ability, NoseCannon> NOSE_CANNON = ability("bomb_nose_cannon", NoseCannon::new);
    public static final DeferredHolder<Ability, BombKick> BOMB_KICK = ability("bomb_kick", BombKick::new);
    public static final DeferredHolder<Ability, BreathBomb> BREATH_BOMB = ability("bomb_breath_bomb", BreathBomb::new);
    public static final DeferredHolder<Ability, FingerMines> FINGER_MINES = ability("bomb_finger_mines", FingerMines::new);
    public static final DeferredHolder<Ability, BombTackle> BOMB_TACKLE = ability("bomb_tackle", BombTackle::new);
    public static final DeferredHolder<Ability, FullBody> FULL_BODY = ability("bomb_full_body", FullBody::new);
    public static final DeferredHolder<Ability, Barrage> BARRAGE = ability("bomb_barrage", Barrage::new);

    public static final ResourceLocation SHELL = GrandLine.id("bomb_shell");
    public static final ResourceLocation BREATH = GrandLine.id("bomb_breath");
    public static final ResourceLocation MINE = GrandLine.id("bomb_mine");

    public BombFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Bomu Bomu no Mi", COLOR)
                .abilities(NOSE_CANNON, BOMB_KICK, BREATH_BOMB, FINGER_MINES, BOMB_TACKLE)
                .awakened(FULL_BODY, BARRAGE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SHELL, Projectiles.blast(2.8, AbilityDamage.BLAST, null, p -> {
            if (p.level() instanceof ServerLevel level) {
                boom(level, p, p.position(), 2.8f);
            }
        }));
        // A breath of explosive air hangs for a second, then goes off.
        ProjectileBehavior.Registry.register(BREATH, Projectiles.zone(Integer.MAX_VALUE, (p, target) -> {}, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount == 20) {
                explode(owner, p.position(), p.size() / 2.0, p.damage());
                p.discard();
            }
        }));
        // A mine waits on the ground and goes off under the first foe to step on it.
        ProjectileBehavior.Registry.register(MINE, Projectiles.zone(2, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && !p.isRemoved()) {
                explode(owner, p.position(), 3.0, p.damage());
                p.discard();
            }
        }, null));
    }

    /** The look and sound of an explosion that harms no blocks. */
    static void boom(ServerLevel level, net.minecraft.world.entity.Entity source, Vec3 at, float radius) {
        Vfx.burst(level, source, at, FIRE, radius, Kind.FLAME, Vfx.SHOCKWAVE | Vfx.FLASH | (radius > 4 ? Vfx.HEAVY : 0));
        Vfx.burst(level, source, at, COLOR, radius * 0.8f, Kind.SMOKE, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                Math.min(3f, 0.8f + radius * 0.3f), 1.1f - Math.min(0.5f, radius * 0.06f));
    }

    /** Blasts everyone within {@code radius} of {@code at} (never the user, never blocks). */
    static void explode(ServerPlayer owner, Vec3 at, double radius, float damage) {
        for (LivingEntity target : AbilityDamage.around(owner, at, radius)) {
            Kit.hit(owner, target, AbilityDamage.BLAST, damage);
            AbilityDamage.knockback(target, at, 1.0 + radius * 0.1, 0.4);
        }
        boom(owner.serverLevel(), owner, at, (float) radius);
    }

    /** Explosions can't hurt a body that is itself a bomb. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
        }
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        long until = user.getPersistentData().getLong(TACKLE);
        if (until == 0) {
            return;
        }
        boolean contact = !AbilityDamage.around(user, user.position().add(0, 1, 0), 1.8).isEmpty();
        if (contact || user.level().getGameTime() >= until) {
            user.getPersistentData().remove(TACKLE);
            explode(user, user.position().add(0, 1, 0), 3.5, user.getPersistentData().getFloat(TACKLE + "_damage"));
        }
    }

    @Override
    public boolean ticks() {
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Flick an explosive bogey at a foe. It goes off on contact.
     *
     * @name Nose Fancy Cannon
     */
    public static final class NoseCannon extends Ability {
        NoseCannon() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, FIRE).cooldown(40).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SHELL, 1.5, 0.35f, 0x8AA040, 7f, 30).withGravity(0.02);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.SNOWBALL_THROW, 1f, 0.8f);
        }
    }

    /** Kick with an exploding leg, blasting whatever stands in front of you. */
    public static final class BombKick extends Ability {
        BombKick() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, FIRE).cooldown(60).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 10f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.6, 0.5);
            }
            boom(ctx.level(), player, player.position().add(ctx.horizontalLook().scale(1.6)).add(0, 0.6, 0), 2f);
            ctx.animate(GLEffects.ANIM_SWEEP);
        }
    }

    /** Breathe out a cloud of explosive air where you point; a second later it detonates. */
    public static final class BreathBomb extends Ability {
        BreathBomb() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(120).stamina(14).mastery(M, 10)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.aimPoint(player, 14);
            AbilityProjectile.zone(player, BREATH, at, 3.5 * ctx.power(), 0xC8C0B0, 12f * ctx.power(), 25);
            Vfx.trail(ctx.level(), player, player.getEyePosition(), at, 0xC8C0B0, 0.6f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.GAS_HISS.get(), 1f, 1.2f);
        }
    }

    /** Plant three finger mines in a spread where you point; each goes off under the first foe that steps on it. */
    public static final class FingerMines extends Ability {
        FingerMines() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, FIRE).cooldown(200).stamina(16).mastery(M, 20)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 16), 6);
            for (Vec3 spot : Kit.circle(at, 2.2, 3, player.getYRot() * Math.PI / 180)) {
                Vec3 ground = Kit.ground(ctx.level(), spot.add(0, 1, 0), 4);
                AbilityProjectile.zone(player, MINE, ground, 1.2, FIRE, 9f * ctx.power(), 900);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.TRIPWIRE_ATTACH, 1f, 0.8f);
        }
    }

    /** Charge headlong; your body explodes on the first foe you touch (or when the charge runs out). */
    public static final class BombTackle extends Ability {
        BombTackle() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, FIRE).cooldown(160).stamina(18).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.getPersistentData().putLong(TACKLE, ctx.level().getGameTime() + 12);
            player.getPersistentData().putFloat(TACKLE + "_damage", 12f * ctx.power());
            Kit.dash(player, 1.7, 0.15);
            Vfx.trail(ctx.level(), player, player.position(), player.position().add(player.getLookAngle().scale(6)), FIRE, 0.5f,
                    Kind.FLAME);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.FIRE_FIST.get(), 0.8f, 1.4f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Turn your whole body into a bomb and detonate, blasting everything around you away. You walk out unhurt. */
    public static final class FullBody extends Ability {
        FullBody() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, FIRE).cooldown(600).stamina(36).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            explode(player, player.position().add(0, 1, 0), 7 * ctx.power(), 18f * ctx.power());
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
        }
    }

    /** Hold to fire a rapid stream of exploding shots. */
    public static final class Barrage extends Ability {
        Barrage() {
            super(props(ActivationType.HOLD, AbilityIcon.BEAM, FIRE).cooldown(400).stamina(20).staminaPerTick(0.4f).maxActive(40)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                ServerPlayer player = ctx.player();
                Vec3 spread = new Vec3(player.getRandom().nextGaussian(), player.getRandom().nextGaussian(),
                        player.getRandom().nextGaussian()).scale(0.05);
                AbilityProjectile.spawn(player, SHELL, ctx.handPos(), player.getLookAngle().add(spread).normalize().scale(1.7),
                        0.4f, FIRE, 6f * ctx.power(), 30).withPower(ctx.power());
                ctx.sound(SoundEvents.FIRECHARGE_USE, 0.6f, 1.4f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }
}
