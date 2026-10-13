package com.nerodev.grandline.fruit.flame;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Flame-Flame Fruit's moves. Base kit is about projectiles and zoning; the awakened kit manipulates the
 * area itself (lingering fireflies you detonate at will, a burning field around you).
 */
public final class FlameMoves {
    private static final String M = FlameAbilities.MASTERY;
    private static final int C = FlameAbilities.COLOR;

    private FlameMoves() {}

    private static Ability.Properties props(ActivationType type, AbilityIcon icon) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, C);
    }

    static void sound(ServerLevel level, Vec3 pos, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Fire damage + ignition, scaled per target. */
    static void burn(ServerPlayer owner, Entity target, float damage, float seconds, Entity direct) {
        if (AbilityDamage.hurt(owner, target, AbilityDamage.FIRE, damage, direct)) {
            target.igniteForSeconds(seconds);
        }
    }

    /** Fire explosion: damage falls off with distance, knocks back, burns. */
    static void fireBlast(ServerPlayer owner, Vec3 center, double radius, float damage, float seconds, double knockback,
            Entity direct, float fxScale) {
        for (LivingEntity target : AbilityDamage.around(owner, center, radius)) {
            double falloff = 1.0 - 0.5 * Math.min(1.0, target.position().distanceTo(center) / radius);
            burn(owner, target, (float) (damage * falloff), seconds, direct);
            AbilityDamage.knockback(target, center, knockback * falloff, 0.35);
        }
        ServerLevel level = owner.serverLevel();
        Fx.play(level, GLEffects.FIRE_EXPLOSION, owner, FxParams.at(center).color(C).scale(fxScale));
        sound(level, center, GLSounds.FIRE_EXPLOSION.get(), 1.2f + fxScale * 0.2f, 1.1f - fxScale * 0.1f);
        if (GLServerConfig.abilityGriefing()) {
            igniteGround(level, BlockPos.containing(center), (int) Math.ceil(radius * 0.6));
        }
    }

    static void igniteGround(ServerLevel level, BlockPos center, int radius) {
        for (int i = 0; i < radius * 3; i++) {
            BlockPos pos = center.offset(level.random.nextInt(radius * 2 + 1) - radius, 0, level.random.nextInt(radius * 2 + 1) - radius);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos at = pos.above(dy);
                if (level.isEmptyBlock(at) && BaseFireBlock.canBePlacedAt(level, at, net.minecraft.core.Direction.UP)) {
                    level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
                    break;
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Hiken: Fire Fist
    // ---------------------------------------------------------------------------------------------------------

    /** Charge a fist of fire and throw it as a huge flaming projectile. Longer charge = bigger and stronger. */
    public static final class FireFist extends Ability {
        static final int FULL_CHARGE = 30;

        FireFist() {
            super(props(ActivationType.CHARGE, AbilityIcon.FIST).cooldown(70).stamina(22).maxActive(40).minCharge(6)
                    .masteryGain(1.2));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 0.8f, 1.0f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 3 == 0) {
                ctx.fx(GLEffects.FLAME_CHARGE, FxParams.at(ctx.handPos()).color(C)
                        .scale(0.5f + Math.min(1f, ctx.activeTicks() / (float) FULL_CHARGE)));
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            float charge = Math.min(1f, ctx.activeTicks() / (float) FULL_CHARGE);
            float power = ctx.power() * (0.6f + 0.4f * charge);
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            AbilityProjectile.spawn(player, FlameAbilities.FIRE_FIST_SHOT, ctx.handPos(), look.scale(1.5),
                    0.9f + 0.9f * charge, C, 9f * power, 30).withPower(power);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.fx(GLEffects.FLAME_BURST, FxParams.of(ctx.handPos(), look).color(C).scale(1f + charge));
            ctx.sound(GLSounds.FIRE_FIST.get(), 1.2f, 1.0f - 0.2f * charge);
        }
    }

    public static final class FireFistShot implements ProjectileBehavior {
        @Override
        public boolean onHitEntity(AbilityProjectile p, Entity target) {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                burn(owner, target, p.damage(), 5, p);
                AbilityDamage.knockback(target, p.position().subtract(p.getDeltaMovement()), 1.2 * p.power(), 0.4);
                fireBlast(owner, p.position(), 1.5 + p.size(), p.damage() * 0.4f, 3, 0.6, p, p.size());
            }
            return false;
        }

        @Override
        public void onHitBlock(AbilityProjectile p, BlockHitResult hit) {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                fireBlast(owner, hit.getLocation(), 1.5 + p.size(), p.damage() * 0.5f, 3, 0.7, p, p.size());
            }
        }

        @Override
        public void onExpire(AbilityProjectile p) {
            Fx.play((ServerLevel) p.level(), GLEffects.FLAME_BURST, p, FxParams.at(p.position()).color(C).scale(0.6f));
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Higan: Fire Gun
    // ---------------------------------------------------------------------------------------------------------

    /** Finger-gun volley: five fast fire bullets in a tight spread. */
    public static final class FireGun extends Ability {
        FireGun() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM).cooldown(35).stamina(14).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 hand = ctx.handPos();
            for (int i = -2; i <= 2; i++) {
                float yaw = player.getYRot() + i * 3.5f + (player.getRandom().nextFloat() - 0.5f) * 2f;
                float pitch = player.getXRot() + (player.getRandom().nextFloat() - 0.5f) * 3f;
                Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
                AbilityProjectile.spawn(player, FlameAbilities.FIRE_BULLET, hand.add(dir.scale(0.3)), dir.scale(2.3),
                        0.3f, C, 3.2f * ctx.power(), 18);
            }
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.fx(GLEffects.FLAME_BURST, FxParams.of(hand, player.getLookAngle()).color(C).scale(0.5f));
            ctx.sound(GLSounds.FIRE_GUN.get(), 1.0f, 1.1f);
        }
    }

    public static final class FireBulletShot implements ProjectileBehavior {
        @Override
        public boolean onHitEntity(AbilityProjectile p, Entity target) {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                target.invulnerableTime = 0;
                burn(owner, target, p.damage(), 2, p);
                Fx.play((ServerLevel) p.level(), GLEffects.FLAME_BURST, p, FxParams.at(p.position()).color(C).scale(0.35f));
            }
            return false;
        }

        @Override
        public void onHitBlock(AbilityProjectile p, BlockHitResult hit) {
            Fx.play((ServerLevel) p.level(), GLEffects.FLAME_BURST, p, FxParams.at(hit.getLocation()).color(C).scale(0.3f));
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Kyokaen: Flame Commandment (wall)
    // ---------------------------------------------------------------------------------------------------------

    /** Raise a wall of fire in front of you: burns and repels anyone who crosses, burns arrows out of the air. */
    public static final class FlameCommandment extends Ability {
        static final float WIDTH = 6f;

        FlameCommandment() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD).cooldown(160).stamina(28).mastery(M, 15).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 center = player.position().add(ctx.horizontalLook().scale(3.0));
            AbilityProjectile wall = AbilityProjectile.spawn(player, FlameAbilities.FLAME_WALL, center, Vec3.ZERO, WIDTH, C,
                    3f * ctx.power(), 100);
            wall.setYRot(player.getYRot());
            wall.yRotO = player.getYRot();
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.FLAME_WALL.get(), 1.2f, 0.9f);
        }
    }

    public static final class WallBehavior implements ProjectileBehavior {
        @Override
        public void tick(AbilityProjectile p) {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null || p.tickCount % 5 != 0) {
                return;
            }
            float yaw = p.getYRot() * Mth.DEG_TO_RAD;
            Vec3 along = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
            Vec3 normal = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            double half = p.size() / 2;
            AABB box = new AABB(p.position(), p.position()).inflate(half, 0, half).expandTowards(0, 3.2, 0);
            for (Entity e : p.level().getEntities(p, box)) {
                Vec3 rel = e.position().subtract(p.position());
                if (Math.abs(rel.dot(along)) > half || Math.abs(rel.dot(normal)) > 1.2) {
                    continue;
                }
                if (e instanceof Projectile projectile && projectile.getOwner() != owner) {
                    Fx.play((ServerLevel) p.level(), GLEffects.FLAME_BURST, p, FxParams.at(e.position()).color(C).scale(0.4f));
                    e.discard();
                } else if (e instanceof LivingEntity living && AbilityDamage.canHarm(owner, living) && p.tickCount % 10 == 0) {
                    living.invulnerableTime = 0;
                    burn(owner, living, p.damage(), 4, p);
                    Vec3 away = rel.dot(normal) >= 0 ? normal : normal.reverse();
                    living.setDeltaMovement(living.getDeltaMovement().add(away.scale(0.6)).add(0, 0.2, 0));
                    living.hurtMarked = true;
                }
            }
        }

        @Override
        public boolean onHitEntity(AbilityProjectile p, Entity target) {
            return true;
        }

        @Override
        public boolean hitsBlocks() {
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Hibashira: Fire Pillar
    // ---------------------------------------------------------------------------------------------------------

    /** Mark a spot; a moment later a column of fire erupts there, launching and burning everyone on it. */
    public static final class FirePillar extends Ability {
        static final double RANGE = 26;
        static final int DELAY = 12;

        FirePillar() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME).cooldown(100).stamina(26).mastery(M, 30).masteryGain(1.2));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 target = AbilityDamage.lookTarget(player, RANGE);
            Vec3 ground = findGround(player.serverLevel(), target);
            AbilityProjectile.spawn(player, FlameAbilities.PILLAR, ground, Vec3.ZERO, 2.4f, C, 10f * ctx.power(), DELAY + 22);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 0.9f, 1.3f);
        }

        static Vec3 findGround(ServerLevel level, Vec3 from) {
            BlockPos pos = BlockPos.containing(from);
            for (int i = 0; i < 8; i++) {
                if (!level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                    return new Vec3(from.x, pos.getY(), from.z);
                }
                pos = pos.below();
            }
            return from;
        }
    }

    public static final class PillarBehavior implements ProjectileBehavior {
        @Override
        public void tick(AbilityProjectile p) {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null || p.tickCount != FirePillar.DELAY) {
                return;
            }
            ServerLevel level = (ServerLevel) p.level();
            Vec3 base = p.position();
            AABB column = new AABB(base, base).inflate(p.size(), 0, p.size()).expandTowards(0, 7, 0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, column, e -> AbilityDamage.canHarm(owner, e))) {
                if (target.position().subtract(base).horizontalDistance() <= p.size() + target.getBbWidth() / 2) {
                    burn(owner, target, p.damage(), 4, p);
                    target.setDeltaMovement(target.getDeltaMovement().multiply(0.3, 0, 0.3).add(0, 1.15, 0));
                    target.hurtMarked = true;
                }
            }
            Fx.play(level, GLEffects.FIRE_PILLAR, p, FxParams.at(base).color(C).scale(p.size()));
            sound(level, base, GLSounds.FIRE_PILLAR.get(), 1.4f, 0.9f);
            if (GLServerConfig.abilityGriefing()) {
                igniteGround(level, BlockPos.containing(base), 2);
            }
        }

        @Override
        public boolean onHitEntity(AbilityProjectile p, Entity target) {
            return true;
        }

        @Override
        public boolean hitsBlocks() {
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Dai Enkai: Entei (signature)
    // ---------------------------------------------------------------------------------------------------------

    /** Gather a small sun above your head, then hurl it. Slow and costly; devastating where it lands. */
    public static final class FlameEmperor extends Ability {
        static final int FULL_CHARGE = 60;

        FlameEmperor() {
            super(props(ActivationType.CHARGE, AbilityIcon.ORB).cooldown(400).stamina(45).staminaPerTick(0.25f)
                    .maxActive(80).minCharge(25).mastery(M, 60).masteryGain(3.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.ENTEI_CHARGE.get(), 1.4f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 4 == 0) {
                float charge = Math.min(1f, ctx.activeTicks() / (float) FULL_CHARGE);
                ctx.fx(GLEffects.ENTEI_CHARGE, FxParams.at(ctx.aboveHead(2.8)).color(C).scale(1f + 3f * charge));
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 2, true, false, false));
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            float charge = Math.min(1f, ctx.activeTicks() / (float) FULL_CHARGE);
            ServerPlayer player = ctx.player();
            float power = ctx.power() * (0.7f + 0.3f * charge);
            AbilityProjectile.spawn(player, FlameAbilities.ENTEI, ctx.aboveHead(2.8), player.getLookAngle().scale(0.9),
                    2.4f + 2.2f * charge, C, 17f * power, 60).withPower(power);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.FIRE_FIST.get(), 1.6f, 0.6f);
        }
    }

    public static final class EnteiShot implements ProjectileBehavior {
        @Override
        public boolean onHitEntity(AbilityProjectile p, Entity target) {
            explode(p);
            return false;
        }

        @Override
        public void onHitBlock(AbilityProjectile p, BlockHitResult hit) {
            explode(p);
        }

        @Override
        public void onExpire(AbilityProjectile p) {
            explode(p);
        }

        private static void explode(AbilityProjectile p) {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                fireBlast(owner, p.position(), 3.5 + p.size(), p.damage(), 6, 1.8, p, 2.5f + p.size() * 0.5f);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened: Hotarubi (fireflies) + Hidaruma (detonate)
    // ---------------------------------------------------------------------------------------------------------

    /** Scatter glowing fireflies that drift to nearby enemies and cling to them, waiting for your signal. */
    public static final class Fireflies extends Ability {
        static final int COUNT = 12;

        Fireflies() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR).cooldown(200).stamina(30).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = 0; i < COUNT; i++) {
                double a = Mth.TWO_PI * i / COUNT;
                Vec3 out = new Vec3(Math.cos(a), 0.3 + player.getRandom().nextDouble() * 0.4, Math.sin(a));
                AbilityProjectile.spawn(player, FlameAbilities.FIREFLY, player.position().add(0, 1.2, 0).add(out),
                        out.scale(0.18), 0.25f, 0x9CFF6A, 6f * ctx.power(), 20 * 12);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.FIREFLY.get(), 1.0f, 1.2f);
        }
    }

    public static final class FireflyBehavior implements ProjectileBehavior {
        @Override
        public void tick(AbilityProjectile p) {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null) {
                return;
            }
            Vec3 motion = p.getDeltaMovement().scale(0.9);
            if (p.tickCount % 5 == 0) {
                List<LivingEntity> near = AbilityDamage.around(owner, p.position(), 7);
                if (!near.isEmpty()) {
                    Vec3 to = near.getFirst().getBoundingBox().getCenter().subtract(p.position());
                    motion = motion.add(to.normalize().scale(Math.min(0.12, to.length() * 0.05)));
                }
            }
            // gentle bobbing so idle fireflies look alive
            motion = motion.add(0, Mth.sin(p.tickCount * 0.25f + p.getId()) * 0.004, 0);
            p.setDeltaMovement(motion);
        }

        @Override
        public boolean onHitEntity(AbilityProjectile p, Entity target) {
            // Cling: match the target's movement instead of damaging.
            p.setDeltaMovement(target.getDeltaMovement());
            return true;
        }

        @Override
        public boolean hitsBlocks() {
            return false;
        }
    }

    /** Ignite every firefly you have placed. Each one bursts into a small fire explosion. */
    public static final class Detonate extends Ability {
        Detonate() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST).cooldown(40).stamina(10).masteryGain(0.8));
        }

        static List<AbilityProjectile> fireflies(ServerPlayer player) {
            return player.serverLevel().getEntitiesOfClass(AbilityProjectile.class, player.getBoundingBox().inflate(48),
                    p -> p.getOwner() == player && FlameAbilities.FIREFLY.equals(p.kind()));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return fireflies(player).isEmpty() ? Optional.of(Component.literal("No fireflies to ignite")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (AbilityProjectile firefly : fireflies(ctx.player())) {
                fireBlast(ctx.player(), firefly.position(), 2.2, firefly.damage(), 3, 0.5, firefly, 0.8f);
                firefly.discard();
            }
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened: flame field
    // ---------------------------------------------------------------------------------------------------------

    /** Turn the ground around you into a sea of flames that burns and slows enemies while it lasts. */
    public static final class FlameField extends Ability {
        static final double RADIUS = 6;

        FlameField() {
            super(props(ActivationType.TOGGLE, AbilityIcon.WAVE).cooldown(300).stamina(25).staminaPerTick(0.45f)
                    .maxActive(20 * 20).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FLAME_WALL.get(), 1.4f, 0.7f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 != 0) {
                return true;
            }
            ServerPlayer player = ctx.player();
            ctx.fx(GLEffects.FLAME_FIELD, FxParams.at(player.position()).color(C).scale((float) RADIUS));
            for (LivingEntity target : AbilityDamage.around(player, player.position(), RADIUS)) {
                target.invulnerableTime = 0;
                burn(player, target, 2.5f * ctx.power(), 3, player);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
            if (GLServerConfig.abilityGriefing() && ctx.activeTicks() % 40 == 0) {
                igniteGround(player.serverLevel(), player.blockPosition(), (int) RADIUS - 1);
            }
            return true;
        }
    }
}
