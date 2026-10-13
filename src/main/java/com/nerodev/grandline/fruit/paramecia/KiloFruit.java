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
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Kilo Kilo no Mi. Change your own weight anywhere from one kilogram to ten thousand: float on the wind, then drop
 * like a meteor. Awakened, the power reaches out to the weight of everything around you.
 */
public class KiloFruit extends DevilFruit {
    public static final int COLOR = 0xF0E040;
    public static final int LEAF = 0x4A8A3A;
    static final String M = "fruit/grandline:kilo";
    static final String PRESS = "grandline_kilo_press";

    static final Buffs.Mod PRESS_GRAVITY = Buffs.mod(Attributes.GRAVITY, GrandLine.id("kilo_press_gravity"), 0.3, Operation.ADD_VALUE);
    static final Buffs.Mod HEAVY_GRAVITY = Buffs.mod(Attributes.GRAVITY, GrandLine.id("kilo_ten_ton_gravity"), 0.12, Operation.ADD_VALUE);
    static final Buffs.Mod HEAVY_JUMP = Buffs.mod(Attributes.JUMP_STRENGTH, GrandLine.id("kilo_ten_ton_jump"), -1, Operation.ADD_MULTIPLIED_TOTAL);

    public static final DeferredHolder<Ability, FeatherWeight> FEATHER = ability("kilo_feather_weight", FeatherWeight::new);
    public static final DeferredHolder<Ability, TenThousandPress> PRESS_MOVE = ability("kilo_press", TenThousandPress::new);
    public static final DeferredHolder<Ability, HeavyBody> HEAVY_BODY = ability("kilo_heavy_body", HeavyBody::new);
    public static final DeferredHolder<Ability, WeightKick> WEIGHT_KICK = ability("kilo_weight_kick", WeightKick::new);
    public static final DeferredHolder<Ability, KiloBounce> BOUNCE = ability("kilo_bounce", KiloBounce::new);
    public static final DeferredHolder<Ability, TenTonField> TEN_TON_FIELD = ability("kilo_ten_ton_field", TenTonField::new);
    public static final DeferredHolder<Ability, FeatherField> FEATHER_FIELD = ability("kilo_feather_field", FeatherField::new);

    public static final ResourceLocation HEAVY_ZONE = GrandLine.id("kilo_heavy_field");

