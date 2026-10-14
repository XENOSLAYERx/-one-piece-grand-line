package com.nerodev.grandline.item;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.style.FightingStyle;
import com.nerodev.grandline.style.StyleManager;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import org.jetbrains.annotations.Nullable;

/**
 * A technique scroll: reading it teaches a fighting style or a Rokushiki technique for good. This is how styles and
 * Rokushiki are earned without commands (craft the scroll, or find one).
 */
public class TechniqueScrollItem extends Item {
    @Nullable
    private final Supplier<? extends FightingStyle> style;
    @Nullable
    private final Supplier<? extends Ability> technique;

    private TechniqueScrollItem(@Nullable Supplier<? extends FightingStyle> style, @Nullable Supplier<? extends Ability> technique) {
        super(new Item.Properties().stacksTo(1));
        this.style = style;
        this.technique = technique;
    }

    public static TechniqueScrollItem style(Supplier<? extends FightingStyle> style) {
        return new TechniqueScrollItem(style, null);
    }

    public static TechniqueScrollItem technique(Supplier<? extends Ability> technique) {
        return new TechniqueScrollItem(null, technique);
    }

    /** Whether reading this scroll teaches {@code id} (a fighting style's or a technique's). */
    public boolean teaches(ResourceLocation id) {
        return style != null ? style.get().id().equals(id) : technique.get().id().equals(id);
    }

    Component teaches() {
        return style != null ? style.get().displayName() : technique.get().displayName();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.success(stack);
        }
        boolean learned;
        if (style != null) {
            learned = StyleManager.learn(server, style.get());
            if (learned) {
                StyleManager.equip(server, style.get());
            }
        } else {
            Ability ability = technique.get();
            boolean[] fresh = {false};
            GLPlayer.edit(server, data -> fresh[0] = data.unlock(UnlockCategory.ABILITY, ability.id()));
            learned = fresh[0];
            if (learned) {
                AbilityState.of(server).equipInFreeSlot(ability.id());
                AbilityManager.sync(server);
            }
        }
        if (!learned) {
            server.displayClientMessage(Component.literal("You already know ").append(teaches()), true);
            return InteractionResultHolder.fail(stack);
        }
        server.displayClientMessage(Component.literal("You learned ").append(teaches()).append("!").withStyle(ChatFormatting.GOLD), false);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        stack.consume(1, player);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Teaches: ").withStyle(ChatFormatting.GRAY).append(teaches().copy().withStyle(ChatFormatting.GOLD)));
        tooltip.add(Component.literal("Use to read it.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
