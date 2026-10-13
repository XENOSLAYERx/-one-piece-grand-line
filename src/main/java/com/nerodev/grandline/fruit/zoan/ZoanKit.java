package com.nerodev.grandline.fruit.zoan;

import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Small building blocks shared by Zoan moves: bites that bleed, gores that toss, charges and stomps. */
public final class ZoanKit {
    private ZoanKit() {}

    /** Claws or fangs: cutting damage, and the wound keeps bleeding for {@code seconds}. */
    public static void maul(ServerPlayer player, LivingEntity target, float damage, int seconds, int level) {
        if (Kit.hit(player, target, AbilityDamage.SLASH, damage) && seconds > 0) {
            target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, seconds * 20, level));
        }
    }

    /** Horns, tusks or a skull: a heavy blow that throws the target along {@code dir} and up. */
    public static void gore(ServerPlayer player, LivingEntity target, float damage, Vec3 dir, double strength, double lift) {
        Kit.hit(player, target, AbilityDamage.STRIKE, damage);
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        Vec3 push = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(strength);
        Kit.launch(target, push.add(0, lift, 0));
    }

    /** Sweeps a straight charge of {@code length} in front of the user: everything in the way is gored aside. */
    public static List<LivingEntity> charge(ServerPlayer player, double length, double width, float damage, double toss) {
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        List<LivingEntity> hit = AbilityDamage.line(player, length, width);
        for (LivingEntity target : hit) {
            double side = target.position().subtract(player.position()).dot(right) >= 0 ? 1 : -1;
            gore(player, target, damage, look.add(right.scale(side * 0.8)), toss, 0.45);
        }
        Kit.dash(player, Math.min(2.6, length / 5), 0.1);
        return hit;
    }

    /** A crushing stomp around {@code at}: damage, a bounce into the air and a short stagger. */
    public static List<LivingEntity> stomp(ServerPlayer player, Vec3 at, double radius, float damage) {
        List<LivingEntity> hit = AbilityDamage.around(player, at, radius);
        for (LivingEntity target : hit) {
            Kit.hit(player, target, AbilityDamage.CRUSH, damage);
            Kit.launch(target, new Vec3(0, 0.6, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        return hit;
    }

    /** The user, allied players and the user's minions within {@code radius}. */
    public static List<LivingEntity> friends(ServerPlayer player, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        out.add(player);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            if ((e instanceof Player && !AbilityDamage.canHarm(player, e)) || Minions.isAlly(player, e)) {
                out.add(e);
            }
        }
        return out;
    }

    /** A roar that cows everything nearby: pushed back, weakened and slowed. */
    public static List<LivingEntity> roar(ServerPlayer player, double radius, float damage, int ticks) {
        List<LivingEntity> hit = AbilityDamage.around(player, player.position(), radius);
        for (LivingEntity target : hit) {
            Kit.hit(player, target, AbilityDamage.STRIKE, damage);
            AbilityDamage.knockback(target, player.position(), 1.4, 0.35);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks * 2 / 3, 1));
        }
        return hit;
    }
}
