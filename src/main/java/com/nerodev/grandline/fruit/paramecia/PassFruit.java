package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Nuke Nuke no Mi. Pass through walls and floors, and let blows pass through you: step through stone, sink to the
 * floor below, and reach through armour to strike the body beneath.
 */
public class PassFruit extends DevilFruit {
    public static final int COLOR = 0xA8A8C8;
    public static final int DEEP = 0x5A5A8A;
    static final String M = "fruit/grandline:pass";

    public static final DeferredHolder<Ability, PassThrough> PASS_THROUGH = ability("pass_through", PassThrough::new);
    public static final DeferredHolder<Ability, Intangible> INTANGIBLE = ability("pass_intangible", Intangible::new);
    public static final DeferredHolder<Ability, SinkThrough> SINK_THROUGH = ability("pass_sink_through", SinkThrough::new);
    public static final DeferredHolder<Ability, PassStrike> PASS_STRIKE = ability("pass_strike", PassStrike::new);
    public static final DeferredHolder<Ability, HalfSink> HALF_SINK = ability("pass_half_sink", HalfSink::new);
    public static final DeferredHolder<Ability, Phantom> PHANTOM = ability("pass_phantom", Phantom::new);
    public static final DeferredHolder<Ability, ThroughAndThrough> THROUGH_AND_THROUGH = ability("pass_through_and_through",
            ThroughAndThrough::new);

    public PassFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Nuke Nuke no Mi", COLOR)
                .abilities(PASS_THROUGH, INTANGIBLE, SINK_THROUGH, PASS_STRIKE, HALF_SINK)
                .awakened(PHANTOM, THROUGH_AND_THROUGH));
    }

    static void init() {}

    static void behaviors() {}

    /** The first open space below the floor the user stands on (with ground under it), up to twelve blocks down. */
    @Nullable
    static Vec3 below(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int d = 2; d <= 12; d++) {
            Vec3 to = player.position().subtract(0, d, 0);
            if (level.noCollision(player, player.getBoundingBox().move(0, -d, 0))) {
                BlockPos floor = BlockPos.containing(to).below();
                if (!level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) {
                    return to;
                }
            }
        }
        return null;
    }

    static void move(ServerPlayer player, Vec3 to) {
        Vec3 from = player.position();
        player.teleportTo(to.x, to.y, to.z);
        player.resetFallDistance();
        Vfx.blink(player.serverLevel(), player, from, to, COLOR, Kind.RING);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Walk straight through the wall in front of you, however thick (up to eight blocks).
     *
     * @name Pass Through
     */
    public static final class PassThrough extends Ability {
        PassThrough() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(50).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return DoorFruit.farthestFree(player, player.position(), player.getLookAngle().multiply(1, 0, 1).normalize(), 8) == null
                    ? Optional.of(Component.literal("Nowhere to come out")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vec3 to = DoorFruit.farthestFree(ctx.player(), ctx.player().position(), ctx.horizontalLook(), 8);
            if (to != null) {
                move(ctx.player(), to);
            }
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(GLSounds.EVADE.get(), 1f, 0.7f);
        }
    }

    /** Hold to let everything pass through you: blows and shots go straight through, though you can barely move. */
    public static final class Intangible extends Ability {
        Intangible() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, DEEP).cooldown(160).stamina(10).staminaPerTick(0.35f).maxActive(60)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 2, false, false));
            if (ctx.activeTicks() % 4 == 0) {
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.RING);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            if (event.getSource().getEntity() != null || event.getSource().getDirectEntity() != null) {
                event.setCanceled(true);
            }
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Sink through the floor and drop into the open space beneath it. */
    public static final class SinkThrough extends Ability {
        SinkThrough() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, COLOR).cooldown(80).stamina(8).mastery(M, 10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return below(player) == null ? Optional.of(Component.literal("No open space below")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vec3 to = below(ctx.player());
            if (to != null) {
                move(ctx.player(), to);
            }
            ctx.sound(GLSounds.EVADE.get(), 1f, 0.5f);
        }
    }

    /**
     * Reach through a foe's armour and strike the body beneath: armour does nothing against it.
     *
     * @name Pass Strike
     */
    public static final class PassStrike extends Ability {
        PassStrike() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, DEEP).cooldown(60).stamina(10).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 6).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.DARK, 10f * ctx.power());
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), DEEP, 1.4f, Kind.RING);
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 0.8f, 1.4f);
        }
    }

    /** Push a foe halfway into the floor: it is stuck to the waist and squeezed. */
    public static final class HalfSink extends Ability {
        HalfSink() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(200).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.STONE, 80);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                Kit.hit(player, target, AbilityDamage.CRUSH, 4f * ctx.power());
                Vfx.burst(ctx.level(), player, target.position(), COLOR, 1.4f, Kind.RING, Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.STONE_PLACE, 1.4f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Become a phantom for ten seconds: unseen, and every attack passes straight through you. */
    public static final class Phantom extends Ability {
        Phantom() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, DEEP).cooldown(1000).stamina(26).staminaPerTick(0.4f).maxActive(200)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 2f, Kind.RING, Vfx.FLASH);
            ctx.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ctx.player().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            if (event.getSource().getEntity() != null || event.getSource().getDirectEntity() != null) {
                event.setCanceled(true);
            }
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ctx.player().removeEffect(MobEffects.INVISIBILITY);
        }
    }

    /**
     * Rush straight through everything ahead, foes and walls alike: those you pass through are struck from inside.
     *
     * @name Through and Through
     */
    public static final class ThroughAndThrough extends Ability {
        ThroughAndThrough() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 12, 1.4)) {
                Kit.hit(player, target, AbilityDamage.DARK, 14f * ctx.power());
            }
            Vec3 to = DoorFruit.farthestFree(player, player.position(), ctx.horizontalLook(), 12);
            if (to != null) {
                Vfx.trail(ctx.level(), player, player.position().add(0, 1, 0), to.add(0, 1, 0), COLOR, 1.4f, Kind.RING);
                move(player, to);
            }
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(GLSounds.SORU.get(), 1.2f, 0.7f);
        }
    }
}
