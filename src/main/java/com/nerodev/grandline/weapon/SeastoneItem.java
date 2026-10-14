package com.nerodev.grandline.weapon;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Raw Seastone and its ingots: a Devil Fruit user holding either feels the sea drain their strength. */
public class SeastoneItem extends Item {
    public SeastoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Holds the power of the sea: Devil Fruit users who touch it are sealed")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
