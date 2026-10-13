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
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Hira Hira no Mi. Make anything flutter like a flag: a steel cape that ripples bullets aside, a sword that flows like
 * a whip, ground that rolls like cloth under a foe's feet.
 */
public class RippleFruit extends DevilFruit {
    public static final int COLOR = 0xE8C840;
    public static final int CAPE = 0x3A3AA8;
    static final String M = "fruit/grandline:ripple";

    public static final DeferredHolder<Ability, FlagCape> FLAG_CAPE = ability("ripple_flag_cape", FlagCape::new);
    public static final DeferredHolder<Ability, FlutterBlade> FLUTTER_BLADE = ability("ripple_flutter_blade", FlutterBlade::new);
    public static final DeferredHolder<Ability, GroundFlag> GROUND_FLAG = ability("ripple_ground_flag", GroundFlag::new);
    public static final DeferredHolder<Ability, SteelBanner> STEEL_BANNER = ability("ripple_steel_banner", SteelBanner::new);
    public static final DeferredHolder<Ability, WhipSword> WHIP_SWORD = ability("ripple_whip_sword", WhipSword::new);
    public static final DeferredHolder<Ability, RollingLand> ROLLING_LAND = ability("ripple_rolling_land", RollingLand::new);
    public static final DeferredHolder<Ability, CapeStorm> CAPE_STORM = ability("ripple_cape_storm", CapeStorm::new);

    public static final ResourceLocation WAVES = GrandLine.id("ripple_waves");

    public RippleFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Hira Hira no Mi", COLOR)
                .abilities(FLAG_CAPE, FLUTTER_BLADE, GROUND_FLAG, STEEL_BANNER, WHIP_SWORD)
                .awakened(ROLLING_LAND, CAPE_STORM));
    }

    static void init() {}

    static void behaviors() {
        // The ground rolls like a flag in the wind, tossing everyone on it.
        ProjectileBehavior.Registry.register(WAVES, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.launch(target, target.getDeltaMovement().add(0, 0.8, 0));
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
            }
        }, null));
    }

    /** While the cape is up, blows from in front ripple off it and lose most of their force. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (!AbilityState.of(user).isActive(FLAG_CAPE.getId()) || event.getSource().getSourcePosition() == null) {
            return;
        }
        Vec3 to = event.getSource().getSourcePosition().subtract(user.position()).multiply(1, 0, 1);
        if (to.lengthSqr() > 1.0E-4 && to.normalize().dot(user.getLookAngle().multiply(1, 0, 1).normalize()) > 0.3) {
            event.setAmount(event.getAmount() * 0.4f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Hold up your steel cape: projectiles from in front ripple off it back the way they came, and blows hit softer. */
    public static final class FlagCape extends Ability {
        FlagCape() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, CAPE).cooldown(160).stamina(10).staminaPerTick(0.25f)
                    .maxActive(100).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.ARMOR_EQUIP_ELYTRA.value(), 1f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (Projectile p : ctx.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(3.5),
                    e -> e.getOwner() != player)) {
                Vec3 to = p.position().subtract(player.getEyePosition());
                if (to.lengthSqr() > 1.0E-4 && to.normalize().dot(look) > 0.3) {
                    p.setDeltaMovement(p.getDeltaMovement().scale(-1));
                    p.setOwner(player);
                    p.hurtMarked = true;
                    Vfx.impact(ctx.level(), player, p.position(), COLOR, 0.8f, Kind.SPARK);
                }
            }
            if (ctx.activeTicks() % 6 == 0) {
                Vfx.slash(ctx.level(), player, player.getEyePosition().add(look.scale(1.2)), ctx.right(), CAPE, 1.6f, 1);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Your blade flutters like cloth and reaches far further than it should. */
    public static final class FlutterBlade extends Ability {
        FlutterBlade() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 6.5 * ctx.power(), 35)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 9f * ctx.power());
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(2.5)), player.getLookAngle(), COLOR, 3f, 2);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        }
    }

    /** Ripple the ground in a line ahead of you like a flag snapping in the wind: everyone on it is tossed up. */
    public static final class GroundFlag extends Ability {
        GroundFlag() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(100).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.6)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 6f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.9, 0));
            }
            Vec3 start = player.position().add(0, 0.2, 0);
            Vfx.trail(ctx.level(), player, start, start.add(ctx.horizontalLook().scale(range)), COLOR, 1f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.STONE_BREAK, 1.2f, 0.8f);
        }
    }

    /** Stiffen your cape into a wall of steel bars in front of you. */
    public static final class SteelBanner extends Ability {
        SteelBanner() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, CAPE).cooldown(200).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(2.5));
            Kit.place(ctx.level(), Kit.wall(center, dir, 5, 3), Blocks.IRON_BARS.defaultBlockState(), 160);
            Vfx.burst(ctx.level(), player, center.add(0, 1, 0), CAPE, 2.5f, Kind.SPARK, 0);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ANVIL_PLACE, 0.8f, 1.2f);
        }
    }

    /** Your sword ripples into a long whip that lashes a line of foes and drags them toward you. */
    public static final class WhipSword extends Ability {
        WhipSword() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, CAPE).cooldown(90).stamina(12).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 11 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 0.9)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 11f * ctx.power());
                Kit.pull(target, player.position().add(player.getLookAngle().scale(2)), 0.8);
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), AbilityDamage.lookTarget(player, range), COLOR, 0.25f, Kind.SPARK,
                    Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** The whole land around you rolls like a flag in a storm, tossing everyone on it again and again. */
    public static final class RollingLand extends Ability {
        RollingLand() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            AbilityProjectile.zone(player, WAVES, player.position(), radius, COLOR, 4f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), COLOR, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.STONE_BREAK, 2f, 0.5f);
        }
    }

    /** Hold to whirl your rippling cape around you like a storm of blades. */
    public static final class CapeStorm extends Ability {
        CapeStorm() {
            super(props(ActivationType.HOLD, AbilityIcon.TORNADO, CAPE).cooldown(500).stamina(24).staminaPerTick(0.4f).maxActive(60)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_SWEEP);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.around(player, player.position().add(0, 1, 0), 5 * ctx.power())) {
                    Kit.hit(player, target, AbilityDamage.SLASH, 4f * ctx.power());
                }
                Vfx.spiral(ctx.level(), player, player.position().add(0, 1, 0), CAPE, 4f, Kind.SPARK);
                ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.4f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }
}
