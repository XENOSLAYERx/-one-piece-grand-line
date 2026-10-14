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
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.weapon.AmmoItem;
import com.nerodev.grandline.weapon.GLWeapons;
import com.nerodev.grandline.weapon.GunItem;
import com.nerodev.grandline.weapon.SeastoneCuffsItem;
import com.nerodev.grandline.weapon.WeaponManager;
import java.util.Comparator;
import java.util.List;
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
 * The gunfighter's way. Guns reload faster and every bullet hits harder in a Gunslinger's hands, and the techniques
 * spend real ammunition: Seastone bullets carry their seal into every trick shot. Melee combos end in a point-blank
 * shot.
 */
public class GunslingerStyle extends FightingStyle {
    public static final int COLOR = 0xC8783A;
    public static final int FLASH = 0xF0C060;
    static final String M = "style/grandline:gunslinger";

    public static final DeferredHolder<Ability, QuickDraw> QUICK_DRAW = ability("gunslinger_quick_draw", QuickDraw::new);
    public static final DeferredHolder<Ability, FanTheHammer> FAN_THE_HAMMER = ability("gunslinger_fan_the_hammer", FanTheHammer::new);
    public static final DeferredHolder<Ability, PiercingRound> PIERCING_ROUND = ability("gunslinger_piercing_round", PiercingRound::new);
    public static final DeferredHolder<Ability, SmokeShell> SMOKE_SHELL = ability("gunslinger_smoke_shell", SmokeShell::new);
    public static final DeferredHolder<Ability, Deadeye> DEADEYE = ability("gunslinger_deadeye", Deadeye::new);

    public static final ResourceLocation PIERCING_SHOT = GrandLine.id("gunslinger_piercing_shot");
    public static final ResourceLocation SMOKE_SHOT = GrandLine.id("gunslinger_smoke_shot");
    public static final ResourceLocation HOMING_SHOT = GrandLine.id("gunslinger_homing_shot");

    public GunslingerStyle() {
        super(COLOR, GunslingerStyle::pointBlank, QUICK_DRAW, FAN_THE_HAMMER, PIERCING_ROUND, SMOKE_SHELL, DEADEYE);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(PIERCING_SHOT, Projectiles.piercing(AbilityDamage.STRIKE, GunslingerStyle::seal));
        ProjectileBehavior.Registry.register(SMOKE_SHOT, Projectiles.blast(4, AbilityDamage.STRIKE, (shell, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            seal(shell, target);
        }, shell -> {
            ServerLevel level = (ServerLevel) shell.level();
            Vfx.burst(level, shell.getOwner(), shell.position(), 0xC8CCD0, 4f, Kind.SMOKE, Vfx.HEAVY);
            Vfx.burst(level, shell.getOwner(), shell.position(), 0x9A9EA4, 3f, Kind.SMOKE, Vfx.RING);
        }));
        ProjectileBehavior.Registry.register(HOMING_SHOT, Projectiles.homing(24, 0.35, GLWeapons.bullet(false)));
    }

    /** Seastone bullets seal whatever they hit, whichever trick shot carried them. */
    static void seal(AbilityProjectile shot, LivingEntity target) {
        if (shot.extra().getBoolean(GunItem.SEASTONE_TAG)) {
            SeastoneCuffsItem.cuff(target, 100);
        }
    }

    @Override
    public float reloadMultiplier() {
        return 0.6f;
    }

    @Override
    public float gunDamageMultiplier() {
        return 1.25f;
    }

    static GunItem gun(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof GunItem gun ? gun : null;
    }

    static Optional<Component> needGun(ServerPlayer player) {
        if (gun(player) == null) {
            return Optional.of(Component.literal("You need a gun in hand"));
        }
        return AmmoItem.count(player) > 0 ? Optional.empty() : Optional.of(Component.literal("Out of ammunition"));
    }

    /**
     * Fires one bullet of {@code kind} from the gun in hand, spending ammunition.
     *
     * @return the bullet, or null if the ammunition ran out
     */
    static AbilityProjectile shoot(ServerPlayer player, ResourceLocation kind, float damageScale, float spread) {
        GunItem gun = gun(player);
        Optional<Boolean> bullet = AmmoItem.consume(player);
        if (gun == null || bullet.isEmpty()) {
            return null;
        }
        boolean seastone = bullet.get();
        ResourceLocation shot = kind != null ? kind : seastone ? GLWeapons.SEASTONE_BULLET_SHOT : GLWeapons.BULLET_SHOT;
        return GunItem.fire(player, shot, WeaponManager.gunDamage(player, gun.damage()) * damageScale, gun.speed(), spread, seastone);
    }

    static void gunAnimation(ServerPlayer player) {
        GunItem gun = gun(player);
        Fx.animate(player, gun != null && gun.longGun() ? GLEffects.ANIM_RIFLE_SHOT : GLEffects.ANIM_PISTOL_SHOT);
    }

