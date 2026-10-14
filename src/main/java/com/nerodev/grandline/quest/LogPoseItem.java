package com.nerodev.grandline.quest;

import com.nerodev.grandline.data.GLPlayer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Log Pose, worn on the wrist: its needle points to the island it is set on (drawn from the player's synced
 * heading). Use it to read the needle, or to let it find the pull of the next island; sneak-use turns it to an island
 * already recorded.
 */
public class LogPoseItem extends Item {
    public LogPoseItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            if (player.isSecondaryUseActive()) {
                LogPose.cycle(server);
            } else if (GLPlayer.data(server).journey().heading().map(Journey.Heading::unknown).orElse(true)) {
                BlockPos next = LogPose.aimAtUnknown(server);
                if (next == null) {
                    server.displayClientMessage(Component.literal("The needle drifts: no island pulls at it within reach")
                            .withStyle(ChatFormatting.GRAY), true);
                    return InteractionResultHolder.consume(stack);
                }
            }
            server.displayClientMessage(LogPose.reading(server), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.grandline.log_pose.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.grandline.log_pose.use").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.grandline.log_pose.sneak").withStyle(ChatFormatting.DARK_AQUA));
    }
}
