package com.nerodev.grandline.fruit.kit;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Attribute changes made by powers: toggled for as long as an ability runs ({@link Toggle}), or timed on any creature
 * ({@link #timed}). Modifiers are transient, so nothing lingers in saved data; timed ones are removed when they run
 * out or the server stops.
 */
public final class Buffs {
    /** One attribute change. The id must be unique per power and attribute. */
    public record Mod(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {}

    private record Timed(LivingEntity entity, Mod mod, long until) {}

    private static final List<Timed> TIMED = new ArrayList<>();

    private Buffs() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, e -> {
            for (Timed t : TIMED) {
                remove(t.entity(), t.mod());
            }
            TIMED.clear();
        });
    }

    public static Mod mod(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        return new Mod(attribute, id, amount, operation);
    }

    /** Adds (or refreshes) {@code mod} on {@code entity}. */
    public static void apply(LivingEntity entity, Mod mod) {
        AttributeInstance instance = entity.getAttribute(mod.attribute());
        if (instance != null) {
            instance.removeModifier(mod.id());
            instance.addTransientModifier(new AttributeModifier(mod.id(), mod.amount(), mod.operation()));
        }
    }

    public static void remove(LivingEntity entity, Mod mod) {
        AttributeInstance instance = entity.getAttribute(mod.attribute());
        if (instance != null) {
            instance.removeModifier(mod.id());
        }
    }

    public static void apply(LivingEntity entity, List<Mod> mods) {
        for (Mod mod : mods) {
            apply(entity, mod);
        }
    }

    public static void remove(LivingEntity entity, List<Mod> mods) {
        for (Mod mod : mods) {
            remove(entity, mod);
        }
    }

    /** Applies {@code mod} to {@code entity} for {@code ticks} (re-applying restarts the timer). */
    public static void timed(LivingEntity entity, Mod mod, int ticks) {
        TIMED.removeIf(t -> t.entity() == entity && t.mod().id().equals(mod.id()));
        apply(entity, mod);
        TIMED.add(new Timed(entity, mod, entity.level().getGameTime() + ticks));
    }

    private static void tick() {
        Iterator<Timed> it = TIMED.iterator();
        while (it.hasNext()) {
            Timed t = it.next();
            if (t.entity().isRemoved() || !t.entity().isAlive() || t.entity().level().getGameTime() >= t.until()) {
                remove(t.entity(), t.mod());
                it.remove();
            }
        }
    }

    /**
     * A TOGGLE (or HOLD) ability that changes the user's attributes while it runs. Override {@link #whileActive} for
     * per-tick effects and {@link #started}/{@link #stopped} for extra setup and cleanup.
     */
    public abstract static class Toggle extends Ability {
        private final List<Mod> mods;

        protected Toggle(Properties properties, List<Mod> mods) {
            super(properties);
            this.mods = mods;
        }

        @Override
        public final void onActivate(AbilityContext ctx) {
            apply(ctx.player(), mods);
            started(ctx);
        }

        @Override
        public final boolean onTick(AbilityContext ctx) {
            return whileActive(ctx);
        }

        @Override
        public final void onEnd(AbilityContext ctx, EndReason reason) {
            remove(ctx.player(), mods);
            stopped(ctx);
        }

        protected void started(AbilityContext ctx) {}

        /** @return false to stop the ability */
        protected boolean whileActive(AbilityContext ctx) {
            return true;
        }

        protected void stopped(AbilityContext ctx) {}
    }
}
