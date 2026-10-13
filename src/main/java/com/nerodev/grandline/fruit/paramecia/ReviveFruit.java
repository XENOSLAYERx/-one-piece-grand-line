package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Yomi Yomi no Mi. The user's soul comes back from the underworld after death; that same soul can leave the body,
 * and carries the underworld's chill into every swing of the sword and every note of the song.
 */
public class ReviveFruit extends DevilFruit {
    public static final int COLOR = 0x2A2A3A;
    public static final int SOUL = 0xA8E0FF;
    static final String M = "fruit/grandline:revive";
    static final String LAST_REVIVE = "grandline_revive_last";
    static final int REVIVE_COOLDOWN = 12000;

    public static final DeferredHolder<Ability, SoulSolid> SOUL_SOLID = ability("revive_soul_solid", SoulSolid::new);
    public static final DeferredHolder<Ability, SoulParade> SOUL_PARADE = ability("revive_soul_parade", SoulParade::new);
    public static final DeferredHolder<Ability, SoulProjection> SOUL_PROJECTION = ability("revive_soul_projection", SoulProjection::new);
    public static final DeferredHolder<Ability, Fubuki> FUBUKI = ability("revive_fubuki", Fubuki::new);
    public static final DeferredHolder<Ability, Lullaby> LULLABY = ability("revive_lullaby", Lullaby::new);
    public static final DeferredHolder<Ability, UnderworldChill> UNDERWORLD = ability("revive_underworld_chill", UnderworldChill::new);
    public static final DeferredHolder<Ability, SoulPound> SOUL_POUND = ability("revive_soul_pound", SoulPound::new);

    public static final ResourceLocation CHILL = GrandLine.id("revive_chill");

