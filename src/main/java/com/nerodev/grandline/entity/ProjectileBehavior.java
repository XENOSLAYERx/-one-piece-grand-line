package com.nerodev.grandline.entity;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Server-side behaviour of an {@link AbilityProjectile} kind (what happens on hit, every tick, on expiry). The
 * matching client look is registered separately by kind id.
 */
public interface ProjectileBehavior {
    /** Called every server tick before movement. */
    default void tick(AbilityProjectile projectile) {}

    /** Hit an entity. Return true to keep flying (piercing). */
    boolean onHitEntity(AbilityProjectile projectile, Entity target);

    /** Hit a block. The projectile is removed afterwards. */
    default void onHitBlock(AbilityProjectile projectile, BlockHitResult hit) {}

    /** Ran out of lifetime. */
    default void onExpire(AbilityProjectile projectile) {}

    /** Whether the projectile collides with blocks at all. */
    default boolean hitsBlocks() {
        return true;
    }

    /** Registry of behaviours by kind id. */
    final class Registry {
        private static final Map<ResourceLocation, ProjectileBehavior> BEHAVIORS = new HashMap<>();

        private Registry() {}

        public static void register(ResourceLocation kind, ProjectileBehavior behavior) {
            BEHAVIORS.put(kind, behavior);
        }

        @Nullable
        public static ProjectileBehavior get(ResourceLocation kind) {
            return BEHAVIORS.get(kind);
        }
    }
}
