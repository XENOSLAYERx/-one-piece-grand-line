package com.nerodev.grandline.weapon;

import com.nerodev.grandline.entity.AbilityProjectile;
import java.util.List;
import net.minecraft.ChatFormatting;
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

/** A net woven with Seastone: throw it, and whatever it lands on is tangled, held in place and sealed. */
public class SeastoneNetItem extends Item {
    public SeastoneNetItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer thrower) {
            AbilityProjectile.spawn(thrower, GLWeapons.NET_THROWN, thrower.getEyePosition(), thrower.getLookAngle().scale(1.2), 1.2f,
                    GLWeapons.SEASTONE_COLOR, 1f, 40).withGravity(0.04);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1f, 0.6f);
            player.getCooldowns().addCooldown(this, 30);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Throw it: roots and seals whatever it lands on").withStyle(ChatFormatting.DARK_AQUA));
    }
}
