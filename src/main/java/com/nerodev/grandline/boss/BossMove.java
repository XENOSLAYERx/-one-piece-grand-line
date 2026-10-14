package com.nerodev.grandline.boss;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;

/**
 * One thing a boss can do in a fight: a telegraphed windup, an active stretch where it lands, and a recovery the
 * player can punish. The timings are shared with the client, which poses the boss from them.
 *
 * @param id         name used by boss definitions and synced to clients
 * @param windup     ticks of telegraph before it lands
 * @param active     ticks it is landing (a swing's instant, a charge's run, a spin's whirl)
 * @param recovery   ticks the boss is left open afterwards
 * @param cooldown   ticks before the boss may use it again
 * @param minRange   closest the target may be to start it
 * @param maxRange   farthest the target may be to start it
 * @param shield     whether the boss takes no damage while doing it (roars between phases)
 */
public record BossMove(String id, int windup, int active, int recovery, int cooldown, double minRange, double maxRange, boolean shield,
        Action action) {
    /** What the move does on each tick, {@code t} counting from the start of the windup. */
    @FunctionalInterface
    public interface Action {
        void tick(BossEntity boss, @Nullable LivingEntity target, int t);
    }

    public int length() {
        return windup + active + recovery;
    }

    public boolean inRange(double distance) {
        return distance >= minRange && distance <= maxRange;
    }

    /** Whether tick {@code t} falls in the active stretch. */
    public boolean activeAt(int t) {
        return t >= windup && t < windup + active;
    }
}
