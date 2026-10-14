package com.nerodev.grandline.style;

import static com.nerodev.grandline.style.GLStyles.ability;
import static com.nerodev.grandline.style.GLStyles.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.combat.CombatManager;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import com.nerodev.grandline.weapon.WeaponItem;
import com.nerodev.grandline.weapon.WeaponKind;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The way of the sword. One sword in the hand, a second in the other hand, a third anywhere on the hotbar (held in
 * the mouth, Santoryu style): every technique grows with the number of swords. Sword blows hit harder with each
 * blade, and combos end in a drawing cut that leaves the foe bleeding.
 */
public class SwordsmanStyle extends FightingStyle {
    public static final int COLOR = 0x4A9A5A;
    public static final int STEEL = 0xE8F0F8;
    static final String M = "style/grandline:swordsman";

    public static final DeferredHolder<Ability, ShishiSonson> SHISHI_SONSON = ability("swordsman_shishi_sonson", ShishiSonson::new);
    public static final DeferredHolder<Ability, PoundPhoenix> POUND_PHOENIX = ability("swordsman_pound_phoenix", PoundPhoenix::new);
    public static final DeferredHolder<Ability, Rashomon> RASHOMON = ability("swordsman_rashomon", Rashomon::new);
    public static final DeferredHolder<Ability, OniGiri> ONI_GIRI = ability("swordsman_oni_giri", OniGiri::new);
    public static final DeferredHolder<Ability, Tatsumaki> TATSUMAKI = ability("swordsman_tatsumaki", Tatsumaki::new);

    /** The flying slash of the Pound Phoenix. */
    public static final ResourceLocation FLYING_SLASH = GrandLine.id("swordsman_flying_slash");

