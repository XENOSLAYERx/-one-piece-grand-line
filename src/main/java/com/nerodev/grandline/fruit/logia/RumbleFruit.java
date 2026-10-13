package com.nerodev.grandline.fruit.logia;

import static com.nerodev.grandline.fruit.logia.LogiaBatch.ability;
import static com.nerodev.grandline.fruit.logia.LogiaBatch.props;

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
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Goro Goro no Mi. A body of lightning: strikes from the sky, discharges that freeze muscles, travel at the speed
 * of thunder. Lightning cannot hurt it. Rubber does not conduct it.
 */
public class RumbleFruit extends LogiaFruit {
    public static final int COLOR = 0x6AA0FF;
    public static final int CORE = 0xFFF27A;
    static final String M = "fruit/grandline:rumble";

    public static final DeferredHolder<Ability, ElThor> EL_THOR = ability("el_thor", ElThor::new);
    public static final DeferredHolder<Ability, Vari> VARI = ability("vari", Vari::new);
    public static final DeferredHolder<Ability, Sango> SANGO = ability("sango", Sango::new);
    public static final DeferredHolder<Ability, ThunderSpear> THUNDER_SPEAR = ability("thunder_spear", ThunderSpear::new);
    public static final DeferredHolder<Ability, ThunderTravel> THUNDER_TRAVEL = ability("thunder_travel", ThunderTravel::new);
    public static final DeferredHolder<Ability, Raigo> RAIGO = ability("raigo", Raigo::new);
    public static final DeferredHolder<Ability, Mamaragan> MAMARAGAN = ability("mamaragan", Mamaragan::new);

    public static final ResourceLocation SPEAR = GrandLine.id("thunder_spear");
    public static final ResourceLocation STORM_CLOUD = GrandLine.id("raigo");