    public ReviveFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Yomi Yomi no Mi", COLOR)
                .abilities(SOUL_SOLID, SOUL_PARADE, SOUL_PROJECTION, FUBUKI, LULLABY)
                .awakened(UNDERWORLD, SOUL_POUND));
    }

    static void init() {}

    static void behaviors() {
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, ReviveFruit::onDeath);
        ProjectileBehavior.Registry.register(CHILL, Projectiles.zone(20, (p, target) -> {
            chill(target, 60);
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.FREEZE, p.damage());
            }
        }, null));
    }

    /** The cold of the underworld: slowed and frosting over. */
    static void chill(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + ticks));
    }

    /** Death is not the end: once every ten minutes, the soul returns and the body gets back up. */
    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(FruitManager.fruitOf(player) instanceof ReviveFruit)) {
            return;
        }
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(LAST_REVIVE);
        if (last != 0 && now - last < REVIVE_COOLDOWN) {
            return;
        }
        event.setCanceled(true);
        player.getPersistentData().putLong(LAST_REVIVE, now);
        player.setHealth(player.getMaxHealth() * 0.5f);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        Vfx.burst(player.serverLevel(), player, player.getBoundingBox().getCenter(), SOUL, 3f, Kind.GLOW, Vfx.FLASH | Vfx.RING);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 0.6f);
        player.displayClientMessage(Component.literal("Your soul returns from the underworld!"), true);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Swing a blade chilled by the underworld: everything it cuts starts to freeze.
     *
     * @name Soul Solid
     */
    public static final class SoulSolid extends Ability {
        SoulSolid() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, SOUL).cooldown(40).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5 * ctx.power(), 50)) {
                Kit.hit(player, target, AbilityDamage.FREEZE, 8f * ctx.power());
                chill(target, 100);
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.6)), player.getLookAngle(), SOUL, 2f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.ICE_SHATTER.get(), 0.7f, 1.6f);
        }
    }

    /**
     * Walk past a line of foes at a stroll; a heartbeat later every one of them is cut.
     *
     * @name Hanauta Sancho: Yahazu Giri
     */
    public static final class SoulParade extends Ability {
        SoulParade() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, SOUL).cooldown(80).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 9 * ctx.power(), 1.2)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 10f * ctx.power());
                chill(target, 60);
            }
            Vec3 start = player.position().add(0, 1, 0);
            Kit.dash(player, 1.6, 0.1);
            Vfx.trail(ctx.level(), player, start, start.add(player.getLookAngle().scale(9)), SOUL, 0.5f, Kind.SNOW);
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.5f);
        }
    }

    /** Let your soul slip out of your body and drift unseen through the air, scouting freely. */
    public static final class SoulProjection extends Ability {
        SoulProjection() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, SOUL).cooldown(300).stamina(16).staminaPerTick(0.15f)
                    .maxActive(400).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SOUL, 1.5f, Kind.GLOW, 0);
            ctx.sound(SoundEvents.SOUL_ESCAPE.value(), 1.2f, 1f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));
            player.resetFallDistance();
            if (ctx.activeTicks() % 5 == 0) {
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), SOUL, 0.6f, Kind.GLOW);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
            ctx.player().removeEffect(MobEffects.INVISIBILITY);
            ctx.sound(SoundEvents.SOUL_ESCAPE.value(), 1f, 0.7f);
        }
    }

    /**
     * A freezing sweep of the sword that seals foes in ice.
     *
     * @name Kasuri Uta: Fubuki Giri
     */
    public static final class Fubuki extends Ability {
        Fubuki() {
            super(props(ActivationType.INSTANT, AbilityIcon.SNOWFLAKE, SOUL).cooldown(160).stamina(18).mastery(M, 20)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 7 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.FREEZE, 7f * ctx.power());
                Kit.encase(target, Vfx.StatusLook.ICE, 40);
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(7)), SOUL, 1.5f,
                    Kind.SNOW, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.2f, 1.2f);
        }
    }

    /**
     * Hold to play a soothing tune: monsters nearby fall asleep, players grow drowsy.
     *
     * @name Nemuri Uta: Lullaby
     */
    public static final class Lullaby extends Ability {
        Lullaby() {
            super(props(ActivationType.HOLD, AbilityIcon.MOON, SOUL).cooldown(300).stamina(16).staminaPerTick(0.3f)
                    .maxActive(100).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FOCUS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 10 == 0) {
                float pitch = 0.6f + (ctx.activeTicks() / 10 % 5) * 0.15f;
                ctx.sound(SoundEvents.NOTE_BLOCK_HARP.value(), 1f, pitch);
                Vfx.burst(ctx.level(), player, player.getEyePosition(), SOUL, 1.2f, Kind.STAR, 0);
            }
            if (ctx.activeTicks() % 20 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 10)) {
                    if (target instanceof Player) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    } else if (target instanceof Mob && !target.getType().is(Tags.EntityTypes.BOSSES)
                            && !target.getType().is(EntityTypeTags.UNDEAD)) {
                        target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 45, 0, false, false, true));
                    }
                }
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Open the cold of the underworld around you: everyone nearby freezes slowly while it lasts. */
    public static final class UnderworldChill extends Ability {
        UnderworldChill() {
            super(props(ActivationType.INSTANT, AbilityIcon.SNOWFLAKE, COLOR).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            AbilityProjectile.zone(player, CHILL, player.position(), radius, SOUL, 3f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.5, 0), SOUL, (float) radius, Kind.SNOW,
                    Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.SOUL_ESCAPE.value(), 2f, 0.4f);
        }
    }

    /**
     * A single slash carrying the underworld's full cold: it cuts in a long line and freezes solid.
     *
     * @name Soul Pound
     */
    public static final class SoulPound extends Ability {
        SoulPound() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, SOUL).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 20 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.4)) {
                Kit.hit(player, target, AbilityDamage.FREEZE, 16f * ctx.power());
                Kit.encase(target, Vfx.StatusLook.ICE, 60);
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), AbilityDamage.lookTarget(player, range), SOUL, 1.2f, Kind.SNOW,
                    Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.ICE_SHATTER.get(), 1.6f, 0.6f);
        }
    }
}
