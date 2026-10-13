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
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Hebi Hebi no Mi, Model: King Cobra. Become a king cobra: deadly venom in fang and spit, a hood that cows foes, and
 * a hood so hot it scorches.
 */
public class KingCobraFruit extends ZoanFruit {
    public static final int COLOR = 0xA88A3A;
    public static final int VENOM = 0x6AC83A;
    static final String M = "fruit/grandline:king_cobra";

    /**
     * @hybrid Cobra-Woman | Grow a hood and a cobra's tail: stronger and tougher, with venom in your bite.
     * @beast King Cobra Form | Become a king cobra, reared up and hooded: very tough and quick to strike.
     */
    static final ZoanForms FORMS = bodies("king_cobra", COLOR, () -> SoundEvents.CAT_HISS, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 4, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ARMOR, 3, Operation.ADD_VALUE)
                    .dimensions(1.0f, 1.4f, 1.2f));

    public static final DeferredHolder<Ability, VenomFang> VENOM_FANG = ability("king_cobra_venom_fang", VenomFang::new);
    public static final DeferredHolder<Ability, VenomSpit> VENOM_SPIT = ability("king_cobra_venom_spit", VenomSpit::new);
    public static final DeferredHolder<Ability, HoodFlare> HOOD_FLARE = ability("king_cobra_hood_flare", HoodFlare::new);
    public static final DeferredHolder<Ability, ScorchingHood> SCORCHING_HOOD = ability("king_cobra_scorching_hood", ScorchingHood::new);
    public static final DeferredHolder<Ability, DeathCoil> DEATH_COIL = ability("king_cobra_death_coil", DeathCoil::new);
    public static final DeferredHolder<Ability, FireSerpent> FIRE_SERPENT = ability("king_cobra_fire_serpent", FireSerpent::new);
    public static final DeferredHolder<Ability, VenomPool> VENOM_POOL = ability("king_cobra_venom_pool", VenomPool::new);

    public static final ResourceLocation SPIT = GrandLine.id("king_cobra_spit");
    public static final ResourceLocation POOL = GrandLine.id("king_cobra_pool");

    public KingCobraFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.RARE, "Hebi Hebi no Mi, Model: King Cobra", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), VENOM_FANG, VENOM_SPIT, HOOD_FLARE, SCORCHING_HOOD, DEATH_COIL)
                .awakened(FIRE_SERPENT, VENOM_POOL), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SPIT, Projectiles.bolt(AbilityDamage.POISON, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        }));
        ProjectileBehavior.Registry.register(POOL, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.POISON, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
            }
        }, null));
    }

    static void envenom(LivingEntity target, int ticks, int level) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, level));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Sink venomous fangs into whatever is in front of you. Needs a form. */
    public static final class VenomFang extends Ability {
        VenomFang() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, VENOM).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.6, 40)) {
                ZoanKit.maul(player, target, 7f * FORMS.boost(player) * ctx.power(), 0, 0);
                envenom(target, 120, 2);
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.3)), VENOM, 1.2f, Kind.DROP);
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.CAT_HISS, 1f, 1.2f);
        }
    }

    /** Spit venom at a foe: it poisons and blinds. */
    public static final class VenomSpit extends Ability {
        VenomSpit() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, VENOM).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SPIT, 1.6, 0.35f, VENOM, 5f * FORMS.boost(ctx.player()), 25);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.LLAMA_SPIT, 1f, 0.8f);
        }
    }

    /** Spread your hood and rear up: foes in front of you lose their nerve, weakened and slowed. */
    public static final class HoodFlare extends Ability {
        HoodFlare() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(240).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 8, 50)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), COLOR, 3f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.CAT_HISS, 1.4f, 0.6f);
        }
    }

    /** Heat your hood until it glows and lash it at everything in front of you: they burn. */
    public static final class ScorchingHood extends Ability {
        ScorchingHood() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, 0xF08A3A).cooldown(120).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 6, 45)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 10f * FORMS.boost(player) * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(player.getLookAngle().scale(2)), 0xF08A3A, 2.5f, Kind.FLAME, 0);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.FIRE_FIST.get(), 0.8f, 1.2f);
        }
    }

    /** Coil around a foe and bite deep: it is held fast as venom and decay set in. Needs a form. */
    public static final class DeathCoil extends Ability {
        DeathCoil() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, VENOM).cooldown(220).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 6).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.STRINGS, 60);
                envenom(target, 160, 2);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                Kit.hit(player, target, AbilityDamage.POISON, 6f * ctx.power());
            });
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(SoundEvents.CAT_HISS, 1.4f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Loose a serpent of fire from your burning hood: it scorches everything in a long cone. */
    public static final class FireSerpent extends Ability {
        FireSerpent() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, COLOR).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 12 * ctx.power(), 30)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 16f * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 160));
            }
            for (int i = 1; i <= 4; i++) {
                Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(i * 3)).add(ctx.right().scale(Math.sin(i) * 0.8)),
                        0xF08A3A, 1.2f + i * 0.5f, Kind.FLAME, 0);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 1.4f, 0.9f);
        }
    }

    /** Pour out a pool of venom around you: for eight seconds, everything standing in it is poisoned and hurt. */
    public static final class VenomPool extends Ability {
        VenomPool() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, VENOM).cooldown(700).stamina(26).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, POOL);
            AbilityProjectile.zone(player, POOL, player.position(), 8 * ctx.power(), VENOM, 3f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, player.position(), VENOM, 8f, Kind.DROP, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BREWING_STAND_BREW, 1.4f, 0.6f);
        }
    }
}
