package com.nerodev.grandline.client.render;

import com.nerodev.grandline.fruit.kit.Disguise;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Draws disguised players: as a puppet creature of the disguise type that copies their position, rotation and walk
 * cycle, or (for a borrowed face) with the other player's skin.
 */
public final class DisguiseRenderer {
    private record Puppet(EntityType<?> type, Entity entity) {}

    private static final Map<Integer, Puppet> PUPPETS = new HashMap<>();
    private static boolean drawing;

    private DisguiseRenderer() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, RenderPlayerEvent.Pre.class, DisguiseRenderer::onRenderPlayer);
    }

    /** The skin to show instead of {@code player}'s own, if they wear another player's face. */
    @Nullable
    public static PlayerSkin borrowedSkin(AbstractClientPlayer player) {
        UUID face = Disguise.playerFace(player);
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (face == null || connection == null || face.equals(player.getUUID())) {
            return null;
        }
        PlayerInfo info = connection.getPlayerInfo(face);
        return info == null ? null : info.getSkin();
    }

    private static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (drawing || !(event.getEntity() instanceof AbstractClientPlayer player) || player.isInvisible()) {
            return;
        }
        Puppet puppet = PUPPETS.get(player.getId());
        if (puppet == null) {
            return;
        }
        event.setCanceled(true);
        float partial = event.getPartialTick();
        float yaw = Mth.rotLerp(partial, player.yRotO, player.getYRot());
        drawing = true;
        try {
            Minecraft.getInstance().getEntityRenderDispatcher().render(puppet.entity(), 0, 0, 0, yaw, partial,
                    event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        } finally {
            drawing = false;
        }
    }

    /** Once per client tick: create, update and drop puppets. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PUPPETS.clear();
            return;
        }
        for (Player player : mc.level.players()) {
            EntityType<?> type = Disguise.type(player);
            Puppet puppet = PUPPETS.get(player.getId());
            if (type == null) {
                if (puppet != null) {
                    PUPPETS.remove(player.getId());
                }
                continue;
            }
            if (puppet == null || puppet.type() != type) {
                Entity entity = type.create(mc.level);
                if (entity == null) {
                    continue;
                }
                puppet = new Puppet(type, entity);
                PUPPETS.put(player.getId(), puppet);
            }
            copy(player, puppet.entity());
        }
        Iterator<Integer> it = PUPPETS.keySet().iterator();
        while (it.hasNext()) {
            Entity owner = mc.level.getEntity(it.next());
            if (!(owner instanceof Player)) {
                it.remove();
            }
        }
    }

    private static void copy(Player player, Entity puppet) {
        puppet.xo = player.xo;
        puppet.yo = player.yo;
        puppet.zo = player.zo;
        puppet.xOld = player.xOld;
        puppet.yOld = player.yOld;
        puppet.zOld = player.zOld;
        puppet.setPos(player.getX(), player.getY(), player.getZ());
        puppet.yRotO = player.yRotO;
        puppet.xRotO = player.xRotO;
        puppet.setYRot(player.getYRot());
        puppet.setXRot(player.getXRot());
        puppet.setOnGround(player.onGround());
        puppet.setShiftKeyDown(player.isShiftKeyDown());
        puppet.setDeltaMovement(player.getDeltaMovement());
        puppet.tickCount = player.tickCount;
        if (puppet instanceof LivingEntity living) {
            living.yBodyRotO = player.yBodyRotO;
            living.yBodyRot = player.yBodyRot;
            living.yHeadRotO = player.yHeadRotO;
            living.yHeadRot = player.yHeadRot;
            living.hurtTime = player.hurtTime;
            living.walkAnimation.update(player.walkAnimation.speed(), 1f);
        }
    }
}
