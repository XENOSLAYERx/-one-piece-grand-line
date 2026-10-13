package com.nerodev.grandline.transform;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.data.GLAttachments;
import com.nerodev.grandline.data.PublicState;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent;

/**
 * Central authority for body transformations: starting one ends any other in the same group, attribute
 * modifiers and hitbox changes are applied and removed together, and the state is synced to every viewer.
 */
public final class TransformationManager {
    private TransformationManager() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EntityEvent.Size.class, TransformationManager::onSize);
    }

    @Nullable
    public static Transformation active(Player player) {
        return PublicState.of(player).transformation().map(GLTransformations.REGISTRY::get).orElse(null);
    }

    public static boolean isActive(Player player, Transformation transformation) {
        return PublicState.of(player).transformation().map(id -> id.equals(transformation.id())).orElse(false);
    }

    /** Starts {@code transformation}, ending any current body transformation first. */
    public static void start(ServerPlayer player, Transformation transformation) {
        Transformation current = active(player);
        if (current == transformation) {
            return;
        }
        if (current != null) {
            end(player);
        }
        PublicState.of(player).setTransformation(Optional.of(transformation.id()), AbilityManager.now(player));
        List<Transformation.Modifier> modifiers = transformation.modifiers();
        for (int i = 0; i < modifiers.size(); i++) {
            Transformation.Modifier m = modifiers.get(i);
            AttributeInstance attribute = player.getAttribute(m.attribute());
            if (attribute != null) {
                ResourceLocation id = transformation.modifierId(i);
                attribute.removeModifier(id);
                attribute.addTransientModifier(new AttributeModifier(id, m.amount(), m.operation()));
            }
        }
        player.refreshDimensions();
        transformation.onStart(player);
        sync(player);
    }

    /** Ends the active transformation, if any, and the toggle ability that sustains it. */
    public static void end(ServerPlayer player) {
        Transformation current = active(player);
        if (current == null) {
            return;
        }
        PublicState.of(player).setTransformation(Optional.empty(), AbilityManager.now(player));
        for (int i = 0; i < current.modifiers().size(); i++) {
            AttributeInstance attribute = player.getAttribute(current.modifiers().get(i).attribute());
            if (attribute != null) {
                attribute.removeModifier(current.modifierId(i));
            }
        }
        player.refreshDimensions();
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        current.onEnd(player);
        sync(player);

        Ability source = current.sourceAbility();
        if (source != null && AbilityState.of(player).isActive(source.id())) {
            AbilityManager.end(player, source, Ability.EndReason.INTERRUPTED);
        }
    }

    private static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player) || event.getPose() != Pose.STANDING) {
            return;
        }
        if (!player.hasData(GLAttachments.PUBLIC_STATE)) {
            return;
        }
        Transformation transformation = active(player);
        if (transformation != null) {
            EntityDimensions dimensions = transformation.dimensions();
            if (dimensions != null) {
                event.setNewSize(dimensions);
            }
        }
    }

    public static void sync(ServerPlayer player) {
        if (GLAttachments.canSync(player)) {
            player.syncData(GLAttachments.PUBLIC_STATE);
        }
    }
}
