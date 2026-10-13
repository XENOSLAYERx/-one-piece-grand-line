package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

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
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Kira Kira no Mi. Turn your body to diamond, the hardest thing there is: blades glance off, and a diamond fist hits
 * like a cannon.
 */
public class SparkleFruit extends DevilFruit {
    public static final int COLOR = 0xE0F8FF;
    public static final int BLUE = 0x8AD0F0;
    static final String M = "fruit/grandline:sparkle";

    public static final DeferredHolder<Ability, DiamondFist> DIAMOND_FIST = ability("sparkle_diamond_fist", DiamondFist::new);
    public static final DeferredHolder<Ability, DiamondBody> DIAMOND_BODY = ability("sparkle_diamond_body", DiamondBody::new);
    public static final DeferredHolder<Ability, BrilliantPunk> BRILLIANT_PUNK = ability("sparkle_brilliant_punk", BrilliantPunk::new);
    public static final DeferredHolder<Ability, DiamondGuard> DIAMOND_GUARD = ability("sparkle_diamond_guard", DiamondGuard::new);
    public static final DeferredHolder<Ability, DiamondShards> DIAMOND_SHARDS = ability("sparkle_diamond_shards", DiamondShards::new);
    public static final DeferredHolder<Ability, BoulderThrow> BOULDER_THROW = ability("sparkle_boulder_throw", BoulderThrow::new);
    public static final DeferredHolder<Ability, PrismFlash> PRISM_FLASH = ability("sparkle_prism_flash", PrismFlash::new);

    public static final ResourceLocation SHARD = GrandLine.id("sparkle_shard");
    public static final ResourceLocation BOULDER = GrandLine.id("sparkle_boulder");

    public SparkleFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Kira Kira no Mi", COLOR)
                .abilities(DIAMOND_FIST, DIAMOND_BODY, BRILLIANT_PUNK, DIAMOND_GUARD, DIAMOND_SHARDS)
                .awakened(BOULDER_THROW, PRISM_FLASH));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SHARD, Projectiles.piercing(AbilityDamage.SLASH, null));
        ProjectileBehavior.Registry.register(BOULDER, Projectiles.blast(4, AbilityDamage.BLAST,
                (p, target) -> Kit.launch(target, target.position().subtract(p.position()).normalize().scale(1.2).add(0, 0.5, 0)),
                null));
    }

    /** Diamond skin: blades and arrows glance off. In Diamond Guard, almost nothing gets through and attackers bruise. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE) || event.getSource().is(AbilityDamage.SHARP)) {
            event.setAmount(event.getAmount() * 0.7f);
        }
        if (AbilityState.of(user).isActive(DIAMOND_GUARD.getId())) {
            event.setAmount(event.getAmount() * 0.2f);
            if ((event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK))
                    && event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
                Kit.hit(user, attacker, AbilityDamage.STRIKE, 3f);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Punch with a fist of diamond: it hits like a cannonball and throws the foe back. */
    public static final class DiamondFist extends Ability {
        DiamondFist() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.8, 40)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.1).add(0, 0.3, 0));
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.2)), COLOR, 1.4f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.4f, 0.8f);
        }
    }

    /** Turn your skin to diamond: far tougher, and hard to knock back. */
    public static final class DiamondBody extends Buffs.Toggle {
        DiamondBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, BLUE).cooldown(160).stamina(10).staminaPerTick(0.12f)
                            .maxActive(800).masteryGain(0.7),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("sparkle_body_armor"), 12, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("sparkle_body_toughness"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("sparkle_body_kb"), 0.6, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.6f, Kind.STAR, Vfx.FLASH);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.2f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 12 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 0.8f, Kind.STAR);
            }
            return true;
        }
    }

    /** Charge in shoulder-first, diamond-hard, and crash through the first foe in your way. */
    public static final class BrilliantPunk extends Ability {
        BrilliantPunk() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(120).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            for (LivingEntity target : AbilityDamage.line(player, 8 * ctx.power(), 1.2)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 14f * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.6).add(0, 0.5, 0));
                break;
            }
            Kit.dash(player, 2.0, 0.1);
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), from.add(ctx.horizontalLook().scale(8)).add(0, 1, 0), COLOR, 1f, Kind.STAR);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_BREAK, 1.6f, 0.6f);
        }
    }

    /** Hold to brace in solid diamond: almost no damage gets through, and anyone who strikes you bruises their hands. */
    public static final class DiamondGuard extends Ability {
        DiamondGuard() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, COLOR).cooldown(160).stamina(10).staminaPerTick(0.3f).maxActive(80)
                    .mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 3, false, false));
            if (ctx.activeTicks() % 6 == 0) {
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), BLUE, 1.2f, Kind.STAR);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Flick a spray of razor-sharp diamond shards that cut through everything in their path. */
    public static final class DiamondShards extends Ability {
        DiamondShards() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, BLUE).cooldown(80).stamina(12).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.08)).normalize();
                AbilityProjectile.spawn(player, SHARD, ctx.handPos(), dir.scale(2.0), 0.3f, COLOR, 5f * ctx.power(), 20);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Tear out a boulder of diamond and hurl it: it lands like a meteor and scatters everything around. */
    public static final class BoulderThrow extends Ability {
        BoulderThrow() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.shoot(ctx, BOULDER, 1.2, 2.4f, COLOR, 18f, 60).withGravity(0.04);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_BREAK, 2f, 0.4f);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 2f, Kind.SHARD, Vfx.GROUND);
        }
    }

    /** Catch the light in your diamond body and flash it out: everyone around is blinded and seared. */
    public static final class PrismFlash extends Ability {
        PrismFlash() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, BLUE).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.LIGHT, 10f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
            }
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 10f, Kind.STAR, Vfx.FLASH | Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GLINT_FLASH.get(), 1.6f, 1f);
        }
    }
}