    public KiloFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Kilo Kilo no Mi", COLOR)
                .abilities(FEATHER, PRESS_MOVE, HEAVY_BODY, WEIGHT_KICK, BOUNCE)
                .awakened(TEN_TON_FIELD, FEATHER_FIELD));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(HEAVY_ZONE, Projectiles.zone(10, (p, target) -> {
            Buffs.timed(target, HEAVY_GRAVITY, 15);
            Buffs.timed(target, HEAVY_JUMP, 15);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 3));
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 20 == 0) {
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
            }
        }, null));
    }

    /** A light body lands softly; a heavy one lands as a weapon. */
    @Override
    public void onFall(ServerPlayer user, LivingFallEvent event) {
        if (user.getPersistentData().getLong(PRESS) == 0) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.5f);
            return;
        }
        user.getPersistentData().remove(PRESS);
        Buffs.remove(user, PRESS_GRAVITY);
        float damage = Math.min(26f, 8f + event.getDistance() * 1.2f) * user.getPersistentData().getFloat(PRESS + "_power");
        double radius = Math.min(6, 2.5 + event.getDistance() * 0.15);
        for (LivingEntity target : AbilityDamage.around(user, user.position(), radius)) {
            Kit.hit(user, target, AbilityDamage.CRUSH, damage);
            Kit.launch(target, target.position().subtract(user.position()).normalize().scale(0.6).add(0, 0.5, 0));
        }
        event.setDamageMultiplier(0f);
        Vfx.burst(user.serverLevel(), user, user.position().add(0, 0.1, 0), COLOR, (float) radius, Kind.SHARD,
                Vfx.RING | Vfx.SHOCKWAVE | Vfx.GROUND | Vfx.HEAVY);
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(), GLSounds.GIANT_IMPACT.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.7f);
    }

    /** The press wears off if you never land (water, a ledge you grab, a long fall cancelled). */
    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        long since = user.getPersistentData().getLong(PRESS);
        if (since != 0 && user.level().getGameTime() - since > 120) {
            user.getPersistentData().remove(PRESS);
            Buffs.remove(user, PRESS_GRAVITY);
        }
    }

    @Override
    public boolean ticks() {
        return true;
    }

    @Override
    public void onLost(ServerPlayer player) {
        player.getPersistentData().remove(PRESS);
        Buffs.remove(player, PRESS_GRAVITY);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Become as light as a feather: jump high, fall slowly and ride the wind.
     *
     * @name 1-Kilo Body
     */
    public static final class FeatherWeight extends Buffs.Toggle {
        FeatherWeight() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, COLOR).cooldown(60).stamina(8).staminaPerTick(0.1f)
                            .maxActive(1200).masteryGain(0.6),
                    List.of(Buffs.mod(Attributes.GRAVITY, GrandLine.id("kilo_feather_gravity"), -0.06, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.JUMP_STRENGTH, GrandLine.id("kilo_feather_jump"), 0.25, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.SAFE_FALL_DISTANCE, GrandLine.id("kilo_feather_fall"), 40, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 1.5f, Kind.FEATHER, Vfx.RING);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 0.6f, 1.6f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (!ctx.player().onGround() && ctx.activeTicks() % 6 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 0.4f, Kind.FEATHER);
            }
            return true;
        }
    }

    /**
     * Leap up and turn to ten tonnes: you crash down on everyone below, harder the higher you fell.
     *
     * @name 10,000-Kilo Press
     */
    public static final class TenThousandPress extends Ability {
        TenThousandPress() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, LEAF).cooldown(120).stamina(16).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (player.onGround()) {
                Kit.launch(player, new Vec3(player.getLookAngle().x * 0.4, 1.3, player.getLookAngle().z * 0.4));
            }
            player.getPersistentData().putLong(PRESS, ctx.level().getGameTime());
            player.getPersistentData().putFloat(PRESS + "_power", ctx.power());
            Buffs.apply(player, PRESS_GRAVITY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ANVIL_PLACE, 0.7f, 0.6f);
        }
    }

    /** Settle into ten thousand kilos: nothing can push you, blows hurt less, but you move slowly. */
    public static final class HeavyBody extends Buffs.Toggle {
        HeavyBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, LEAF).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(400).mastery(M, 15).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("kilo_heavy_knockback"), 1, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("kilo_heavy_armor"), 8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("kilo_heavy_speed"), -0.45, Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), LEAF, 2f, Kind.SHARD, Vfx.GROUND);
            ctx.sound(SoundEvents.ANVIL_LAND, 0.6f, 0.5f);
        }
    }

    /** Shift your whole weight into one kick that sends a foe flying. */
    public static final class WeightKick extends Ability {
        WeightKick() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, COLOR).cooldown(70).stamina(12).mastery(M, 25)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4 * ctx.power(), 40)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 2.0, 0.5);
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.5)), COLOR, 1.4f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1f, 0.7f);
        }
    }

    /** Drop to one kilogram in the middle of a jump and let the push carry you high into the air. */
    public static final class KiloBounce extends Ability {
        KiloBounce() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(90).stamina(10).mastery(M, 35)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            Kit.launch(player, new Vec3(look.x * 0.7, 1.7, look.z * 0.7));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
            player.resetFallDistance();
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 1.6f, Kind.FEATHER, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Everything around you grows unbearably heavy: foes are pinned, grounded and slowly crushed.
     *
     * @name Ten-Tonne Field
     */
    public static final class TenTonField extends Ability {
        TenTonField() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, LEAF).cooldown(700).stamina(36).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 9 * ctx.power();
            AbilityProjectile.zone(player, HEAVY_ZONE, player.position(), radius, LEAF, 3f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), LEAF, (float) radius, Kind.SHARD,
                    Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ANVIL_LAND, 1.5f, 0.4f);
        }
    }

    /** Make everything around you weigh nothing: foes float helplessly up, then fall when the effect ends. */
    public static final class FeatherField extends Ability {
        FeatherField() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 9 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 2));
                Vfx.impact(ctx.level(), target, target.position(), COLOR, 1f, Kind.FEATHER);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.5, 0), COLOR, (float) radius, Kind.FEATHER, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.6f, 0.8f);
        }
    }
}
