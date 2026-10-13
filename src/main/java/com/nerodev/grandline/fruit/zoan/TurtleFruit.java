package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.bodies;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Kame Kame no Mi. Become a turtle, as Pekoms does: a shell that almost nothing gets through, snapping jaws, and a body
 * that is slow on land but at home in the water.
 */
public class TurtleFruit extends ZoanFruit {
    public static final int COLOR = 0x7A9A5A;
    public static final int SHELL = 0x5A6A3A;
    static final String M = "fruit/grandline:turtle";

    /**
     * @hybrid Turtle-Man | Grow a turtle's shell on your back: very hard to hurt, hard to push, and a strong swimmer.
     * @beast Turtle Form | Become a great turtle: a walking fortress of shell, slow on land and quick in the water.
     */
    static final ZoanForms FORMS = bodies("turtle", COLOR, () -> SoundEvents.TURTLE_AMBIENT_LAND, ZoanForms.Traits.SWIMMER,
            b -> b.modifier(Attributes.ARMOR, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.5, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.ARMOR, 14, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR_TOUGHNESS, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 12, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, -0.25, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(1.6f, 1.1f, 0.8f));

    public static final DeferredHolder<Ability, ShellGuard> SHELL_GUARD = ability("turtle_shell_guard", ShellGuard::new);
    public static final DeferredHolder<Ability, ShellSpin> SHELL_SPIN = ability("turtle_shell_spin", ShellSpin::new);
    public static final DeferredHolder<Ability, SnappingBite> SNAPPING_BITE = ability("turtle_snapping_bite", SnappingBite::new);
    public static final DeferredHolder<Ability, ShellRocket> SHELL_ROCKET = ability("turtle_shell_rocket", ShellRocket::new);
    public static final DeferredHolder<Ability, ShellSlam> SHELL_SLAM = ability("turtle_shell_slam", ShellSlam::new);
    public static final DeferredHolder<Ability, LivingFortress> LIVING_FORTRESS = ability("turtle_living_fortress", LivingFortress::new);
    public static final DeferredHolder<Ability, TidalShell> TIDAL_SHELL = ability("turtle_tidal_shell", TidalShell::new);

    public TurtleFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Kame Kame no Mi", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), SHELL_GUARD, SHELL_SPIN, SNAPPING_BITE, SHELL_ROCKET, SHELL_SLAM)
                .awakened(LIVING_FORTRESS, TIDAL_SHELL), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    // ---------------------------------------------------------------------------------------------------------

    /** Pull into your shell: you cannot move, but blows that reach you lose nearly all their force. */
    public static final class ShellGuard extends Ability {
        ShellGuard() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, SHELL).cooldown(120).stamina(8).staminaPerTick(0.25f).maxActive(120)
                    .masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SHELL, 1.8f, Kind.SHARD, Vfx.RING);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(SoundEvents.TURTLE_EGG_CRACK, 1f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.15f);
            Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SHELL, 1.2f, Kind.SPARK);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Spin on your shell: everything around you is battered and thrown aside. Needs a form. */
    public static final class ShellSpin extends Ability {
        ShellSpin() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, SHELL).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 4 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * FORMS.boost(player) * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.4, 0.4);
            }
            Vfx.spiral(ctx.level(), player, player.position(), SHELL, 4f, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_FRENZY);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
        }
    }

    /** Snap your jaws shut on the foe in front of you: a turtle's bite does not let go easily. */
    public static final class SnappingBite extends Ability {
        SnappingBite() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(40).stamina(8).mastery(M, 10).masteryGain(0.6));
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
                ZoanKit.maul(player, target, 11f * FORMS.boost(player) * ctx.power(), 3, 0);
                Kit.root(target, Vfx.StatusLook.COILS, 30);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.TURTLE_HURT, 1.2f, 0.6f);
        }
    }

    /** Pull into your shell and shoot forward like a cannonball: everything in your path is bowled over. Needs a form. */
    public static final class ShellRocket extends Ability {
        ShellRocket() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, SHELL).cooldown(120).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 14 * ctx.power();
            ZoanKit.charge(player, length, 1.6, 13f * FORMS.boost(player) * ctx.power(), 1.6);
            Vfx.trail(ctx.level(), player, from.add(0, 0.5, 0), from.add(ctx.horizontalLook().scale(length)).add(0, 0.5, 0), SHELL, 1.2f,
                    Kind.STREAK);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.2f, 0.7f);
        }
    }

    /** Leap up and come down shell first on the spot you point at: everything there is crushed. Needs a form. */
    public static final class ShellSlam extends Ability {
        ShellSlam() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, SHELL).cooldown(200).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 16), 10);
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            ZoanKit.stomp(player, at, 4 * ctx.power(), 15f * FORMS.boost(player) * ctx.power());
            Vfx.trail(ctx.level(), player, from.add(0, 3, 0), at.add(0, 1, 0), SHELL, 1.2f, Kind.STREAK);
            Vfx.burst(ctx.level(), player, at, SHELL, 4f, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.2f, 1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Become a living fortress for twenty seconds: blows lose most of their force and attackers bounce off your shell. */
    public static final class LivingFortress extends Ability {
        LivingFortress() {
            super(props(ActivationType.TOGGLE, AbilityIcon.WALL, SHELL).cooldown(900).stamina(20).staminaPerTick(0.1f).maxActive(400)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SHELL, 2.6f, Kind.SHARD, Vfx.RING | Vfx.FLASH);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1.2f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            return FORMS.any(ctx.player());
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.35f);
            if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.distanceToSqr(ctx.player()) < 25) {
                AbilityDamage.knockback(attacker, ctx.player().position(), 1.2, 0.3);
            }
        }
    }

    /** Spin your shell through a churning wave: everything around you is swept away and soaked. Needs a form. */
    public static final class TidalShell extends Ability {
        TidalShell() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, 0x3A8AC8).cooldown(700).stamina(26).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 9 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 18f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 2.0, 0.5);
                target.clearFire();
            }
            Vfx.burst(ctx.level(), player, player.position(), 0x3A8AC8, (float) radius, Kind.DROP, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            Vfx.spiral(ctx.level(), player, player.position(), 0x8AC8F0, (float) radius, Kind.BUBBLE);
            ctx.animate(GLEffects.ANIM_FRENZY);
            ctx.sound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.6f, 0.6f);
        }
    }
}
