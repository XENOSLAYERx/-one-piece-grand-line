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
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Jiki Jiki no Mi. Magnetise anything metal: pull it in, fling it away, and build an arm, or a whole body, out of
 * scrap. Armoured foes are dragged harder and hit harder.
 */
public class MagnetFruit extends DevilFruit {
    public static final int COLOR = 0x7A1A1A;
    public static final int STEEL = 0xB8B8C0;
    static final String M = "fruit/grandline:magnet";

    public static final DeferredHolder<Ability, Repel> REPEL = ability("magnet_repel", Repel::new);
    public static final DeferredHolder<Ability, Attract> ATTRACT = ability("magnet_attract", Attract::new);
    public static final DeferredHolder<Ability, ScrapArm> SCRAP_ARM = ability("magnet_scrap_arm", ScrapArm::new);
    public static final DeferredHolder<Ability, ScrapShot> SCRAP_SHOT = ability("magnet_scrap_shot", ScrapShot::new);
    public static final DeferredHolder<Ability, MagneticSlam> MAGNETIC_SLAM = ability("magnet_slam", MagneticSlam::new);
    public static final DeferredHolder<Ability, DamnedPunk> DAMNED_PUNK = ability("magnet_damned_punk", DamnedPunk::new);
    public static final DeferredHolder<Ability, PunkRotten> PUNK_ROTTEN = ability("magnet_punk_rotten", PunkRotten::new);

    public static final ResourceLocation SCRAP = GrandLine.id("magnet_scrap");

    public MagnetFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Jiki Jiki no Mi", COLOR)
                .abilities(REPEL, ATTRACT, SCRAP_ARM, SCRAP_SHOT, MAGNETIC_SLAM)
                .awakened(DAMNED_PUNK, PUNK_ROTTEN));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SCRAP, Projectiles.piercing(AbilityDamage.STRIKE, null));
    }

    /** Armoured creatures are full of metal: magnetism grips them harder. */
    static float metalBonus(LivingEntity target) {
        return target.getArmorValue() >= 8 ? 1.5f : target.getArmorValue() > 0 ? 1.2f : 1f;
    }

    /** Pulls loose items and foes within {@code radius} toward the user. */
    static void attract(ServerPlayer player, double radius, double speed, float damage) {
        Vec3 to = player.position().add(0, 0.5, 0);
        for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
            Kit.pull(target, to, speed * metalBonus(target));
            if (damage > 0 && target.getArmorValue() > 0) {
                Kit.hit(player, target, AbilityDamage.STRIKE, damage * metalBonus(target));
            }
        }
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(radius))) {
            Kit.pull(item, to, 0.8);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Push everything in front of you away with a magnetic wave: foes are flung, and arrows and shots turn back. */
    public static final class Repel extends Ability {
        Repel() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, STEEL).cooldown(50).stamina(9).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (LivingEntity target : AbilityDamage.cone(player, 8 * ctx.power(), 40)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * ctx.power());
                Kit.launch(target, look.scale(1.4 * metalBonus(target)).add(0, 0.35, 0));
            }
            AABB box = player.getBoundingBox().expandTowards(look.scale(8)).inflate(2);
            for (Projectile shot : ctx.level().getEntitiesOfClass(Projectile.class, box, e -> e.getOwner() != player)) {
                shot.setDeltaMovement(shot.getDeltaMovement().scale(-1));
                shot.setOwner(player);
                shot.hurtMarked = true;
                Vfx.impact(ctx.level(), player, shot.position(), STEEL, 0.8f, Kind.SPARK);
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(2)), STEEL, 2.5f, Kind.SPARK, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.RUMBLE_ZAP.get(), 1f, 0.6f);
        }
    }

    /** Magnetise everything around you and pull it in: foes are dragged to your feet, loose items fly to you. */
    public static final class Attract extends Ability {
        Attract() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, COLOR).cooldown(100).stamina(11).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            attract(player, 12 * ctx.power(), 1.2, 4f * ctx.power());
            Vfx.spiral(ctx.level(), player, player.position().add(0, 1, 0), STEEL, 6f, Kind.SPARK);
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.RUMBLE_ZAP.get(), 1.2f, 0.4f);
        }
    }

    /** Pull scrap metal onto your arm into a massive fist: your blows land much harder and reach further. */
    public static final class ScrapArm extends Buffs.Toggle {
        ScrapArm() {
            super(props(ActivationType.TOGGLE, AbilityIcon.FIST, STEEL).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(600).mastery(M, 10).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("magnet_arm_damage"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ENTITY_INTERACTION_RANGE, GrandLine.id("magnet_arm_reach"), 1.5, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("magnet_arm_kb"), 0.3, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.handPos(), STEEL, 1.6f, Kind.SHARD, 0);
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(SoundEvents.ANVIL_USE, 1f, 0.7f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.handPos(), STEEL, 0.7f, Kind.SPARK);
            }
            return true;
        }
    }

    /** Fire three chunks of magnetised scrap that tear through everything in their way. */
    public static final class ScrapShot extends Ability {
        ScrapShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, STEEL).cooldown(70).stamina(11).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.08)).normalize();
                AbilityProjectile.spawn(player, SCRAP, ctx.handPos(), dir.scale(2.0), 0.5f, STEEL, 6f * ctx.power(), 25);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ANVIL_LAND, 0.6f, 1.6f);
        }
    }

    /**
     * Magnetise a foe and drag it into the ground: it is crushed and pinned.
     *
     * @name Magnetic Slam
     */
    public static final class MagneticSlam extends Ability {
        MagneticSlam() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(180).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 14f * ctx.power() * metalBonus(target));
                target.setDeltaMovement(0, -2, 0);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                Vfx.burst(ctx.level(), player, target.position(), STEEL, 2.5f, Kind.SHARD, Vfx.GROUND | Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ANVIL_LAND, 1.4f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Charge a magnetic railgun on your arm and fire: a bolt that tears through everything in a long line. */
    public static final class DamnedPunk extends Ability {
        DamnedPunk() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, COLOR).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 30 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.6)) {
                Kit.hit(player, target, AbilityDamage.SHOCK, 20f * ctx.power());
                Kit.launch(target, player.getLookAngle().scale(1.2).add(0, 0.3, 0));
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(range)), 0xFF4A4A, 1.4f,
                    Kind.SPARK, Vfx.BEAM_GLOW);
            Kit.launch(player, player.getLookAngle().scale(-0.8));
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.RUMBLE_THUNDER.get(), 1.4f, 1.2f);
        }
    }

    /** Pull every scrap of metal around into a giant body: bigger, armoured and crushing, it drags foes toward you. */
    public static final class PunkRotten extends Buffs.Toggle {
        PunkRotten() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, STEEL).cooldown(900).stamina(30).staminaPerTick(0.3f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("magnet_rotten_scale"), 1.2, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("magnet_rotten_armor"), 10, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("magnet_rotten_damage"), 8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("magnet_rotten_kb"), 1, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.eruption(ctx.level(), ctx.player(), ctx.player().position(), STEEL, 4f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(SoundEvents.IRON_GOLEM_REPAIR, 1.6f, 0.5f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 40 == 0) {
                attract(ctx.player(), 10 * ctx.power(), 0.7, 0f);
                Vfx.spiral(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), STEEL, 5f, Kind.SPARK);
            }
            return true;
        }
    }
}
