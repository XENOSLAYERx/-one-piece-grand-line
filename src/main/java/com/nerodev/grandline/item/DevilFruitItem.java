package com.nerodev.grandline.item;

import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.FruitManager;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * An edible Devil Fruit. Eating grants its power (see {@link FruitManager#eat}). If you already have a fruit,
 * eating is refused unless you sneak, because a second fruit is deadly.
 */
public class DevilFruitItem extends Item {
    private final Supplier<? extends DevilFruit> fruit;

    public DevilFruitItem(Supplier<? extends DevilFruit> fruit, Rarity rarity) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(rarity)
                .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build()));
        this.fruit = fruit;
    }

    public DevilFruit fruit() {
        return fruit.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (GLPlayer.data(player).fruit().isPresent() && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal(
                        "You already have a Devil Fruit. Eating another will kill you. Sneak to eat it anyway.")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            FruitManager.eat(player, fruit.get());
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DevilFruit devilFruit = fruit.get();
        tooltip.add(Component.literal(devilFruit.originalName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(devilFruit.type().display().copy().append(Component.literal("  ").append(devilFruit.rarity().display())));
        tooltip.add(devilFruit.description().copy().withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Eat to gain its power. One fruit per body; the sea will reject you.")
                .withStyle(ChatFormatting.DARK_RED));
    }
}
