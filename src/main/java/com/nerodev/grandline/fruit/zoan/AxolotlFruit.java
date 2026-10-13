package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.bodies;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

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
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Sara Sara no Mi, Model: Axolotl. Become an axolotl whose body is living poison gas: toxic breath, poisonous slime,
 * clouds that drift and eat at anything inside, and an axolotl's knack for regrowing what it loses. A natural swimmer.
 */
public class AxolotlFruit extends ZoanFruit {
    public static final int COLOR = 0xF0A8C8;
    public static final int GAS = 0xB86AD8;
    static final String M = "fruit/grandline:axolotl";

    /**
     * @hybrid Axolotl-Man | Grow gills and a tail: tougher, quick to heal, and at home in water.
     * @beast Axolotl Form | Become an axolotl: low and slippery, very tough, a swift swimmer that heals fast.
     */
    static final ZoanForms FORMS = bodies("axolotl", COLOR, () -> SoundEvents.AXOLOTL_SPLASH, ZoanForms.Traits.SWIMMER,
            b -> b.modifier(Attributes.ARMOR, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(0.9f, 0.6f, 0.45f));

    public static final DeferredHolder<Ability, ToxicBreath> TOXIC_BREATH = ability("axolotl_toxic_breath", ToxicBreath::new);
    public static final DeferredHolder<Ability, Regrow> REGROW = ability("axolotl_regrow", Regrow::new);
    public static final DeferredHolder<Ability, GasBody> GAS_BODY = ability("axolotl_gas_body", GasBody::new);
    public static final DeferredHolder<Ability, SlimeSpit> SLIME_SPIT = ability("axolotl_slime_spit", SlimeSpit::new);
    public static final DeferredHolder<Ability, PoisonCloud> POISON_CLOUD = ability("axolotl_poison_cloud", PoisonCloud::new);
    public static final DeferredHolder<Ability, GiantSmiley> GIANT_SMILEY = ability("axolotl_giant_smiley", GiantSmiley::new);
    public static final DeferredHolder<Ability, ToxicBurst> TOXIC_BURST = ability("axolotl_toxic_burst", ToxicBurst::new);

    public static final ResourceLocation SLIME = GrandLine.id("axolotl_slime");
    public static final ResourceLocation CLOUD = GrandLine.id("axolotl_cloud");

    public AxolotlFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Sara Sara no Mi, Model: Axolotl", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), TOXIC_BREATH, REGROW, GAS_BODY, SLIME_SPIT, POISON_CLOUD)
                .awakened(GIANT_SMILEY, TOXIC_BURST), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SLIME, Projectiles.blast(2.5, AbilityDamage.POISON,
                (p, target) -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)), null));
        ProjectileBehavior.Registry.register(CLOUD, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.POISON, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            }
        }, null));
    }

    /** Axolotls regrow what they lose: the user heals twice as fast in either form. */
    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        super.tick(user, awakened);
        if (user.tickCount % 50 == 25 && forms().any(user) && user.getHealth() < user.getMaxHealth()) {
            user.heal(1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Hold to breathe out a stream of poison gas over everything in front of you. */
    public static final class ToxicBreath extends Ability {
        ToxicBreath() {
            super(props(ActivationType.HOLD, AbilityIcon.CLOUD, GAS).cooldown(120).stamina(10).staminaPerTick(0.3f).maxActive(60)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_ROAR);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.cone(player, 5.5 * ctx.power(), 30)) {
                    Kit.hit(player, target, AbilityDamage.POISON, 2f * FORMS.boost(player) * ctx.power());
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                }
                Vfx.beam(ctx.level(), player, player.getEyePosition().add(player.getLookAngle().scale(0.5)),
                        player.getEyePosition().add(player.getLookAngle().scale(5.5)), GAS, 1f, Kind.SMOKE, Vfx.BEAM_PARTICLES);
                ctx.sound(GLSounds.GAS_HISS.get(), 0.5f, 1.2f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Regrow your wounds: heal at once and keep healing for five seconds. */
    public static final class Regrow extends Ability {
        Regrow() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(400).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.heal(8f);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1.6f, Kind.HEART, 0);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.AXOLOTL_SPLASH, 1f, 1.4f);
        }
    }

    /** Let your body thin into gas: blows hurt less, and whatever strikes you breathes in poison. */
    public static final class GasBody extends Ability {
        GasBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, COLOR).cooldown(200).stamina(10).staminaPerTick(0.15f).maxActive(400)
                    .mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.sound(GLSounds.GAS_HISS.get(), 1f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 8 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), GAS, 1.4f, Kind.SMOKE);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.8f);
            if ((event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK))
                    && event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
                attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            }
        }
    }

    /** Spit a glob of toxic slime that bursts on impact and poisons everything around it. */
    public static final class SlimeSpit extends Ability {
        SlimeSpit() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, GAS).cooldown(60).stamina(9).mastery(M, 20).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SLIME, 1.3, 0.6f, GAS, 6f * FORMS.boost(ctx.player()), 30).withGravity(0.03);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.SLIME_SQUISH, 1f, 1.2f);
        }
    }

    /** Leave a drifting cloud of poison gas where you point: for eight seconds it poisons and sickens everything inside. */
    public static final class PoisonCloud extends Ability {
        PoisonCloud() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, GAS).cooldown(240).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, CLOUD, Kit.ground(ctx.level(), Kit.aimPoint(player, 16), 6), 6 * ctx.power(), GAS,
                    3f * ctx.power(), 160);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GAS_HISS.get(), 1.4f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Swell into a vast grinning cloud of poison around you: for ten seconds everything inside withers in it. */
    public static final class GiantSmiley extends Ability {
        GiantSmiley() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, GAS).cooldown(900).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, CLOUD);
            AbilityProjectile.zone(player, CLOUD, player.position(), 12 * ctx.power(), GAS, 4f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), GAS, 12f, Kind.SMOKE, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GAS_BOOM.get(), 1.4f, 0.6f);
        }
    }

    /** Burst your gas body outward: everything around you is blasted with poison and thrown away. */
    public static final class ToxicBurst extends Ability {
        ToxicBurst() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(600).stamina(26).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.POISON, 14f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2));
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.1).add(0, 0.5, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.8, 0), GAS, 8f, Kind.SMOKE, Vfx.RING | Vfx.FLASH | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GAS_BOOM.get(), 1.6f, 0.8f);
        }
    }
}
