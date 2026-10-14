package com.nerodev.grandline.guide;

import com.nerodev.grandline.npc.NpcDefinition;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Where something comes from in survival play, worked out from the game's own data so it stays true: the recipes
 * that make it, the people who sell or carry it, the bosses who fight with it, plus a written note for what only loot
 * tables know (the {@code guide.grandline.found.<key>} line, when there is one). Works on both sides: the client's
 * Guide shows it and a GameTest checks it on the server.
 */
public final class Sources {
    private Sources() {}

    /**
     * Every way to get an item matching {@code wanted}.
     *
     * @param foundKey key of the written note on where it is found (looked up as {@code guide.grandline.found.<key>})
     */
    public static List<Component> of(Predicate<ItemStack> wanted, String foundKey, RecipeManager recipes, RegistryAccess access) {
        List<Component> out = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            Recipe<?> recipe = holder.value();
            ItemStack result = recipe.getResultItem(access);
            if (!result.isEmpty() && wanted.test(result)) {
                out.add(recipeLine(recipe));
            }
        }
        access.registry(GLRegistries.NPC).ifPresent(registry -> registry.forEach(npc -> {
            for (NpcDefinition.Trade trade : npc.trades()) {
                if (wanted.test(new ItemStack(trade.sell()))) {
                    MutableComponent line = Component.literal("Sold by " + npc.displayName().getString() + " for " + trade.buyCount()
                            + " " + trade.buy().getDescription().getString());
                    trade.buyB().ifPresent(b -> line.append(" and " + trade.buyBCount() + " " + b.getDescription().getString()));
                    if (trade.membersOnly()) {
                        line.append(" (only to members of their faction)");
                    }
                    out.add(line);
                }
            }
        }));
        access.registry(GLRegistries.NPC).ifPresent(registry -> registry.forEach(npc -> {
            if (npc.equipment().values().stream().anyMatch(item -> wanted.test(new ItemStack(item)))) {
                out.add(Component.literal(String.format(Locale.ROOT, "Carried by %s, who drops it now and then (%.0f%%)",
                        npc.displayName().getString(), NpcEntity.EQUIPMENT_DROP_CHANCE * 100)));
            }
        }));
        access.registry(GLRegistries.BOSS).ifPresent(registry -> registry.forEach(boss -> boss.weapon()
                .filter(weapon -> wanted.test(new ItemStack(weapon)))
                .ifPresent(weapon -> out.add(Component.literal("Always dropped by " + boss.displayName().getString()
                        + ", who fights with it")))));
        String key = "guide.grandline.found." + foundKey;
        if (Language.getInstance().has(key)) {
            out.add(Component.translatable(key));
        }
        return out;
    }

    /** "Craft: 2 Iron Block, Gold Block, ..." with the kind of recipe first. */
    private static Component recipeLine(Recipe<?> recipe) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            ItemStack[] options = ingredient.getItems();
            if (ingredient.isEmpty() || options.length == 0) {
                continue;
            }
            String name = options[0].getHoverName().getString() + (options.length > 1 ? " (or similar)" : "");
            counts.merge(name, 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((name, count) -> parts.add(count > 1 ? count + " " + name : name));
        RecipeType<?> type = recipe.getType();
        String verb = type == RecipeType.CRAFTING ? "Craft" : type == RecipeType.SMELTING || type == RecipeType.BLASTING ? "Smelt"
                : "Make";
        return Component.literal(verb + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.join(", ", parts)).withStyle(ChatFormatting.WHITE));
    }
}
