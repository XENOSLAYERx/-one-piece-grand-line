package com.nerodev.grandline.creator;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.Loadout;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.haki.HakiType;
import com.nerodev.grandline.race.RaceManager;
import com.nerodev.grandline.registry.GLRegistries;
import com.nerodev.grandline.style.FightingStyle;
import com.nerodev.grandline.style.GLStyles;
import com.nerodev.grandline.style.StyleManager;
import com.nerodev.grandline.weapon.GLWeapons;
import com.nerodev.grandline.weapon.WeaponItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Builds for testing: capture what a player is right now as a {@link Loadout}, put a loadout back on, or roll a
 * random one. Creator tools only (the commands that call these check the permission).
 */
public final class Builds {
    private Builds() {}

    /** The player's current build. */
    public static Loadout capture(ServerPlayer player) {
        PlayerData data = GLPlayer.data(player);
        Map<String, Double> haki = new HashMap<>();
        for (HakiType type : HakiType.values()) {
            if (HakiManager.has(player, type)) {
                haki.put(type.getSerializedName(), HakiManager.level(player, type));
            }
        }
        Optional<ResourceLocation> weapon = player.getMainHandItem().isEmpty() ? Optional.empty()
                : Optional.of(BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()));
        List<String> slots = new ArrayList<>();
        AbilityState state = AbilityState.of(player);
        for (int i = 0; i < AbilityState.SLOT_COUNT; i++) {
            ResourceLocation id = state.slot(i);
            slots.add(id == null ? "" : id.toString());
        }
        return new Loadout(data.race(), data.fruit(), data.isAwakened(), data.style(), haki, weapon, slots);
    }

    /** Puts {@code loadout} on the player: race, fruit, Haki, style, weapon and ability slots. */
    public static void apply(ServerPlayer player, Loadout loadout) {
        AbilityManager.endAll(player, Ability.EndReason.INTERRUPTED, false);
        if (!GLPlayer.data(player).race().equals(loadout.race())) {
            RaceManager.setRace(player, loadout.race());
        }

        DevilFruit fruit = loadout.fruit().map(GLFruits.REGISTRY::get).orElse(null);
        if (fruit == null) {
            FruitManager.removeFruit(player);
        } else {
            if (FruitManager.fruitOf(player) != fruit) {
                FruitManager.setFruit(player, fruit);
            }
            if (loadout.awakened()) {
                FruitManager.awaken(player);
            }
        }

        for (HakiType type : HakiType.values()) {
            Double level = loadout.haki().get(type.getSerializedName());
            if (level == null) {
                HakiManager.remove(player, type);
            } else {
                HakiManager.setLevel(player, type, level);
            }
        }

        FightingStyle style = loadout.style().map(GLStyles.REGISTRY::get).orElse(GLStyles.BRAWLER.get());
        StyleManager.learn(player, style);
        StyleManager.equip(player, style);

        loadout.weapon().map(BuiltInRegistries.ITEM::get).filter(item -> item != Items.AIR).ifPresent(item -> give(player, item));

        AbilityState state = AbilityState.of(player);
        for (int i = 0; i < AbilityState.SLOT_COUNT; i++) {
            String id = i < loadout.slots().size() ? loadout.slots().get(i) : "";
            ResourceLocation ability = id.isEmpty() ? null : ResourceLocation.tryParse(id);
            Ability move = ability == null ? null : GLAbilities.REGISTRY.get(ability);
            AbilityManager.setSlot(player, i, move != null && AbilityAccess.canUse(player, move) ? ability : null);
        }
        state.refillStamina(AbilityManager.now(player));
        AbilityManager.sync(player);
    }

    /** A random build: any race, fruit, style and melee weapon, and a random set of Haki at random levels. */
    public static Loadout random(ServerPlayer player) {
        RandomSource random = player.getRandom();
        List<ResourceLocation> races = player.server.registryAccess().registryOrThrow(GLRegistries.RACE).keySet().stream()
                .sorted().toList();
        List<ResourceLocation> fruits = GLFruits.REGISTRY.keySet().stream().sorted().toList();
        List<ResourceLocation> styles = GLStyles.REGISTRY.keySet().stream().sorted().toList();
        List<ResourceLocation> weapons = GLWeapons.weapons().values().stream()
                .filter(holder -> holder.get() instanceof WeaponItem).map(holder -> holder.getId()).toList();
        Map<String, Double> haki = new HashMap<>();
        for (HakiType type : HakiType.values()) {
            if (random.nextFloat() < 0.6f) {
                haki.put(type.getSerializedName(), (double) (20 + random.nextInt(61)));
            }
        }
        return new Loadout(pick(races, random), Optional.of(pick(fruits, random)), random.nextFloat() < 0.25f,
                Optional.of(pick(styles, random)), haki, Optional.of(pick(weapons, random)), List.of());
    }

    /** After a random build, fill the empty slots with whatever the build can use. */
    public static void fillSlots(ServerPlayer player) {
        AbilityState state = AbilityState.of(player);
        for (Ability ability : AbilityAccess.usable(player)) {
            state.equipInFreeSlot(ability.id());
        }
        AbilityManager.sync(player);
    }

    private static <T> T pick(List<T> list, RandomSource random) {
        return list.get(random.nextInt(list.size()));
    }

    /** Gives {@code item} unless the player already carries one. */
    private static void give(ServerPlayer player, Item item) {
        if (player.getInventory().countItem(item) > 0) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
