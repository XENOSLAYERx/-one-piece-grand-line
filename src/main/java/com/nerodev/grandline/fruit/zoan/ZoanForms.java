package com.nerodev.grandline.fruit.zoan;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.transform.Transformation;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A Zoan fruit's two bodies, hybrid and full beast, with the toggle abilities that hold them and what each body can do
 * (fly, swim, climb). Built by {@link ZoanBatch#bodies}.
 */
public final class ZoanForms {
    /** What the transformed body can do beyond its attribute changes. */
    public record Traits(boolean beastFlight, boolean hybridFlight, boolean swim, boolean climb) {
        public static final Traits NONE = new Traits(false, false, false, false);
        public static final Traits FLYER = new Traits(true, false, false, false);
        public static final Traits WINGED = new Traits(true, true, false, false);
        public static final Traits SWIMMER = new Traits(false, false, true, false);
        public static final Traits CLIMBER = new Traits(false, false, false, true);
    }

    final String fruitId;
    final int color;
    final Supplier<SoundEvent> sound;
    final Traits traits;
    DeferredHolder<Transformation, Transformation> hybrid;
    DeferredHolder<Transformation, Transformation> beast;
    DeferredHolder<Ability, ZoanForm> hybridAbility;
    DeferredHolder<Ability, ZoanForm> beastAbility;

    ZoanForms(String fruitId, int color, Supplier<SoundEvent> sound, Traits traits) {
        this.fruitId = fruitId;
        this.color = color;
        this.sound = sound;
        this.traits = traits;
    }

    public String fruitId() {
        return fruitId;
    }

    public int color() {
        return color;
    }

    public Traits traits() {
        return traits;
    }

    public DeferredHolder<Ability, ZoanForm> hybridAbility() {
        return hybridAbility;
    }

    public DeferredHolder<Ability, ZoanForm> beastAbility() {
        return beastAbility;
    }

    public Transformation hybridForm() {
        return hybrid.get();
    }

    public Transformation beastForm() {
        return beast.get();
    }

    public boolean hybrid(ServerPlayer player) {
        return TransformationManager.isActive(player, hybrid.get());
    }

    public boolean beast(ServerPlayer player) {
        return TransformationManager.isActive(player, beast.get());
    }

    public boolean any(ServerPlayer player) {
        return hybrid(player) || beast(player);
    }

    /** How much harder a Zoan move lands in each body: 1 in human form, more as the body changes. */
    public float boost(ServerPlayer player) {
        return beast(player) ? 1.25f : hybrid(player) ? 1.15f : 1f;
    }

    public Optional<Component> needsForm(ServerPlayer player) {
        return any(player) ? Optional.empty() : Optional.of(Component.literal("Transform first"));
    }

    public Optional<Component> needsHybrid(ServerPlayer player) {
        return hybrid(player) ? Optional.empty() : Optional.of(Component.literal("Requires hybrid form"));
    }

    public Optional<Component> needsBeast(ServerPlayer player) {
        return beast(player) ? Optional.empty() : Optional.of(Component.literal("Requires full beast form"));
    }
}
