package com.nerodev.grandline.data;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * What other players need to know to render someone: their fruit, whether it is awakened, and the active body
 * transformation. Synced to everyone tracking the player; never saved (rebuilt from {@link PlayerData} on join).
 */
public final class PublicState {
    public static final StreamCodec<RegistryFriendlyByteBuf, PublicState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), PublicState::fruit,
            ByteBufCodecs.BOOL, PublicState::awakened,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), PublicState::transformation,
            ByteBufCodecs.VAR_LONG, PublicState::transformedAt,
            PublicState::new);

    private Optional<ResourceLocation> fruit = Optional.empty();
    private boolean awakened;
    private Optional<ResourceLocation> transformation = Optional.empty();
    private long transformedAt;

    public PublicState() {}

    private PublicState(Optional<ResourceLocation> fruit, boolean awakened, Optional<ResourceLocation> transformation,
            long transformedAt) {
        this.fruit = fruit;
        this.awakened = awakened;
        this.transformation = transformation;
        this.transformedAt = transformedAt;
    }

    public static PublicState of(Player player) {
        return player.getData(GLAttachments.PUBLIC_STATE);
    }

    public Optional<ResourceLocation> fruit() {
        return fruit;
    }

    public boolean awakened() {
        return awakened;
    }

    public Optional<ResourceLocation> transformation() {
        return transformation;
    }

    /** Game time the current transformation started (for intro animations). */
    public long transformedAt() {
        return transformedAt;
    }

    public void setFruit(Optional<ResourceLocation> fruit, boolean awakened) {
        this.fruit = fruit;
        this.awakened = awakened;
    }

    public void setTransformation(Optional<ResourceLocation> transformation, long now) {
        this.transformation = transformation;
        this.transformedAt = now;
    }
}
