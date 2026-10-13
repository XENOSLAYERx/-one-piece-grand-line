package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

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
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Netsu Netsu no Mi. Heat your body, and anything you touch, hot enough to boil the sea. Creatures standing in water
 * are boiled for double damage; the user shrugs off most of the heat.
 */
public class HeatFruit extends DevilFruit {
    public static final int COLOR = 0xE85A2A;
    public static final int YELLOW = 0xF0E040;
    static final String M = "fruit/grandline:heat";

    public static final DeferredHolder<Ability, HeatPalm> HEAT_PALM = ability("heat_palm", HeatPalm::new);
    public static final DeferredHolder<Ability, BoilingPoint> BOILING_POINT = ability("heat_boiling_point", BoilingPoint::new);
    public static final DeferredHolder<Ability, HeatAura> HEAT_AURA = ability("heat_aura", HeatAura::new);
    public static final DeferredHolder<Ability, SteamBurst> STEAM_BURST = ability("heat_steam_burst", SteamBurst::new);
    public static final DeferredHolder<Ability, SearingCharge> SEARING_CHARGE = ability("heat_searing_charge", SearingCharge::new);
    public static final DeferredHolder<Ability, BoilingSea> BOILING_SEA = ability("heat_boiling_sea", BoilingSea::new);
    public static final DeferredHolder<Ability, HeatWave> HEAT_WAVE = ability("heat_wave", HeatWave::new);

    public static final ResourceLocation SEA = GrandLine.id("heat_sea");

    public HeatFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Netsu Netsu no Mi", COLOR)
                .abilities(HEAT_PALM, BOILING_POINT, HEAT_AURA, STEAM_BURST, SEARING_CHARGE)
                .awakened(BOILING_SEA, HEAT_WAVE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SEA, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                scald(owner, target, p.damage(), 60);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
        }, null));
    }

    /** Heat damage that boils creatures standing in water for double. */
    static void scald(ServerPlayer player, LivingEntity target, float amount, int fireTicks) {
        boolean wet = target.isInWaterRainOrBubble();
        Kit.hit(player, target, AbilityDamage.FIRE, wet ? amount * 2 : amount);
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), fireTicks));
        if (wet) {
            Vfx.impact(player.serverLevel(), player, target.getBoundingBox().getCenter(), 0xFFFFFF, 1.4f, Kind.SMOKE);
        }
    }

    /** The user is used to the heat: fire and burning hurt them far less. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * 0.3f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Strike with a red-hot palm: it burns, and it boils anything standing in water.
     *
     * @name Heat Palm
     */
    public static final class HeatPalm extends Ability {
        HeatPalm() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.5, 50)) {
                scald(player, target, 8f * ctx.power(), 80);
                Kit.launch(target, ctx.horizontalLook().scale(0.6).add(0, 0.2, 0));
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.2)), COLOR, 1.4f, Kind.FLAME);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.FIRE_FIST.get(), 0.8f, 1.3f);
        }
    }

    /** Raise a foe's body to boiling point from afar: it burns, weakens and slows. */
    public static final class BoilingPoint extends Ability {
        BoilingPoint() {
            super(props(ActivationType.INSTANT, AbilityIcon.SUN, YELLOW).cooldown(90).stamina(11).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                scald(player, target, 6f * ctx.power(), 60);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), COLOR, 1.4f, Kind.SMOKE, 0);
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.STEAM.get(), 1f, 0.9f);
        }
    }

    /**
     * Glow red-hot: anything that stands near you burns, and you can't be set alight.
     *
     * @name Heat Aura
     */
    public static final class HeatAura extends Ability {
        HeatAura() {
            super(props(ActivationType.TOGGLE, AbilityIcon.FLAME, COLOR).cooldown(200).stamina(12).staminaPerTick(0.2f)
                    .maxActive(400).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0, false, false));
            if (ctx.activeTicks() % 10 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 4 * ctx.power())) {
                    scald(player, target, 2f * ctx.power(), 40);
                }
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.FLAME);
            }
            return true;
        }
    }

    /** Flash-boil the moisture around you into a scalding blast of steam that blinds and throws foes back. */
    public static final class SteamBurst extends Ability {
        SteamBurst() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, 0xFFFFFF).cooldown(160).stamina(14).mastery(M, 20)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 6 * ctx.power())) {
                scald(player, target, 7f * ctx.power(), 40);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                Vec3 away = target.position().subtract(player.position()).normalize();
                Kit.launch(target, away.scale(1.1).add(0, 0.35, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), 0xFFFFFF, 6f, Kind.SMOKE, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.STEAM.get(), 1.6f, 0.7f);
        }
    }

    /** Charge forward with searing fists: everything in your path is burned and bowled aside. */
    public static final class SearingCharge extends Ability {
        SearingCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, YELLOW).cooldown(140).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            for (LivingEntity target : AbilityDamage.line(player, 9 * ctx.power(), 1.4)) {
                scald(player, target, 12f * ctx.power(), 80);
                Kit.launch(target, ctx.right().scale(target.position().subtract(from).dot(ctx.right()) >= 0 ? 0.9 : -0.9)
                        .add(0, 0.4, 0));
            }
            Kit.dash(player, 2.2, 0.15);
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), from.add(ctx.horizontalLook().scale(9)).add(0, 1, 0), COLOR, 1.2f,
                    Kind.FLAME);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.FIRE_FIST.get(), 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Heat the ground around you until it boils: for ten seconds, everything standing in it scalds and slows. */
    public static final class BoilingSea extends Ability {
        BoilingSea() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, SEA);
            AbilityProjectile.zone(player, SEA, player.position(), 10 * ctx.power(), COLOR, 4f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 10f, Kind.SMOKE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.STEAM.get(), 2f, 0.5f);
        }
    }

    /**
     * Release a wave of searing heat that sweeps the land in front of you, setting everything ablaze.
     *
     * @name Heat Wave
     */
    public static final class HeatWave extends Ability {
        HeatWave() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, YELLOW).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 14 * ctx.power(), 40)) {
                scald(player, target, 14f * ctx.power(), 140);
                Kit.launch(target, look.scale(0.8).add(0, 0.3, 0));
            }
            for (int i = 1; i <= 4; i++) {
                Vfx.burst(ctx.level(), player, player.position().add(look.scale(i * 3.2)).add(0, 1, 0), COLOR, 1.6f + i * 0.7f,
                        Kind.FLAME, 0);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 1.6f, 0.8f);
        }
    }
}
