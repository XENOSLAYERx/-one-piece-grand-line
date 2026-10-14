package com.nerodev.grandline.ship;

import com.nerodev.grandline.registry.GLRegistries;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A ship in a bottle: use it on water and the ship inside sails out at full size, facing where you look. Sneak and
 * use an empty bottle on an empty ship to put it back.
 */
public class ShipItem extends Item {
    public ShipItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** A bottle holding a ship of {@code type} with {@code health} hull health. */
    public static ItemStack of(ResourceLocation type, float health) {
        ItemStack stack = new ItemStack(GLShips.SHIP_ITEM.get());
        stack.set(GLShips.SHIP_TYPE.get(), type);
        stack.set(GLShips.SHIP_HEALTH.get(), health);
        return stack;
    }

    public static ResourceLocation type(ItemStack stack) {
        ResourceLocation type = stack.get(GLShips.SHIP_TYPE.get());
        return type == null ? ShipEntity.DEFAULT_TYPE : type;
    }

    /** The translation key of a ship type's name ({@code ship.grandline.galleon}). */
    public static String nameKey(ResourceLocation type) {
        return "ship." + type.getNamespace() + "." + type.getPath().toLowerCase(Locale.ROOT);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.grandline.ship_in_a_bottle.named", Component.translatable(nameKey(type(stack))));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Ships have to be launched on water").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel server) {
            ShipEntity ship = GLShips.SHIP.get().create(server);
            if (ship == null) {
                return InteractionResultHolder.fail(stack);
            }
            ship.setShipType(type(stack));
            ShipDefinition def = ship.definition();
            if (def == null) {
                player.displayClientMessage(Component.literal("This ship type no longer exists").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            Float health = stack.get(GLShips.SHIP_HEALTH.get());
            ship.setHealth(health == null ? def.maxHealth() : health);
            Vec3 at = Vec3.atBottomCenterOf(pos).add(0, level.getFluidState(pos).getHeight(level, pos) - def.draft(), 0);
            ship.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
            server.addFreshEntity(ship);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1f, 0.8f);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2f, 0.7f);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation type = type(stack);
        var registries = context.registries();
        if (registries == null) {
            return;
        }
        registries.lookup(GLRegistries.SHIP).flatMap(lookup -> lookup.get(ResourceKey.create(GLRegistries.SHIP, type)))
                .ifPresent(holder -> {
                    ShipDefinition def = holder.value();
                    Float health = stack.get(GLShips.SHIP_HEALTH.get());
                    tooltip.add(Component.literal(String.format(Locale.ROOT, "Hull %.0f/%.0f", health == null ? def.maxHealth() : health,
                            def.maxHealth())).withStyle(ChatFormatting.GRAY));
                    tooltip.add(Component.literal(String.format(Locale.ROOT, "%.0f knots, %d crew, %d cannons a side, %d rows of cargo",
                            def.maxSpeed() * 20 * 1.944, def.crew(), def.cannons(), def.storageRows())).withStyle(ChatFormatting.GRAY));
                    tooltip.add(Component.literal("Use on water to launch").withStyle(ChatFormatting.DARK_GRAY));
                });
    }
}
