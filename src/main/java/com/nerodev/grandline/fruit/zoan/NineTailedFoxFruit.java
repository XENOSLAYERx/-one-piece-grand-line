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
import com.nerodev.grandline.fruit.kit.Disguise;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Inu Inu no Mi, Model: Kyubi no Kitsune. Become a nine-tailed fox, as Catarina Devon does: a trickster that takes the
 * exact shape of anyone it has seen, bewitches its prey, and burns it with will-o'-the-wisp fox fire.
 */
public class NineTailedFoxFruit extends ZoanFruit {
    public static final int COLOR = 0xE8B060;
    public static final int FOXFIRE = 0x6AD8F0;
    static final String M = "fruit/grandline:nine_tailed_fox";

    /**
     * @hybrid Fox-Woman | Grow fox ears and nine tails: quicker, stronger and full of tricks.
     * @beast Nine-Tailed Fox Form | Become a nine-tailed fox: very fast, and its tails strike like whips.
     */
    static final ZoanForms FORMS = bodies("nine_tailed_fox", COLOR, () -> SoundEvents.FOX_SCREECH, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.4, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.2, Operation.ADD_VALUE)
                    .dimensions(1.0f, 1.2f, 1.0f));

    public static final DeferredHolder<Ability, FoxFire> FOX_FIRE = ability("nine_tailed_fox_fox_fire", FoxFire::new);
    public static final DeferredHolder<Ability, Mimicry> MIMICRY = ability("nine_tailed_fox_mimicry", Mimicry::new);
    public static final DeferredHolder<Ability, TailLash> TAIL_LASH = ability("nine_tailed_fox_tail_lash", TailLash::new);
    public static final DeferredHolder<Ability, Bewitch> BEWITCH = ability("nine_tailed_fox_bewitch", Bewitch::new);
    public static final DeferredHolder<Ability, FoxPounce> FOX_POUNCE = ability("nine_tailed_fox_pounce", FoxPounce::new);
    public static final DeferredHolder<Ability, NineFoxFires> NINE_FOX_FIRES = ability("nine_tailed_fox_nine_fires", NineFoxFires::new);
    public static final DeferredHolder<Ability, PerfectMimicry> PERFECT_MIMICRY = ability("nine_tailed_fox_perfect_mimicry",
            PerfectMimicry::new);

    public static final ResourceLocation WISP = GrandLine.id("nine_tailed_fox_wisp");

    public NineTailedFoxFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Inu Inu no Mi, Model: Kyubi no Kitsune", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), FOX_FIRE, MIMICRY, TAIL_LASH, BEWITCH, FOX_POUNCE)
                .awakened(NINE_FOX_FIRES, PERFECT_MIMICRY), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(WISP, Projectiles.homing(16, 0.25, Projectiles.bolt(AbilityDamage.FIRE,
                (p, target) -> target.igniteForSeconds(3))));
    }

    @Override
    public void onLost(ServerPlayer player) {
        super.onLost(player);
        Disguise.clear(player);
    }

    /** Fires {@code count} wisps of fox fire in a fan; each one hunts the nearest foe. */
    static void wisps(AbilityContext ctx, int count, float damage) {
        ServerPlayer player = ctx.player();
        Vec3 look = player.getLookAngle();
        for (int i = 0; i < count; i++) {
            double spread = count == 1 ? 0 : (i - (count - 1) / 2.0) * (1.2 / count);
            Vec3 dir = look.yRot((float) spread).add(0, 0.15, 0).normalize();
            AbilityProjectile.spawn(player, WISP, player.getEyePosition().add(dir), dir.scale(0.9), 0.5f, FOXFIRE, damage * ctx.power(), 80)
                    .withPower(ctx.power());
        }
    }

    /** Takes the shape of {@code target}: its face if it is a person, its kind if it is a creature. */
    static void copy(ServerPlayer player, LivingEntity target) {
        if (target instanceof Player other) {
            Disguise.asPlayer(player, other.getUUID());
        } else {
            Disguise.as(player, target.getType());
        }
    }

    /** Monsters hunting the user lose the scent, unless it has just struck them ({@code always}: even then). */
    static void shake(AbilityContext ctx, boolean always) {
        ServerPlayer player = ctx.player();
        for (Mob hunter : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16),
                m -> m.getTarget() == player && (always || m.getLastHurtByMob() != player))) {
            hunter.setTarget(null);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Loose three wisps of fox fire that hunt down the nearest foes and burn them. */
    public static final class FoxFire extends Ability {
        FoxFire() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, FOXFIRE).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            wisps(ctx, 3, 6f * FORMS.boost(ctx.player()));
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 0.8f, 1.6f);
        }
    }

    /**
     * Take the exact shape of the creature or person you are looking at (a villager if no one) for up to a minute.
     * Monsters lose interest in you until you strike one.
     */
    public static final class Mimicry extends Ability {
        Mimicry() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(300).stamina(12).staminaPerTick(0.05f).maxActive(1200)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Optional<LivingEntity> target = Kit.target(player, 24);
            if (target.isPresent()) {
                copy(player, target.get());
            } else {
                Disguise.as(player, EntityType.VILLAGER);
            }
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1.8f, Kind.SMOKE, Vfx.FLASH);
            ctx.sound(SoundEvents.FOX_AMBIENT, 1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                shake(ctx, false);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Disguise.clear(ctx.player());
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.4f, Kind.SMOKE, 0);
        }
    }

    /** Whip all nine tails around you: every foe nearby is struck three times and thrown aside. Needs a form. */
    public static final class TailLash extends Ability {
        TailLash() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, COLOR).cooldown(80).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 5 * ctx.power())) {
                for (int i = 0; i < 3; i++) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 4f * FORMS.boost(player) * ctx.power());
                }
                AbilityDamage.knockback(target, player.position(), 1.3, 0.35);
            }
            Vfx.spiral(ctx.level(), player, player.position(), COLOR, 5f, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.9f);
        }
    }

    /** Bewitch everything in front of you: foes are dazed, blinded and weakened, and monsters forget who they hunted. */
    public static final class Bewitch extends Ability {
        Bewitch() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, FOXFIRE).cooldown(240).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 10 * ctx.power(), 60)) {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
                Vfx.impact(ctx.level(), player, target.getEyePosition(), FOXFIRE, 1f, Kind.STAR);
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(3)), FOXFIRE, 4f, Kind.GLOW, Vfx.RING);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 1f, 1.2f);
        }
    }

    /**
     * Pounce on the foe you face from up to fourteen blocks away, landing behind it with your fangs in its neck.
     *
     * @name Fox Pounce
     */
    public static final class FoxPounce extends Ability {
        FoxPounce() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(160).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No prey in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 behind = target.position().add(ctx.horizontalLook().scale(1.2));
                player.teleportTo(behind.x, target.getY(), behind.z);
                player.resetFallDistance();
                ZoanKit.maul(player, target, 16f * FORMS.boost(player) * ctx.power(), 4, 1);
                Vfx.trail(ctx.level(), player, from.add(0, 0.8, 0), behind.add(0, 0.8, 0), COLOR, 1f, Kind.STREAK);
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.FOX_SCREECH, 1f, 1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * One wisp of fox fire for each tail: nine of them, each hunting its own foe.
     *
     * @name Nine Fox Fires
     */
    public static final class NineFoxFires extends Ability {
        NineFoxFires() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, FOXFIRE).cooldown(500).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            wisps(ctx, 9, 9f);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), FOXFIRE, 2.4f, Kind.GLOW, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 1.4f, 1.2f);
        }
    }

    /**
     * The perfect copy: become whoever you look at for two minutes, so convincingly that monsters ignore you even
     * after you strike them, and you move faster.
     */
    public static final class PerfectMimicry extends Ability {
        PerfectMimicry() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, FOXFIRE).cooldown(1200).stamina(20).staminaPerTick(0.05f)
                    .maxActive(2400).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Optional<LivingEntity> target = Kit.target(player, 32);
            if (target.isPresent()) {
                copy(player, target.get());
            } else {
                Disguise.as(player, EntityType.VILLAGER);
            }
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), FOXFIRE, 2.4f, Kind.GLOW, Vfx.FLASH | Vfx.RING);
            ctx.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 1f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 5 == 0) {
                shake(ctx, true);
                ctx.player().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 1, false, false));
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Disguise.clear(ctx.player());
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), FOXFIRE, 1.6f, Kind.GLOW, 0);
        }
    }
}
