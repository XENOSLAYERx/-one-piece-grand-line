package com.nerodev.grandline.weapon;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Bullets for guns. Seastone bullets seal the Devil Fruit powers of whatever they hit and strike Logia bodies. */
public class AmmoItem extends Item {
    private final boolean seastone;

    public AmmoItem(boolean seastone, Properties properties) {
        super(properties);
        this.seastone = seastone;
    }

    public boolean seastone() {
        return seastone;
    }

    /** The ammunition a gun would use: the off hand first, then the rest of the inventory. Empty if none. */
    public static ItemStack find(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof AmmoItem) {
            return offhand;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof AmmoItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Takes one bullet from {@code player}'s ammunition (creative players never run out).
     *
     * @return whether the bullet is Seastone, or empty if there is none
     */
    public static Optional<Boolean> consume(Player player) {
        ItemStack ammo = find(player);
        if (ammo.isEmpty()) {
            return player.getAbilities().instabuild ? Optional.of(false) : Optional.empty();
        }
        boolean seastone = ((AmmoItem) ammo.getItem()).seastone();
        if (!player.getAbilities().instabuild) {
            ammo.shrink(1);
        }
        return Optional.of(seastone);
    }

    /** How many bullets {@code player} carries (creative players count as having plenty). */
    public static int count(Player player) {
        if (player.getAbilities().instabuild) {
            return 64;
        }
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof AmmoItem) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (seastone) {
            tooltip.add(Component.literal("Seals Devil Fruit powers; strikes Logia").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
