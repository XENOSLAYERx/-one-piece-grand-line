package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.bodies;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
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
 * Tori Tori no Mi, Model: Phoenix. Become a phoenix wreathed in blue flames. The flames do not burn: they mend. Wounds
 * close as they open, allies are healed by a touch, and even a killing blow only brings the bird back out of its
 * own fire.
 */
public class PhoenixFruit extends ZoanFruit {
    public static final int COLOR = 0x3A8AE8;
    public static final int FLAME = 0x5AC8FF;
    public static final int GOLD = 0xF0E040;
    static final String M = "fruit/grandline:phoenix";
    static final String LAST_REBIRTH = "grandline_phoenix_rebirth";

    /**
     * @hybrid Phoenix-Man | Your arms become wings of blue fire: you can fly, and the flames heal you as you fight.
     * @beast Phoenix Form | Become a phoenix of blue fire: a fast flier whose flames close every wound.
     */
    static final ZoanForms FORMS = bodies("phoenix", COLOR, () -> SoundEvents.BLAZE_AMBIENT, ZoanForms.Traits.WINGED,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 30, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.5, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 60, Operation.ADD_VALUE)
                    .dimensions(1.8f, 1.6f, 1.2f));

    public static final DeferredHolder<Ability, PhoenixBrand> PHOENIX_BRAND = ability("phoenix_brand", PhoenixBrand::new);
    public static final DeferredHolder<Ability, TalonSeize> TALON_SEIZE = ability("phoenix_talon_seize", TalonSeize::new);
    public static final DeferredHolder<Ability, RestoringFlames> RESTORING_FLAMES = ability("phoenix_restoring_flames",
            RestoringFlames::new);
    public static final DeferredHolder<Ability, Pyreapple> PYREAPPLE = ability("phoenix_pyreapple", Pyreapple::new);
    public static final DeferredHolder<Ability, BlueBirdDive> BLUE_BIRD_DIVE = ability("phoenix_blue_bird_dive", BlueBirdDive::new);
    public static final DeferredHolder<Ability, EverlastingFlames> EVERLASTING = ability("phoenix_everlasting_flames",
            EverlastingFlames::new);
    public static final DeferredHolder<Ability, Ascension> ASCENSION = ability("phoenix_ascension", Ascension::new);

    public PhoenixFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Tori Tori no Mi, Model: Phoenix", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), PHOENIX_BRAND, TALON_SEIZE, RESTORING_FLAMES, PYREAPPLE,
                        BLUE_BIRD_DIVE)
                .awakened(EVERLASTING, ASCENSION), FORMS);
    }

    static void init() {}

    static void behaviors() {
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, PhoenixFruit::onDeath);
    }

    /** Blue flames mend the transformed body: a heart every half second (twice as fast once awakened). */
    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.tickCount % (awakened ? 5 : 10) == 0 && FORMS.any(user) && user.getHealth() < user.getMaxHealth()) {
            user.heal(1f);
            if (user.tickCount % 20 == 0) {
                Vfx.impact(user.serverLevel(), user, user.getBoundingBox().getCenter(), FLAME, 1.2f, Kind.FLAME);
            }
        }
    }

    /** The flames flare wherever a blow lands on the transformed body. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (FORMS.any(user) && event.getAmount() > 0) {
            Vfx.impact(user.serverLevel(), user, user.getBoundingBox().getCenter(), FLAME, 1.4f, Kind.FLAME);
        }
    }

    /**
     * A phoenix struck down while transformed bursts back out of its own fire at half health, once every
     * three minutes (one minute once awakened).
     */
    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(FruitManager.fruitOf(player) instanceof PhoenixFruit)
                || !FORMS.any(player)) {
            return;
        }
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(LAST_REBIRTH);
        int cooldown = FruitManager.isAwakened(player) ? 1200 : 3600;
        if (last != 0 && now - last < cooldown) {
            return;
        }
        event.setCanceled(true);
        player.getPersistentData().putLong(LAST_REBIRTH, now);
        player.setHealth(player.getMaxHealth() * 0.5f);
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
        Vfx.burst(player.serverLevel(), player, player.getBoundingBox().getCenter(), FLAME, 4f, Kind.FLAME, Vfx.FLASH | Vfx.RING);
        Vfx.eruption(player.serverLevel(), player, player.position(), FLAME, 4f, Kind.FLAME);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1.4f);
        player.displayClientMessage(Component.literal("You rise again from the blue flames!"), true);
    }

    /** Mend {@code target} with blue flames: health back and poisons burnt away. */
    static void mend(ServerPlayer player, LivingEntity target, float amount) {
        target.heal(amount);
        target.removeEffect(MobEffects.POISON);
        target.removeEffect(MobEffects.WITHER);
        target.clearFire();
        Vfx.impact(player.serverLevel(), player, target.getBoundingBox().getCenter(), FLAME, 1.4f, Kind.FLAME);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Shoot forward and drive a flaming kick through everything in your path.
     *
     * @name Phoenix Brand
     */
    public static final class PhoenixBrand extends Ability {
        PhoenixBrand() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, FLAME).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            for (LivingEntity target : AbilityDamage.line(player, 7, 1.4)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 13f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, player.getLookAngle().scale(1.4).add(0, 0.4, 0));
            }
            Kit.dash(player, 1.8, 0.1);
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), from.add(player.getLookAngle().scale(7)).add(0, 1, 0), FLAME, 1.4f, Kind.FLAME);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 1f, 1.4f);
        }
    }

    /** Seize the foe in front of you in burning talons and carry it up into the sky. Needs a form. */
    public static final class TalonSeize extends Ability {
        TalonSeize() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, GOLD).cooldown(120).stamina(12).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 5).ifPresent(target -> {
                ZoanKit.maul(player, target, 10f * FORMS.boost(player) * ctx.power(), 3, 0);
                Kit.launch(target, new Vec3(0, 1.8, 0));
                Kit.launch(player, new Vec3(0, 1.5, 0));
                Vfx.trail(ctx.level(), player, target.position(), target.position().add(0, 6, 0), FLAME, 1.2f, Kind.FLAME);
            });
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.4f, 0.8f);
        }
    }

    /** Wash yourself and every ally near you in blue flames: wounds close and poisons burn away. */
    public static final class RestoringFlames extends Ability {
        RestoringFlames() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, FLAME).cooldown(300).stamina(16).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity friend : ZoanKit.friends(player, 8)) {
                mend(player, friend, 8f * ctx.power());
                friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            }
            Vfx.burst(ctx.level(), player, player.position(), FLAME, 8f, Kind.FLAME, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1f, 1.4f);
        }
    }

    /**
     * Hurl a ball of blue fire that bursts where you point, battering foes and mending friends.
     *
     * @name Phoenix Pyreapple
     */
    public static final class Pyreapple extends Ability {
        Pyreapple() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, FLAME).cooldown(120).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.aimPoint(player, 24);
            double radius = 3.5 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 14f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, target.position().subtract(at).normalize().scale(1.0).add(0, 0.5, 0));
            }
            for (LivingEntity friend : ZoanKit.friends(player, 24)) {
                if (friend.distanceToSqr(at) <= radius * radius) {
                    mend(player, friend, 4f);
                }
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), at, FLAME, 0.6f, Kind.FLAME, Vfx.BEAM_PARTICLES);
            Vfx.burst(ctx.level(), player, at, FLAME, (float) radius, Kind.FLAME, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 1f, 1.3f);
        }
    }

    /** Fold your wings and dive onto the spot you point at: a crash of blue fire that throws everything aside. */
    public static final class BlueBirdDive extends Ability {
        BlueBirdDive() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, FLAME).cooldown(180).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 28), 12);
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            double radius = 4.5 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 16f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, target.position().subtract(at).normalize().scale(1.3).add(0, 0.6, 0));
            }
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), at.add(0, 1, 0), FLAME, 1.4f, Kind.FLAME);
            Vfx.burst(ctx.level(), player, at, FLAME, (float) radius, Kind.FLAME, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 1.4f, 1.1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** For twenty seconds the blue flames take most of every blow: wounds close before they finish opening. */
    public static final class EverlastingFlames extends Ability {
        EverlastingFlames() {
            super(props(ActivationType.TOGGLE, AbilityIcon.FLAME, FLAME).cooldown(900).stamina(24).staminaPerTick(0.1f).maxActive(400)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), FLAME, 3f, Kind.FLAME, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 1.4f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (!FORMS.any(ctx.player())) {
                return false;
            }
            if (ctx.activeTicks() % 4 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), FLAME, 1.6f, Kind.FLAME);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.3f);
        }
    }

    /** Rise on a pillar of blue fire: foes around you are blasted into the air, allies are fully restored. */
    public static final class Ascension extends Ability {
        Ascension() {
            super(props(ActivationType.INSTANT, AbilityIcon.SUN, FLAME).cooldown(1000).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 18f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.4, 0).add(target.position().subtract(player.position()).normalize().scale(0.6)));
            }
            for (LivingEntity friend : ZoanKit.friends(player, radius)) {
                mend(player, friend, 20f);
            }
            Kit.launch(player, new Vec3(0, 1.6, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
            Vfx.eruption(ctx.level(), player, player.position(), FLAME, 10f, Kind.FLAME);
            Vfx.burst(ctx.level(), player, player.position(), FLAME, (float) radius, Kind.FLAME, Vfx.RING | Vfx.GROUND | Vfx.HEAVY | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 2f, 0.7f);
        }
    }
}
