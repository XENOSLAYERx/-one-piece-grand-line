package com.nerodev.grandline.ability.rokushiki;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Soru (Shave): kick the ground so fast you seem to vanish. Blinks up to {@link #DISTANCE} blocks along the
 * horizontal look direction, stepping up single-block ledges and stopping at walls, and grants a short evade
 * window during which attacks from entities miss.
 */
public class SoruAbility extends Ability {
    public static final double DISTANCE = 8.0;
    public static final int EVADE_TICKS = 6;
    private static final double STEP = 0.25;

    public SoruAbility() {
        super(Properties.of(ActivationType.INSTANT, AbilityCategory.ROKUSHIKI)
                .icon(AbilityIcon.DASH, 0xD8E4FF)
                .cooldown(30)
                .stamina(18));
    }

    @Override
    public Optional<Component> checkUse(ServerPlayer player) {
        if (player.isPassenger()) {
            return Optional.of(Component.literal("Can't use Soru while riding"));
        }
        if (player.isFallFlying()) {
            return Optional.of(Component.literal("Can't use Soru while gliding"));
        }
        return Optional.empty();
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        Vec3 start = player.position();
        Vec3 dir = ctx.horizontalLook();
        Vec3 end = findDestination(player.serverLevel(), player, start, dir);

        ctx.fx(GLEffects.SORU_DASH, new FxParams(start, end.subtract(start), 1f, 0xD8E4FF, 0));
        ctx.animate(GLEffects.ANIM_SORU);
        ctx.sound(GLSounds.SORU.get(), 1.0f, 0.9f + player.getRandom().nextFloat() * 0.2f);

        if (end.distanceToSqr(start) > 0.01) {
            player.teleportTo(end.x, end.y, end.z);
            player.resetFallDistance();
            // Carry a little momentum out of the dash so it feels fast rather than a hard stop.
            player.setDeltaMovement(dir.scale(0.35).add(0, Math.max(0, player.getDeltaMovement().y), 0));
            player.hurtMarked = true;
        }
        ctx.state().evadeFor(AbilityManager.now(player), EVADE_TICKS);
    }

    /** Walks forward in small steps; steps up one block if blocked; stops before walls. */
    public static Vec3 findDestination(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 dir) {
        AABB box = player.getBoundingBox().move(start.subtract(player.position()));
        Vec3 current = start;
        for (double travelled = STEP; travelled <= DISTANCE; travelled += STEP) {
            Vec3 next = start.add(dir.scale(travelled)).add(0, current.y - start.y, 0);
            AABB moved = box.move(next.subtract(start));
            if (level.noCollision(player, moved)) {
                current = next;
                continue;
            }
            Vec3 stepped = next.add(0, 1.0, 0);
            if (level.noCollision(player, box.move(stepped.subtract(start)))) {
                current = stepped;
                continue;
            }
            break;
        }
        return current;
    }
}
