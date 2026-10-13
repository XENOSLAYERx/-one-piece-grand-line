package com.nerodev.grandline.transform;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * A body state such as Gear Second or a Zoan form. At most one {@link Group#BODY} transformation can be active, so
 * two body changes never stack; {@link TransformationManager} enforces that and syncs the state for rendering.
 */
public class Transformation {
    public enum Group {
        /** Changes the body itself (Zoan forms, Gears). Mutually exclusive. */
        BODY
    }

    public record Modifier(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {}

    private final Group group;
    private final List<Modifier> modifiers;
    @Nullable
    private final EntityDimensions dimensions;
    @Nullable
    private final Supplier<? extends Ability> source;

    protected Transformation(Builder builder) {
        this.group = builder.group;
        this.modifiers = List.copyOf(builder.modifiers);
        this.dimensions = builder.dimensions;
        this.source = builder.source;
    }

    public final ResourceLocation id() {
        return Objects.requireNonNull(GLTransformations.REGISTRY.getKey(this), "Transformation is not registered");
    }

    public Group group() {
        return group;
    }

    public List<Modifier> modifiers() {
        return modifiers;
    }

    /** Standing hitbox while transformed, or null to keep the player's. */
    @Nullable
    public EntityDimensions dimensions() {
        return dimensions;
    }

    /** The toggle ability that keeps this transformation running, if any. Ended together with it. */
    @Nullable
    public Ability sourceAbility() {
        return source == null ? null : source.get();
    }

    ResourceLocation modifierId(int index) {
        ResourceLocation id = id();
        return GrandLine.id("transform/" + id.getNamespace() + "/" + id.getPath() + "/" + index);
    }

    public void onStart(ServerPlayer player) {}

    public void onEnd(ServerPlayer player) {}

    public static Builder builder(Group group) {
        return new Builder(group);
    }

    public static final class Builder {
        private final Group group;
        private final List<Modifier> modifiers = new ArrayList<>();
        @Nullable
        private EntityDimensions dimensions;
        @Nullable
        private Supplier<? extends Ability> source;

        private Builder(Group group) {
            this.group = group;
        }

        public Builder modifier(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
            modifiers.add(new Modifier(attribute, amount, operation));
            return this;
        }

        public Builder dimensions(float width, float height, float eyeHeight) {
            this.dimensions = EntityDimensions.scalable(width, height).withEyeHeight(eyeHeight);
            return this;
        }

        public Builder source(Supplier<? extends Ability> ability) {
            this.source = ability;
            return this;
        }

        public Transformation build() {
            return new Transformation(this);
        }
    }
}