    public SwordsmanStyle() {
        super(COLOR, SwordsmanStyle::iai, SHISHI_SONSON, POUND_PHOENIX, RASHOMON, ONI_GIRI, TATSUMAKI);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FLYING_SLASH, Projectiles.piercing(AbilityDamage.SLASH,
                (slash, target) -> target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 40, 0))));
    }

    static boolean isSword(ItemStack stack) {
        return stack.getItem() instanceof WeaponItem weapon && weapon.kind() == WeaponKind.SWORD;
    }

    /**
     * How many swords {@code player} fights with: 0 without one in the main hand, otherwise one more for a sword in
     * the off hand and one more for another sword on the hotbar.
     */
    public static int swords(ServerPlayer player) {
        if (!isSword(player.getMainHandItem())) {
            return 0;
        }
        int count = 1;
        if (isSword(player.getOffhandItem())) {
            count++;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (i != inventory.selected && isSword(inventory.getItem(i))) {
                count++;
                break;
            }
        }
        return count;
    }

    static Optional<Component> needSword(ServerPlayer player, int count) {
        int swords = swords(player);
        if (swords == 0) {
            return Optional.of(Component.literal("You need a sword in hand"));
        }
        if (swords < count) {
            return Optional.of(Component.literal("This technique needs " + count + " swords"));
        }
        return Optional.empty();
    }

    /** A cut that bleeds. */
    static void cut(ServerPlayer user, LivingEntity target, float damage, int bleedTicks) {
        Kit.hit(user, target, AbilityDamage.SLASH, damage);
        if (bleedTicks > 0) {
            target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, bleedTicks, 0));
        }
    }

    static Vec3 right(Vec3 look) {
        Vec3 flat = new Vec3(-look.z, 0, look.x);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
    }

    /** Combo finisher: a drawing cut that leaves a bleeding wound. Without a sword in hand, the default uppercut. */
    static void iai(ServerPlayer attacker, LivingEntity target, int combo) {
        int swords = swords(attacker);
        if (swords == 0) {
            CombatManager.UPPERCUT.finish(attacker, target, combo);
            return;
        }
        cut(attacker, target, 3f + 2f * swords, 60);
        AbilityDamage.knockback(target, attacker.position(), 0.6, 0.15);
        Vfx.slash(attacker.serverLevel(), attacker, target.getBoundingBox().getCenter(), right(attacker.getLookAngle()), STEEL, 2.2f,
                swords);
        Fx.animate(attacker, GLEffects.ANIM_SWORD_IAI);
    }

    /** Every blade behind a sword blow adds to it. */
    @Override
    public void onArmedHit(ServerPlayer user, LivingEntity target, ItemStack weapon, LivingIncomingDamageEvent event) {
        if (isSword(weapon)) {
            event.setAmount(event.getAmount() + swords(user));
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Draw and cut in one motion, dashing straight through everything ahead. Each sword adds to the cut.
     *
     * @name Shishi Sonson
     */
    public static final class ShishiSonson extends Ability {
        ShishiSonson() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, COLOR).cooldown(80).stamina(12).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needSword(player, 1);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int swords = swords(player);
            double range = 7 * ctx.power();
            List<LivingEntity> targets = AbilityDamage.line(player, range, 1.4);
            Kit.blink(player, range, STEEL, Kind.STREAK);
            for (LivingEntity target : targets) {
                cut(player, target, (10f + 3f * swords) * ctx.power(), 40);
                Vfx.slash(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.right(), STEEL, 2f, 1);
            }
            ctx.animate(GLEffects.ANIM_SWORD_IAI);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.3f);
            ctx.sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 1.6f);
        }
    }

    /**
     * Swing so hard the cut flies: a blade of air for every sword you hold, slicing through all in its path.
     *
     * @name 36 Pound Phoenix
     */
    public static final class PoundPhoenix extends Ability {
        PoundPhoenix() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(60).stamina(12).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needSword(player, 1);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int swords = swords(player);
            Vec3 look = player.getLookAngle();
            for (int i = 0; i < swords; i++) {
                float yaw = (i - (swords - 1) / 2f) * 0.09f;
                AbilityProjectile.spawn(player, FLYING_SLASH, ctx.handPos(), look.yRot(yaw).scale(2.2), 1.3f, STEEL, 9f * ctx.power(), 25)
                        .withPower(ctx.power());
            }
            ctx.animate(GLEffects.ANIM_SWORD_SLASH);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.5f);
        }
    }

    /** One colossal downward cut that splits armour and throws back everything in front of you. */
    public static final class Rashomon extends Ability {
        Rashomon() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, STEEL).cooldown(140).stamina(16).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needSword(player, 1);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int swords = swords(player);
            for (LivingEntity target : AbilityDamage.cone(player, 6 * ctx.power(), 30)) {
                cut(player, target, (16f + 2f * swords) * ctx.power() + target.getArmorValue() * 0.5f, 60);
                AbilityDamage.knockback(target, player.position(), 1.2, 0.3);
            }
            Vec3 at = player.getEyePosition().add(ctx.horizontalLook().scale(3));
            Vfx.slash(ctx.level(), player, at, new Vec3(0, 1, 0), STEEL, 5f, 1);
            Vfx.burst(ctx.level(), player, player.position().add(ctx.horizontalLook().scale(4)), 0x8A7A5A, 2f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_SWORD_OVERHEAD);
            ctx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.6f);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 0.8f, 1.4f);
        }
    }

    /**
     * Charge through the foe in front of you with crossed blades, cutting it once for every sword. Needs at least two.
     *
     * @name Oni Giri
     */
    public static final class OniGiri extends Ability {
        OniGiri() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORDS, COLOR).cooldown(160).stamina(18).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            Optional<Component> swords = needSword(player, 2);
            if (swords.isPresent()) {
                return swords;
            }
            return Kit.target(player, 8).isPresent() ? Optional.empty() : Optional.of(Component.literal("No one within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int swords = swords(player);
            Kit.target(player, 8).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 to = target.position().add(ctx.horizontalLook().scale(1.8));
                for (int i = 0; i < swords; i++) {
                    cut(player, target, (7f + swords) * ctx.power(), 80);
                }
                Vec3 center = target.getBoundingBox().getCenter();
                Vec3 side = ctx.right();
                Vfx.slash(ctx.level(), player, center, side.add(0, 1, 0).normalize(), STEEL, 2.6f, 1);
                Vfx.slash(ctx.level(), player, center, side.add(0, -1, 0).normalize(), STEEL, 2.6f, 1);
                if (swords >= 3) {
                    Vfx.slash(ctx.level(), player, center, side, COLOR, 2.6f, 1);
                }
                player.teleportTo(to.x, target.getY(), to.z);
                player.resetFallDistance();
                Vfx.trail(ctx.level(), player, from.add(0, 1, 0), to.add(0, 1, 0), STEEL, 0.8f, Kind.STREAK);
            });
            ctx.animate(GLEffects.ANIM_SWORD_IAI);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, 0.9f);
            ctx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.2f);
        }
    }

    /** Spin with every blade out and raise a cutting whirlwind that lifts everything around you into the air. */
    public static final class Tatsumaki extends Ability {
        Tatsumaki() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, COLOR).cooldown(240).stamina(20).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needSword(player, 1);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int swords = swords(player);
            double radius = 5 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                cut(player, target, (12f + 2f * swords) * ctx.power(), 40);
                Vec3 out = target.position().subtract(player.position()).multiply(1, 0, 1);
                Vec3 push = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale(0.3);
                Kit.launch(target, push.add(0, 1.1, 0));
            }
            Vfx.spiral(ctx.level(), player, player.position(), STEEL, (float) radius, Kind.STREAK);
            Vfx.spiral(ctx.level(), player, player.position().add(0, 1.5, 0), COLOR, (float) radius * 0.7f, Kind.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_SWORD_SPIN);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.7f);
            ctx.sound(GLSounds.SNOW_GUST.get(), 1f, 0.8f);
        }
    }
}
