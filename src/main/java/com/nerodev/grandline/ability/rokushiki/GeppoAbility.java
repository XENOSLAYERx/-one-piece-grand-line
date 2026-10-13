package com.nerodev.grandline.ability.rokushiki;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Geppo (Moon Walk): kick off the air itself. Only works while airborne; up to {@link #MAX_AIR_JUMPS} kicks before
 * you have to touch the ground again. Each kick cancels fall damage built up so far.
 */
public class GeppoAbility extends Ability {
    public static final int MAX_AIR_JUMPS = 3;
    private static final double UP_SPEED = 0.72;
    private static final double FORWARD_PUSH = 0.25;

    public GeppoAbility() {
        super(Properties.of(ActivationType.INSTANT, AbilityCategory.ROKUSHIKI)
                .icon(AbilityIcon.JUMP, 0xBFE8FF)
                .cooldown(6)
                .stamina(12));
    }

    @Override
    public Optional<Component> checkUse(ServerPlayer player) {
        if (player.onGround()) {
            return Optional.of(Component.literal("Geppo only works in the air"));
        }
        if (player.isInWater() || player.isInLava()) {
            return Optional.of(Component.literal("Can't kick off liquid"));
        }
        if (AbilityState.of(player).airJumps() >= MAX_AIR_JUMPS) {
            return Optional.of(Component.literal("Land before using Geppo again"));
        }
        return Optional.empty();
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        AbilityState state = ctx.state();
        state.setAirJumps(state.airJumps() + 1);

        Vec3 push = ctx.horizontalLook().scale(FORWARD_PUSH);
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.6 + push.x, UP_SPEED, motion.z * 0.6 + push.z);
        player.resetFallDistance();
        player.hurtMarked = true;

        ctx.fx(GLEffects.GEPPO_STEP, FxParams.at(player.position()).color(0xE6F4FF));
        ctx.animate(GLEffects.ANIM_GEPPO);
        ctx.sound(GLSounds.GEPPO.get(), 0.8f, 0.95f + state.airJumps() * 0.08f);
    }
}
