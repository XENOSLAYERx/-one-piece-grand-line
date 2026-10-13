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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 * Inu Inu no Mi, Model: Dachshund. Become a dachshund, as the gun Lassoo did when Mr. 4 fed it the fruit: a keen
 * nose, quick little legs, and sneezes that fire like a cannon and burst on whatever they hit.
 */
public class DachshundFruit extends ZoanFruit {
    public static final int COLOR = 0xA86A3A;
    public static final int DARK = 0x3A2A1A;
    public static final int BALL = 0x4A4A52;
    static final String M = "fruit/grandline:dachshund";

    /**
     * @hybrid Dachshund-Man | Grow a dachshund's ears and tail: quicker, with a nose for every foe nearby.
     * @beast Dachshund Form | Become a dachshund: small, very fast and hard to hit.
     */
    static final ZoanForms FORMS = bodies("dachshund", COLOR, () -> SoundEvents.WOLF_AMBIENT, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 4, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.35, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2, Operation.ADD_VALUE)
                    .dimensions(0.8f, 0.7f, 0.55f));

    public static final DeferredHolder<Ability, SneezeShot> SNEEZE_SHOT = ability("dachshund_sneeze_shot", SneezeShot::new);
    public static final DeferredHolder<Ability, AnkleBite> ANKLE_BITE = ability("dachshund_ankle_bite", AnkleBite::new);
    public static final DeferredHolder<Ability, KeenNose> KEEN_NOSE = ability("dachshund_keen_nose", KeenNose::new);
    public static final DeferredHolder<Ability, RapidSneezes> RAPID_SNEEZES = ability("dachshund_rapid_sneezes", RapidSneezes::new);
    public static final DeferredHolder<Ability, BigSneeze> BIG_SNEEZE = ability("dachshund_big_sneeze", BigSneeze::new);
    public static final DeferredHolder<Ability, CannonBarrage> CANNON_BARRAGE = ability("dachshund_cannon_barrage", CannonBarrage::new);
    public static final DeferredHolder<Ability, Bloodhound> BLOODHOUND = ability("dachshund_bloodhound", Bloodhound::new);

    public static final ResourceLocation CANNONBALL = GrandLine.id("dachshund_cannonball");
    public static final ResourceLocation BIG_BALL = GrandLine.id("dachshund_big_cannonball");

    public DachshundFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Inu Inu no Mi, Model: Dachshund", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), SNEEZE_SHOT, ANKLE_BITE, KEEN_NOSE, RAPID_SNEEZES, BIG_SNEEZE)
                .awakened(CANNON_BARRAGE, BLOODHOUND), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(CANNONBALL, Projectiles.blast(2.5, AbilityDamage.BLAST, null,
                p -> Vfx.burst((ServerLevel) p.level(), p, p.position(), 0xF0A040, 2.5f, Kind.SMOKE, Vfx.FLASH)));
        ProjectileBehavior.Registry.register(BIG_BALL, Projectiles.blast(5, AbilityDamage.BLAST, null,
                p -> Vfx.burst((ServerLevel) p.level(), p, p.position(), 0xF0A040, 5f, Kind.SMOKE, Vfx.RING | Vfx.FLASH | Vfx.HEAVY)));
    }

    /** Sneezes a cannonball from the user's mouth along {@code dir}. */
    static void sneeze(AbilityContext ctx, ResourceLocation kind, Vec3 dir, float size, float damage) {
        ServerPlayer player = ctx.player();
        AbilityProjectile.spawn(player, kind, player.getEyePosition().add(dir.scale(0.6)), dir.scale(1.4), size, BALL, damage * ctx.power(), 50)
                .withPower(ctx.power());
        Vfx.impact(ctx.level(), player, player.getEyePosition().add(dir), 0xD8D0C8, size * 1.5f, Kind.SMOKE);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Sneeze a cannonball: it bursts on whatever it hits. Needs a form. */
    public static final class SneezeShot extends Ability {
        SneezeShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, BALL).cooldown(45).stamina(9).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            sneeze(ctx, CANNONBALL, ctx.player().getLookAngle(), 0.5f, 10f * FORMS.boost(ctx.player()));
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.6f);
        }
    }

    /** Dart in and bite at the legs of the foe in front of you: it bleeds and stumbles. Needs a form. */
    public static final class AnkleBite extends Ability {
        AnkleBite() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(30).stamina(6).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4).ifPresent(target -> {
                ZoanKit.maul(player, target, 8f * FORMS.boost(player) * ctx.power(), 3, 0);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.WOLF_GROWL, 1f, 1.4f);
        }
    }

    /** Sniff the air: every creature within thirty-two blocks glows through walls for fifteen seconds. */
    public static final class KeenNose extends Ability {
        KeenNose() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(300).stamina(8).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 32)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), COLOR, 2f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.WOLF_AMBIENT, 1f, 1.5f);
        }
    }

    /** Sneeze three times in a row: three cannonballs in a spread. Needs a form. */
    public static final class RapidSneezes extends Ability {
        RapidSneezes() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, BALL).cooldown(120).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vec3 look = ctx.player().getLookAngle();
            for (int i = -1; i <= 1; i++) {
                sneeze(ctx, CANNONBALL, look.yRot(i * 0.15f), 0.5f, 9f * FORMS.boost(ctx.player()));
            }
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.4f);
        }
    }

    /** Hold it in, then let out an enormous sneeze: a huge cannonball with a huge burst. Needs a form. */
    public static final class BigSneeze extends Ability {
        BigSneeze() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, BALL).cooldown(240).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            sneeze(ctx, BIG_BALL, ctx.player().getLookAngle(), 1.2f, 18f * FORMS.boost(ctx.player()));
            Kit.launch(ctx.player(), ctx.player().getLookAngle().scale(-0.6).add(0, 0.2, 0));
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.9f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A fit of sneezing: seven cannonballs in a wide fan, each bursting where it lands. */
    public static final class CannonBarrage extends Ability {
        CannonBarrage() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, 0xF0A040).cooldown(600).stamina(26).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vec3 look = ctx.player().getLookAngle();
            for (int i = -3; i <= 3; i++) {
                sneeze(ctx, CANNONBALL, look.yRot(i * 0.12f).add(0, 0.05 * Math.abs(i), 0).normalize(), 0.6f, 12f);
            }
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 1.2f);
        }
    }

    /** Hunt like a bloodhound for twenty seconds: you run and bite far harder, and every foe near you glows. */
    public static final class Bloodhound extends Ability {
        Bloodhound() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, DARK).cooldown(900).stamina(18).staminaPerTick(0.08f).maxActive(400)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), DARK, 2f, Kind.SMOKE, Vfx.RING);
            ctx.sound(SoundEvents.WOLF_HOWL, 1f, 1.4f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!FORMS.any(player)) {
                return false;
            }
            if (ctx.activeTicks() % 10 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 2, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 1, false, false));
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 24)) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 0));
                }
            }
            return true;
        }
    }
}