    /** Combo finisher: jam the gun into the foe and fire. Without a gun in hand, the default uppercut. */
    static void pointBlank(ServerPlayer attacker, LivingEntity target, int combo) {
        GunItem gun = gun(attacker);
        if (gun == null) {
            CombatManager.UPPERCUT.finish(attacker, target, combo);
            return;
        }
        float damage = WeaponManager.gunDamage(attacker, gun.damage()) * 0.6f;
        target.invulnerableTime = 0;
        AbilityDamage.hurt(attacker, target, AbilityDamage.STRIKE, damage, attacker);
        AbilityDamage.knockback(target, attacker.position(), 1.2, 0.25);
        Vec3 at = target.getBoundingBox().getCenter();
        Vfx.impact(attacker.serverLevel(), attacker, at, FLASH, 1.2f, Kind.SPARK);
        Vfx.impact(attacker.serverLevel(), attacker, at, 0xD0D0D0, 1f, Kind.SMOKE);
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                attacker.getSoundSource(), 0.4f, 1.8f);
        gunAnimation(attacker);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Draw and fire in a heartbeat: one dead-accurate shot that hits half again as hard. */
    public static final class QuickDraw extends Ability {
        QuickDraw() {
            super(props(ActivationType.INSTANT, AbilityIcon.GUN, COLOR).cooldown(40).stamina(6).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needGun(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            AbilityProjectile shot = shoot(ctx.player(), null, 1.5f * ctx.power(), 0f);
            if (shot != null) {
                shot.withPower(ctx.power());
            }
            gunAnimation(ctx.player());
        }
    }

    /** Fan the hammer and empty three shots in a heartbeat, spraying everything in front of you. */
    public static final class FanTheHammer extends Ability {
        FanTheHammer() {
            super(props(ActivationType.INSTANT, AbilityIcon.GUN, FLASH).cooldown(80).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needGun(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (int i = 0; i < 3; i++) {
                if (shoot(ctx.player(), null, 0.8f * ctx.power(), 0.08f) == null) {
                    break;
                }
            }
            ctx.animate(GLEffects.ANIM_GATLING);
        }
    }

    /** A heavy round that punches straight through every creature in its path. */
    public static final class PiercingRound extends Ability {
        PiercingRound() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(100).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needGun(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            AbilityProjectile shot = shoot(ctx.player(), PIERCING_SHOT, 1.3f * ctx.power(), 0f);
            if (shot != null) {
                shot.setSize(0.3f);
            }
            gunAnimation(ctx.player());
        }
    }

    /** Fire a smoke shell: where it bursts, everything is blinded and slowed in a thick grey cloud. */
    public static final class SmokeShell extends Ability {
        SmokeShell() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, 0xC8CCD0).cooldown(200).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needGun(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            AbilityProjectile shot = shoot(ctx.player(), SMOKE_SHOT, 0.5f, 0.01f);
            if (shot != null) {
                shot.setSize(0.35f);
                shot.withGravity(0.03);
            }
            gunAnimation(ctx.player());
        }
    }

    /**
     * Take aim at up to four foes around you and fire at each in turn; the shots curve after their marks.
     *
     * @name Deadeye
     */
    public static final class Deadeye extends Ability {
        Deadeye() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, FLASH).cooldown(300).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            Optional<Component> gun = needGun(player);
            if (gun.isPresent()) {
                return gun;
            }
            return AbilityDamage.around(player, player.position(), 24).isEmpty() ? Optional.of(Component.literal("No one to aim at"))
                    : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            List<LivingEntity> marks = AbilityDamage.around(player, player.position(), 24).stream()
                    .sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(4).toList();
            for (LivingEntity mark : marks) {
                Vec3 eye = player.getEyePosition();
                Vec3 dir = mark.getBoundingBox().getCenter().subtract(eye).normalize();
                Optional<Boolean> bullet = AmmoItem.consume(player);
                GunItem gun = gun(player);
                if (bullet.isEmpty() || gun == null) {
                    break;
                }
                AbilityProjectile shot = AbilityProjectile.spawn(player, HOMING_SHOT, eye.add(dir.scale(0.6)), dir.scale(gun.speed()), 0.18f,
                        bullet.get() ? GLWeapons.SEASTONE_COLOR : FLASH, WeaponManager.gunDamage(player, gun.damage()) * ctx.power(), 40);
                if (bullet.get()) {
                    shot.extra().putBoolean(GunItem.SEASTONE_TAG, true);
                }
                Vfx.trail(ctx.level(), player, eye, mark.getEyePosition(), 0xFF5A3A, 0.25f, Kind.SPARK);
            }
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.CROSSBOW_SHOOT, 1f, 0.6f);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.8f);
        }
    }
}
