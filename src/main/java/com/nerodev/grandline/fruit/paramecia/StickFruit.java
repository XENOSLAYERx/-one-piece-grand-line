package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Beta Beta no Mi. Sticky, stringy mucus that glues whatever it touches in place, and burns like oil once lit.
 */
public class StickFruit extends DevilFruit {
    public static final int COLOR = 0x8AA83A;
    public static final int DARK = 0x3A4A1A;
    static final String M = "fruit/grandline:stick";
    static final String GLUED = "grandline_glued_until";

    public static final DeferredHolder<Ability, MucusShot> MUCUS_SHOT = ability("stick_mucus_shot", MucusShot::new);
    public static final DeferredHolder<Ability, StickyTrap> STICKY_TRAP = ability("stick_trap", StickyTrap::new);
    public static final DeferredHolder<Ability, StickyCoat> STICKY_COAT = ability("stick_coat", StickyCoat::new);
    public static final DeferredHolder<Ability, Ignite> IGNITE = ability("stick_ignite", Ignite::new);
    public static final DeferredHolder<Ability, StringPull> STRING_PULL = ability("stick_string_pull", StringPull::new);
    public static final DeferredHolder<Ability, MucusFlood> MUCUS_FLOOD = ability("stick_mucus_flood", MucusFlood::new);
    public static final DeferredHolder<Ability, Inferno> INFERNO = ability("stick_inferno", Inferno::new);

    public static final ResourceLocation GLOB = GrandLine.id("stick_glob");
    public static final ResourceLocation POOL = GrandLine.id("stick_pool");

    public StickFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Beta Beta no Mi", COLOR)
                .abilities(MUCUS_SHOT, STICKY_TRAP, STICKY_COAT, IGNITE, STRING_PULL)
                .awakened(MUCUS_FLOOD, INFERNO));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(GLOB, Projectiles.bolt(AbilityDamage.STRIKE, (p, target) -> glue(target, 70)));
        ProjectileBehavior.Registry.register(POOL, Projectiles.zone(10, (p, target) -> glue(target, 20), null));
    }

    /** Glues a creature in place (and marks it as covered in flammable mucus for a while). */
    static void glue(LivingEntity target, int ticks) {
        Kit.root(target, Vfx.StatusLook.GLUE, ticks);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks + 40, 1));
        target.getPersistentData().putLong(GLUED, target.level().getGameTime() + ticks + 200);
    }

    static boolean glued(LivingEntity target) {
        return target.getPersistentData().getLong(GLUED) >= target.level().getGameTime();
    }

    /** In Sticky Coat, a bare-handed attacker sticks fast; projectiles glue to the mucus and stop. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (!AbilityState.of(user).isActive(STICKY_COAT.getId())) {
            return;
        }
        if (event.getSource().getDirectEntity() instanceof Projectile) {
            event.setAmount(event.getAmount() * 0.3f);
        } else if ((event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK))
                && event.getSource().getEntity() instanceof LivingEntity attacker) {
            glue(attacker, 40);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Spit a glob of sticky mucus: whoever it hits is glued to the spot. */
    public static final class MucusShot extends Ability {
        MucusShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, COLOR).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, GLOB, 1.4, 0.5f, COLOR, 3f, 30).withGravity(0.03);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SLIME_SQUISH, 1f, 0.6f);
        }
    }

    /** Spread a pool of mucus where you point: anyone who steps in gets stuck. */
    public static final class StickyTrap extends Ability {
        StickyTrap() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, DARK).cooldown(160).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            AbilityProjectile.zone(player, POOL, at, 4 * ctx.power(), COLOR, 0f, 200);
            Vfx.burst(ctx.level(), player, at, COLOR, 4f, Kind.DROP, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.SLIME_BLOCK_PLACE, 1.2f, 0.6f);
        }
    }

    /** Coat yourself in mucus: projectiles stick and lose their force, and anyone who punches you sticks fast. */
    public static final class StickyCoat extends Ability {
        StickyCoat() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                    .maxActive(500).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 8 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 0.8f, Kind.DROP);
            }
            return true;
        }
    }

    /** Light the mucus: everyone glued nearby goes up in flames. */
    public static final class Ignite extends Ability {
        Ignite() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, 0xFF8A2A).cooldown(200).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int lit = 0;
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 16)) {
                if (glued(target)) {
                    Kit.hit(player, target, AbilityDamage.FIRE, 10f * ctx.power());
                    target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
                    Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), 0xFF8A2A, 2f, Kind.FLAME, Vfx.FLASH);
                    lit++;
                }
            }
            if (lit == 0) {
                player.displayClientMessage(Component.literal("Nobody nearby is covered in mucus"), true);
            }
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 1.2f, 1.2f);
        }
    }

    /** Fling a sticky string at a foe and reel it in, glued, to your feet. */
    public static final class StringPull extends Ability {
        StringPull() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, DARK).cooldown(100).stamina(12).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 18).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 18).ifPresent(target -> {
                Kit.pull(target, player.position().add(player.getLookAngle().scale(1.5)), 1.3);
                glue(target, 30);
                Vfx.trail(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.handPos(), COLOR, 0.4f, Kind.DROP);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Flood the area around you with mucus: everyone in it is stuck fast, again and again. */
    public static final class MucusFlood extends Ability {
        MucusFlood() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            AbilityProjectile.zone(player, POOL, player.position(), radius, COLOR, 0f, 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), COLOR, (float) radius, Kind.DROP, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.SLIME_BLOCK_PLACE, 2f, 0.4f);
        }
    }

    /** Set every drop of mucus around you alight at once: a sea of fire over everyone you have glued. */
    public static final class Inferno extends Ability {
        Inferno() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, 0xFF6A1A).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 16 * ctx.power())) {
                float damage = (glued(target) ? 18f : 6f) * ctx.power();
                Kit.hit(player, target, AbilityDamage.FIRE, damage);
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), glued(target) ? 160 : 60));
            }
            Kit.discard(player, POOL);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.5, 0), 0xFF6A1A, 16f, Kind.FLAME, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 2f, 0.7f);
        }
    }
}