    public RumbleFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.LEGENDARY, "Goro Goro no Mi", COLOR)
                .abilities(EL_THOR, VARI, SANGO, THUNDER_SPEAR, THUNDER_TRAVEL)
                .awakened(RAIGO, MAMARAGAN));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SPEAR, Projectiles.piercing(AbilityDamage.SHOCK, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Vfx.impact(owner.serverLevel(), target, target.getBoundingBox().getCenter(), CORE, 1f, Kind.SPARK);
            }
        }));
        // A thunder cloud: every so often a bolt falls on someone beneath it.
        ProjectileBehavior.Registry.register(STORM_CLOUD, Projectiles.zone(Integer.MAX_VALUE, (p, target) -> {}, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null || p.tickCount % 15 != 0) {
                return;
            }
            List<LivingEntity> under = Projectiles.inside(owner, p);
            if (!under.isEmpty()) {
                LivingEntity target = under.get(owner.getRandom().nextInt(under.size()));
                strike(owner, target, p.damage());
            } else if (owner.getRandom().nextBoolean()) {
                double a = owner.getRandom().nextDouble() * Mth.TWO_PI;
                double d = owner.getRandom().nextDouble() * p.size() / 2;
                bolt(owner.serverLevel(), p.position().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }));
    }

    /** A visual-only lightning bolt: the real flash and thunder, no fire. */
    static void bolt(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    static void strike(ServerPlayer owner, LivingEntity target, float damage) {
        bolt(owner.serverLevel(), target.position());
        Kit.hit(owner, target, AbilityDamage.SHOCK, damage);
        Kit.root(target, Vfx.StatusLook.IRON_BANDS, 15);
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_LIGHTNING);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Gather lightning, then call down a pillar of it wherever you look. */
    public static final class ElThor extends Ability {
        ElThor() {
            super(props(ActivationType.CHARGE, AbilityIcon.BOLT, CORE).cooldown(160).stamina(22).minCharge(15)
                    .maxActive(40).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.RUMBLE_ZAP.get(), 1f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 5 == 0) {
                ServerPlayer player = ctx.player();
                Vfx.beam(ctx.level(), player, ctx.aboveHead(0.5), ctx.aboveHead(3 + player.getRandom().nextDouble() * 2), CORE,
                        0.4f, Kind.SPARK, Vfx.BEAM_BOLT);
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float charge = Mth.clamp(ctx.activeTicks() / 40f, 0.4f, 1f);
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 40), 8);
            bolt(ctx.level(), at);
            for (LivingEntity target : AbilityDamage.around(player, at, 3.5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.SHOCK, 15f * charge * ctx.power());
                Kit.root(target, Vfx.StatusLook.IRON_BANDS, 25);
            }
            Vfx.burst(ctx.level(), player, at.add(0, 0.5, 0), COLOR, 4f, Kind.SPARK, Vfx.RING | Vfx.FLASH | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.RUMBLE_THUNDER.get(), 2f, 1f);
        }
    }

    /** A million volts in every direction: everyone around you is struck and locked up for a moment. */
    public static final class Vari extends Ability {
        Vari() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(120).stamina(20).mastery(M, 10)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 chest = player.position().add(0, 1.1, 0);
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 6 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.SHOCK, 9f * ctx.power());
                Kit.root(target, Vfx.StatusLook.IRON_BANDS, 20);
                Vfx.beam(ctx.level(), player, chest, target.getBoundingBox().getCenter(), COLOR, 0.5f, Kind.SPARK, Vfx.BEAM_BOLT);
            }
            Vfx.burst(ctx.level(), player, chest, COLOR, 5f, Kind.SPARK, Vfx.SHOCKWAVE | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.RUMBLE_ZAP.get(), 1.6f, 0.7f);
        }
    }

    /** A bolt from your hand that jumps from the first foe it hits to the ones beside it. */
    public static final class Sango extends Ability {
        Sango() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, CORE).cooldown(60).stamina(12).mastery(M, 20)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = ctx.handPos();
            Vec3 end = AbilityDamage.lookTarget(player, 26);
            List<LivingEntity> line = AbilityDamage.line(player, 26, 0.8);
            Set<Integer> hit = new HashSet<>();
            for (LivingEntity target : line) {
                Kit.hit(player, target, AbilityDamage.SHOCK, 11f * ctx.power());
                hit.add(target.getId());
            }
            Vfx.beam(ctx.level(), player, from, line.isEmpty() ? end : line.get(0).getBoundingBox().getCenter(), CORE, 0.7f,
                    Kind.SPARK, Vfx.BEAM_BOLT);
            // Arc onward from the last target to up to two more.
            LivingEntity last = line.isEmpty() ? null : line.get(line.size() - 1);
            for (int i = 0; i < 2 && last != null; i++) {
                LivingEntity next = null;
                double best = 36;
                for (LivingEntity candidate : AbilityDamage.around(player, last.position(), 6)) {
                    if (!hit.contains(candidate.getId()) && candidate.distanceToSqr(last) < best) {
                        best = candidate.distanceToSqr(last);
                        next = candidate;
                    }
                }
                if (next == null) {
                    break;
                }
                hit.add(next.getId());
                Kit.hit(player, next, AbilityDamage.SHOCK, 6f * ctx.power());
                Vfx.beam(ctx.level(), player, last.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), CORE, 0.5f,
                        Kind.SPARK, Vfx.BEAM_BOLT);
                last = next;
            }
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(GLSounds.RUMBLE_ZAP.get(), 1.2f, 1.2f);
        }
    }

    /** Hurl a spear of lightning that passes straight through everyone in its path. */
    public static final class ThunderSpear extends Ability {
        ThunderSpear() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, CORE).cooldown(40).stamina(10).mastery(M, 35)
                    .masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SPEAR, 2.6, 0.5f, CORE, 8f, 18);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.RUMBLE_ZAP.get(), 1f, 1.5f);
        }
    }

    /** Travel as a bolt to where you look; landing discharges into anyone nearby. */
    public static final class ThunderTravel extends Ability {
        ThunderTravel() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, CORE).cooldown(60).stamina(12).mastery(M, 50)
                    .masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position().add(0, 1, 0);
            if (Kit.blink(player, 26 * ctx.power(), CORE, Kind.SPARK) > 0) {
                Vfx.beam(ctx.level(), player, from, player.position().add(0, 1, 0), CORE, 0.6f, Kind.SPARK, Vfx.BEAM_BOLT);
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 2.5)) {
                    Kit.hit(player, target, AbilityDamage.SHOCK, 5f * ctx.power());
                }
            }
            ctx.sound(GLSounds.RUMBLE_THUNDER.get(), 0.8f, 1.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Summon a thunder cloud over an area: bolts keep falling on anyone beneath it. */
    public static final class Raigo extends Ability {
        Raigo() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(600).stamina(35).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 40), 10);
            AbilityProjectile.zone(player, STORM_CLOUD, at, 8 * ctx.power(), COLOR, 9f * ctx.power(), 200);
            Vfx.rain(ctx.level(), player, at, 0x8AA8D8, 8f, Kind.DROP);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.RUMBLE_THUNDER.get(), 2.5f, 0.6f);
        }
    }

    /** Lightning falls on every enemy for a great distance around you at once. */
    public static final class Mamaragan extends Ability {
        Mamaragan() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, COLOR).cooldown(900).stamina(45).masteryGain(2.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            List<LivingEntity> targets = new ArrayList<>(AbilityDamage.around(player, player.position(), 24 * ctx.power()));
            for (LivingEntity target : targets) {
                strike(player, target, 12f * ctx.power());
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 8f, Kind.SPARK, Vfx.FLASH | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.RUMBLE_THUNDER.get(), 3f, 0.8f);
        }
    }
}
