package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Tama Tama no Mi. A body like an egg, as Baron Tamago has: when the shell breaks it does not die but hatches into
 * something stronger, first a quick chick, then a fierce rooster, before slowly re-forming into an egg again.
 */
public class EggFruit extends DevilFruit {
    public static final int COLOR = 0xF4F0E0;
    public static final int YOLK = 0xE8C040;
    public static final int COMB = 0xD83A2A;
    static final String M = "fruit/grandline:egg";
    static final String STAGE = "grandline_egg_stage";
    static final String HATCHED_AT = "grandline_egg_hatched_at";
    static final int REFORM_TICKS = 3600;
    static final String[] STAGE_NAMES = {"Egg", "Chick", "Rooster"};

    static final List<Buffs.Mod> CHICK = List.of(
            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("egg_chick_speed"), 0.25, Operation.ADD_MULTIPLIED_BASE),
            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("egg_chick_attack"), 2, Operation.ADD_VALUE));
    static final List<Buffs.Mod> ROOSTER = List.of(
            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("egg_rooster_speed"), 0.2, Operation.ADD_MULTIPLIED_BASE),
            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("egg_rooster_attack"), 5, Operation.ADD_VALUE),
            Buffs.mod(Attributes.MAX_HEALTH, GrandLine.id("egg_rooster_health"), 6, Operation.ADD_VALUE),
            Buffs.mod(Attributes.JUMP_STRENGTH, GrandLine.id("egg_rooster_jump"), 0.25, Operation.ADD_VALUE),
            Buffs.mod(Attributes.SAFE_FALL_DISTANCE, GrandLine.id("egg_rooster_fall"), 6, Operation.ADD_VALUE));

    public static final DeferredHolder<Ability, OvumKick> OVUM_KICK = ability("egg_ovum_kick", OvumKick::new);
    public static final DeferredHolder<Ability, Eggshell> EGGSHELL = ability("egg_eggshell", Eggshell::new);
    public static final DeferredHolder<Ability, EggBomb> EGG_BOMB = ability("egg_bomb", EggBomb::new);
    public static final DeferredHolder<Ability, Hatch> HATCH = ability("egg_hatch", Hatch::new);
    public static final DeferredHolder<Ability, RoosterRush> ROOSTER_RUSH = ability("egg_rooster_rush", RoosterRush::new);
    public static final DeferredHolder<Ability, Reform> REFORM = ability("egg_reform", Reform::new);
    public static final DeferredHolder<Ability, DawnCrow> DAWN_CROW = ability("egg_dawn_crow", DawnCrow::new);

    public static final ResourceLocation EGG = GrandLine.id("egg_thrown_egg");

    public EggFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Tama Tama no Mi", COLOR)
                .abilities(OVUM_KICK, EGGSHELL, EGG_BOMB, HATCH, ROOSTER_RUSH)
                .awakened(REFORM, DAWN_CROW));
    }

    static void init() {}

    static void behaviors() {
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, EggFruit::onDeath);
        ProjectileBehavior.Registry.register(EGG, Projectiles.blast(2.5, AbilityDamage.BLAST, null,
                p -> Vfx.burst((ServerLevel) p.level(), p, p.position(), YOLK, 2.5f, Kind.SHARD, Vfx.FLASH)));
    }

    static int stage(ServerPlayer player) {
        return player.getPersistentData().getInt(STAGE);
    }

    /** Sets the hatching stage (0 egg, 1 chick, 2 rooster) and the body that goes with it. */
    static void setStage(ServerPlayer player, int stage) {
        player.getPersistentData().putInt(STAGE, stage);
        player.getPersistentData().putLong(HATCHED_AT, player.level().getGameTime());
        Buffs.remove(player, CHICK);
        Buffs.remove(player, ROOSTER);
        if (stage == 1) {
            Buffs.apply(player, CHICK);
        } else if (stage == 2) {
            Buffs.apply(player, ROOSTER);
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Each stage hits harder than the one before. */
    static float boost(ServerPlayer player) {
        return 1f + 0.25f * stage(player);
    }

    /** Cracks open into the next stage: a burst of shell, and the new body. */
    static void hatch(ServerPlayer player) {
        int next = Math.min(2, stage(player) + 1);
        setStage(player, next);
        Vfx.burst(player.serverLevel(), player, player.getBoundingBox().getCenter(), COLOR, 2.4f, Kind.SHARD, Vfx.FLASH | Vfx.RING);
        Vfx.impact(player.serverLevel(), player, player.getEyePosition(), next == 2 ? COMB : YOLK, 1.6f, Kind.FEATHER);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                next == 2 ? SoundEvents.PARROT_AMBIENT : SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.4f, next == 2 ? 0.6f : 1.4f);
        player.displayClientMessage(Component.literal("Your shell cracks: you hatch into a " + STAGE_NAMES[next] + "!"), true);
    }

    /** While the shell can still crack, a killing blow only hatches the user into the next stage. */
    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(FruitManager.fruitOf(player) instanceof EggFruit)
                || stage(player) >= 2) {
            return;
        }
        event.setCanceled(true);
        hatch(player);
        player.setHealth(player.getMaxHealth() * 0.6f);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3));
    }

    /** A hatched body slowly re-forms into an egg again. */
    @Override
    public boolean ticks() {
        return true;
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.tickCount % 20 != 0 || stage(user) == 0) {
            return;
        }
        long since = user.level().getGameTime() - user.getPersistentData().getLong(HATCHED_AT);
        if (since >= (awakened ? REFORM_TICKS / 2 : REFORM_TICKS)) {
            setStage(user, 0);
            user.displayClientMessage(Component.literal("Your body re-forms into an egg"), true);
        }
    }

    @Override
    public void onGained(ServerPlayer player) {
        setStage(player, 0);
    }

    @Override
    public void onLost(ServerPlayer player) {
        Buffs.remove(player, CHICK);
        Buffs.remove(player, ROOSTER);
        player.getPersistentData().remove(STAGE);
        player.getPersistentData().remove(HATCHED_AT);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** A high, elegant kick at the foe in front of you; it hits harder the further you have hatched. */
    public static final class OvumKick extends Ability {
        OvumKick() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, YOLK).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4, 55)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * boost(player) * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.1).add(0, 0.5, 0));
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(1.8)), new Vec3(0, 1, 0), YOLK, 2f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.2f);
        }
    }

    /** Hold your shell hard around you: you can barely move, but blows that reach you lose most of their force. */
    public static final class Eggshell extends Ability {
        Eggshell() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, COLOR).cooldown(140).stamina(8).staminaPerTick(0.3f).maxActive(100)
                    .masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.8f, Kind.SHARD, Vfx.RING);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1f, 1.3f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 3, false, false));
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.3f);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /**
     * Throw an egg that bursts with a bang on whatever it hits.
     *
     * @name Egg Bomb
     */
    public static final class EggBomb extends Ability {
        EggBomb() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, COLOR).cooldown(60).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, EGG, 1.2, 0.4f, COLOR, 10f * boost(ctx.player()), 40);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.EGG_THROW, 1f, 0.8f);
        }
    }

    /** Crack your own shell on purpose and hatch into the next stage, at the cost of some health. */
    public static final class Hatch extends Ability {
        Hatch() {
            super(props(ActivationType.INSTANT, AbilityIcon.TRANSFORM, YOLK).cooldown(600).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return stage(player) >= 2 ? Optional.of(Component.literal("You are already fully hatched")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.hurt(player.damageSources().generic(), Math.min(player.getHealth() - 1, player.getMaxHealth() * 0.25f));
            hatch(player);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
        }
    }

    /** Dash forward kicking: everything in your path is struck twice. Far stronger once you have hatched. */
    public static final class RoosterRush extends Ability {
        RoosterRush() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COMB).cooldown(160).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 1.6)) {
                for (int i = 0; i < 2; i++) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 8f * boost(player) * ctx.power());
                }
                Kit.launch(target, ctx.horizontalLook().scale(1.2).add(0, 0.6, 0));
            }
            Kit.dash(player, 2.0, 0.15);
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), from.add(ctx.horizontalLook().scale(length)).add(0, 1, 0), COMB, 1f,
                    Kind.FEATHER);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.PARROT_AMBIENT, 1.2f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Re-form your shell at once: back to an egg at full health, ready to hatch all over again. */
    public static final class Reform extends Ability {
        Reform() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(2400).stamina(20).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return stage(player) == 0 ? Optional.of(Component.literal("Your shell is already whole")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            setStage(player, 0);
            player.setHealth(player.getMaxHealth());
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 2.4f, Kind.GLOW, Vfx.FLASH | Vfx.RING);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(SoundEvents.CHICKEN_EGG, 1.4f, 0.8f);
        }
    }

    /** A rooster's dawn crow: everything around you is blasted back, dazed and deafened. Stronger the further you have hatched. */
    public static final class DawnCrow extends Ability {
        DawnCrow() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COMB).cooldown(700).stamina(26).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 12f * boost(player) * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.8, 0.5);
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), COMB, (float) radius, Kind.SHOCKWAVE, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.PARROT_AMBIENT, 2f, 0.4f);
        }
    }
}
