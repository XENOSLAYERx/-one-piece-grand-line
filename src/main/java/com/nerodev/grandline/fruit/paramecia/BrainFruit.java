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
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Nomi Nomi no Mi. A brain that never stops growing: the user reads a creature's health at a glance, finds its weak
 * points, and builds lasers, drones and shields on the spot.
 */
public class BrainFruit extends DevilFruit {
    public static final int COLOR = 0xF0C8C8;
    public static final int PLUM = 0x8A4A6A;
    public static final int LASER = 0xFFE07A;
    static final String M = "fruit/grandline:brain";
    static final String WEAK_BY = "grandline_weak_by";
    static final String WEAK_UNTIL = "grandline_weak_until";
    public static final ResourceLocation DRONE = GrandLine.id("brain_drone");

    public static final DeferredHolder<Ability, Analyze> ANALYZE = ability("brain_analyze", Analyze::new);
    public static final DeferredHolder<Ability, LaserPistol> LASER_PISTOL = ability("brain_laser_pistol", LaserPistol::new);
    public static final DeferredHolder<Ability, Drones> DRONES = ability("brain_drones", Drones::new);
    public static final DeferredHolder<Ability, EnergyShield> ENERGY_SHIELD = ability("brain_energy_shield", EnergyShield::new);
    public static final DeferredHolder<Ability, Overclock> OVERCLOCK = ability("brain_overclock", Overclock::new);
    public static final DeferredHolder<Ability, PunkRecords> PUNK_RECORDS = ability("brain_punk_records", PunkRecords::new);
    public static final DeferredHolder<Ability, MotherFlame> MOTHER_FLAME = ability("brain_mother_flame", MotherFlame::new);

    public BrainFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Nomi Nomi no Mi", COLOR)
                .abilities(ANALYZE, LASER_PISTOL, DRONES, ENERGY_SHIELD, OVERCLOCK)
                .awakened(PUNK_RECORDS, MOTHER_FLAME));
    }

    static void init() {}

    static void behaviors() {}

    /** Marks {@code target}'s weak points for {@code ticks}: the user's blows on it land 30% harder. */
    static void markWeak(ServerPlayer player, LivingEntity target, int ticks) {
        CompoundTag data = target.getPersistentData();
        data.putUUID(WEAK_BY, player.getUUID());
        data.putLong(WEAK_UNTIL, target.level().getGameTime() + ticks);
    }

    static boolean isWeakTo(ServerPlayer player, LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        return data.hasUUID(WEAK_BY) && player.getUUID().equals(data.getUUID(WEAK_BY))
                && data.getLong(WEAK_UNTIL) >= target.level().getGameTime();
    }

    static Component readout(LivingEntity target) {
        return Component.empty().append(target.getDisplayName()).append(Component.literal(String.format(" %.0f/%.0f ❤  %d armour",
                target.getHealth(), target.getMaxHealth(), target.getArmorValue())).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        if (isWeakTo(user, target)) {
            event.setAmount(event.getAmount() * 1.3f);
        }
    }

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, DRONE);
    }

    /** Insight: whatever the user looks at, they read its health and armour. */
    @Override
    public boolean ticks() {
        return true;
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.tickCount % 20 == 0) {
            Kit.target(user, 16).ifPresent(target -> user.displayClientMessage(readout(target), true));
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Study a foe: read its health and armour, light it up, and mark its weak points for ten seconds. */
    public static final class Analyze extends Ability {
        Analyze() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(100).stamina(6).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 24).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                markWeak(player, target, 200);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                player.displayClientMessage(readout(target), true);
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), PLUM, 1.2f, Kind.RING);
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1.6f);
        }
    }

    /** Fire a pocket laser: a beam that burns through everything in a line. */
    public static final class LaserPistol extends Ability {
        LaserPistol() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, LASER).cooldown(40).stamina(9).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 20 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 0.8)) {
                Kit.hit(player, target, AbilityDamage.LIGHT, 9f * ctx.power());
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(range)), LASER, 0.3f,
                    Kind.GLOW, Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(GLSounds.GLINT_LASER.get(), 1f, 1.3f);
        }
    }

    /** Launch three combat drones that hunt your foes for twenty seconds. */
    public static final class Drones extends Ability {
        Drones() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, PLUM).cooldown(500).stamina(18).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Minions.dismiss(player, DRONE);
            for (Vec3 at : Kit.circle(player.position().add(0, 2, 0), 1.5, 3, 0)) {
                Minions.spawn(player, DRONE, EntityType.VEX, at, 400, vex -> vex.setLimitedLife(400));
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1f, 1.8f);
        }
    }

    /** Switch on a personal energy shield: much tougher armour while it holds. */
    public static final class EnergyShield extends Buffs.Toggle {
        EnergyShield() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, LASER).cooldown(240).stamina(12).staminaPerTick(0.2f)
                            .maxActive(400).mastery(M, 20).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("brain_shield_armor"), 8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("brain_shield_toughness"), 4, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            ctx.player().addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), LASER, 2f, Kind.RING, Vfx.FLASH | Vfx.RING);
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 1f, 1.6f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), LASER, 1.4f, Kind.GLOW);
            }
            return true;
        }

        @Override
        protected void stopped(AbilityContext ctx) {
            ctx.player().removeEffect(MobEffects.ABSORPTION);
            ctx.sound(SoundEvents.BEACON_DEACTIVATE, 1f, 1.6f);
        }
    }

    /** Hold to overclock your laser, release to fire: the longer the charge, the hotter and further it burns. */
    public static final class Overclock extends Ability {
        Overclock() {
            super(props(ActivationType.CHARGE, AbilityIcon.BEAM, PLUM).cooldown(200).stamina(18).minCharge(10).maxActive(50)
                    .mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(SoundEvents.BEACON_AMBIENT, 1f, 1.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.handPos(), LASER, 0.5f + ctx.activeTicks() / 50f, Kind.GLOW);
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float charge = Mth.clamp(ctx.activeTicks() / 50f, 0.3f, 1f);
            double range = (16 + 16 * charge) * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.0 + charge)) {
                Kit.hit(player, target, AbilityDamage.LIGHT, (8f + 10f * charge) * ctx.power());
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(range)), LASER,
                    0.5f + charge, Kind.GLOW, Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GLINT_LASER.get(), 1.4f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Tap Punk Records, the shared library of every mind: for thirty seconds you see every creature around you through
     * walls and know all their weak points.
     */
    public static final class PunkRecords extends Ability {
        PunkRecords() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, PLUM).cooldown(1000).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 32)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
                markWeak(player, target, 600);
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), PLUM, 6f, Kind.RING, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.HAKI_SENSE.get(), 1.4f, 1.2f);
        }
    }

    /** Fire Mother Flame, the sun in a bottle: a colossal beam that burns everything in a long line. */
    public static final class MotherFlame extends Ability {
        MotherFlame() {
            super(props(ActivationType.INSTANT, AbilityIcon.SUN, LASER).cooldown(900).stamina(36).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 40 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 2.4)) {
                Kit.hit(player, target, AbilityDamage.LIGHT, 22f * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(range)), LASER, 2.4f,
                    Kind.GLOW, Vfx.BEAM_GLOW);
            Vfx.burst(ctx.level(), player, ctx.handPos(), LASER, 3f, Kind.GLOW, Vfx.FLASH | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(GLSounds.GLINT_LASER.get(), 2f, 0.5f);
        }
    }
}
