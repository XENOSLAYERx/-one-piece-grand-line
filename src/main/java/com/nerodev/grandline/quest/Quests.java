package com.nerodev.grandline.quest;

import com.nerodev.grandline.boss.BossDefinition;
import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.bounty.Bounties;
import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.definition.FactionDefinition;
import com.nerodev.grandline.npc.NpcDefinition;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.registry.GLRegistries;
import com.nerodev.grandline.ship.ShipEntity;
import com.nerodev.grandline.world.island.IslandType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Quests: who offers them, taking them on, progress on their objectives as the player fights, talks, sails,
 * gathers and explores, and the rewards when they are done. Main quests start by themselves as the story moves on;
 * side quests are offered by islanders when they are spoken to.
 */
public final class Quests {
    /** How often held items, bounty and being aboard are checked against objectives. */
    private static final int CHECK_TICKS = 20;
    /** An islander offers the same quest (or repeats its progress) at most this often, so trading is never blocked. */
    private static final long REMIND_TICKS = 600;
    /** Crewmates this close share the credit for a defeated foe. */
    private static final double SHARE_RANGE = 48;

    private static final Map<UUID, Map<String, Long>> REMINDED = new HashMap<>();

    private Quests() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, Quests::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                startDue(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> REMINDED.remove(event.getEntity().getUUID()));
    }

    public static Registry<QuestDefinition> registry(RegistryAccess access) {
        return access.registryOrThrow(GLRegistries.QUEST);
    }

    @Nullable
    public static QuestDefinition get(RegistryAccess access, ResourceLocation id) {
        return access.registry(GLRegistries.QUEST).map(registry -> registry.get(id)).orElse(null);
    }

    /** Every quest, main quests first in story order, then side quests by name. */
    public static List<Map.Entry<ResourceLocation, QuestDefinition>> all(RegistryAccess access) {
        List<Map.Entry<ResourceLocation, QuestDefinition>> out = new ArrayList<>();
        access.registry(GLRegistries.QUEST).ifPresent(registry -> registry.entrySet()
                .forEach(e -> out.add(Map.entry(e.getKey().location(), e.getValue()))));
        out.sort(Comparator.comparing((Map.Entry<ResourceLocation, QuestDefinition> e) -> e.getValue().category())
                .thenComparingInt(e -> e.getValue().order()).thenComparing(e -> e.getKey().toString()));
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Taking quests on and finishing them
    // ---------------------------------------------------------------------------------------------------------

    /** Why {@code player} cannot take {@code quest} now, or null if they can. */
    @Nullable
    public static String blocker(Player player, ResourceLocation id, QuestDefinition quest) {
        PlayerData data = GLPlayer.data(player);
        Journey journey = data.journey();
        if (journey.isActive(id)) {
            return "already under way";
        }
        if (journey.isCompleted(id)) {
            return "already done";
        }
        for (ResourceLocation before : quest.requires()) {
            if (!journey.isCompleted(before)) {
                QuestDefinition earlier = get(player.level().registryAccess(), before);
                return "first finish " + (earlier == null ? before.toString() : earlier.displayName().getString());
            }
        }
        for (Map.Entry<ResourceLocation, Integer> need : quest.standing().entrySet()) {
            if (data.reputation(need.getKey()) < need.getValue()) {
                return "needs more standing with " + need.getKey().getPath();
            }
        }
        return null;
    }

    public static boolean available(Player player, ResourceLocation id, QuestDefinition quest) {
        return blocker(player, id, quest) == null;
    }

    /** Takes {@code quest} on (checks nothing; see {@link #blocker}). */
    public static void start(ServerPlayer player, ResourceLocation id, QuestDefinition quest) {
        GLPlayer.edit(player, data -> data.journey().start(id, quest.objectives().size()));
        player.sendSystemMessage(Component.literal(quest.category() == QuestDefinition.Category.MAIN ? "New story quest: " : "New quest: ")
                .withStyle(ChatFormatting.GOLD).append(quest.displayName().copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
        player.sendSystemMessage(quest.description().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        for (QuestObjective objective : quest.objectives()) {
            player.sendSystemMessage(Component.literal("  - ").withStyle(ChatFormatting.DARK_GRAY).append(describe(player.level().registryAccess(),
                    objective).withStyle(ChatFormatting.WHITE)));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.9f);
        check(player);
    }

    /** Starts every story quest that is now due (no giver, everything before it done). */
    public static void startDue(ServerPlayer player) {
        for (Map.Entry<ResourceLocation, QuestDefinition> e : all(player.level().registryAccess())) {
            if (e.getValue().startsByItself() && available(player, e.getKey(), e.getValue())) {
                start(player, e.getKey(), e.getValue());
            }
        }
    }

    /** Hands a quest in: takes what it consumes, pays out and moves the story on. */
    public static void complete(ServerPlayer player, ResourceLocation id, QuestDefinition quest, boolean free) {
        if (!free) {
            for (QuestObjective objective : quest.objectives()) {
                if (objective.kind() == QuestObjective.Kind.COLLECT && objective.consume()) {
                    Item item = item(objective);
                    if (item != null) {
                        player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), objective.count(),
                                player.inventoryMenu.getCraftSlots());
                    }
                }
            }
        }
        GLPlayer.edit(player, data -> data.journey().complete(id));
        QuestDefinition.Rewards rewards = quest.rewards();
        for (ItemStack stack : rewards.items()) {
            ItemStack given = stack.copy();
            if (!player.getInventory().add(given)) {
                player.drop(given, false);
            }
        }
        if (rewards.bounty() > 0) {
            Bounties.crime(player, rewards.bounty(), false, quest.displayName().getString());
        }
        if (rewards.renown() > 0) {
            Crews.addRenown(player, rewards.renown());
        }
        if (!rewards.reputation().isEmpty()) {
            GLPlayer.edit(player, data -> rewards.reputation().forEach(data::addReputation));
        }
        player.sendSystemMessage(Component.literal("Quest complete: ").withStyle(ChatFormatting.GREEN)
                .append(quest.displayName().copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
        if (!quest.lines().done().isEmpty()) {
            player.sendSystemMessage(quoted(player.level().registryAccess(), quest, quest.lines().done()));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.1f);
        startDue(player);
    }

    /** Abandons or forgets a quest (creators also use this to replay one). */
    public static void reset(ServerPlayer player, @Nullable ResourceLocation id) {
        GLPlayer.edit(player, data -> {
            if (id == null) {
                data.journey().clearQuests();
            } else {
                data.journey().forget(id);
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------
    // Progress
    // ---------------------------------------------------------------------------------------------------------

    /** {@code amount} more towards every active objective of {@code kind} about {@code target}. */
    public static void advance(ServerPlayer player, QuestObjective.Kind kind, ResourceLocation target, int amount) {
        RegistryAccess access = player.level().registryAccess();
        List<Runnable> news = new ArrayList<>();
        GLPlayer.edit(player, data -> {
            Journey journey = data.journey();
            for (ResourceLocation id : List.copyOf(journey.activeQuests())) {
                QuestDefinition quest = get(access, id);
                if (quest == null) {
                    continue;
                }
                for (int i = 0; i < quest.objectives().size(); i++) {
                    QuestObjective objective = quest.objectives().get(i);
                    if (objective.kind() != kind || !objective.matches(target)) {
                        continue;
                    }
                    int before = journey.progress(id, i);
                    int after = Math.min(objective.goal(), before + amount);
                    if (after != before && journey.setProgress(id, i, after)) {
                        news.add(() -> announce(player, objective, after));
                    }
                }
            }
        });
        news.forEach(Runnable::run);
        finishReady(player);
    }

    /** Brings the objectives that depend on what the player has or is (bag, bounty, ship, islands) up to date. */
    public static void check(ServerPlayer player) {
        RegistryAccess access = player.level().registryAccess();
        PlayerData current = GLPlayer.data(player);
        boolean changed = false;
        for (ResourceLocation id : current.journey().activeQuests()) {
            QuestDefinition quest = get(access, id);
            if (quest == null) {
                continue;
            }
            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective objective = quest.objectives().get(i);
                int now = state(player, current, objective);
                if (now >= 0 && now != current.journey().progress(id, i)) {
                    changed = true;
                }
            }
        }
        if (!changed) {
            finishReady(player);
            return;
        }
        List<Runnable> news = new ArrayList<>();
        GLPlayer.edit(player, data -> {
            Journey journey = data.journey();
            for (ResourceLocation id : List.copyOf(journey.activeQuests())) {
                QuestDefinition quest = get(access, id);
                if (quest == null) {
                    continue;
                }
                for (int i = 0; i < quest.objectives().size(); i++) {
                    QuestObjective objective = quest.objectives().get(i);
                    int now = state(player, data, objective);
                    int before = journey.progress(id, i);
                    if (now >= 0 && journey.setProgress(id, i, now) && now > before) {
                        news.add(() -> announce(player, objective, now));
                    }
                }
            }
        });
        news.forEach(Runnable::run);
        finishReady(player);
    }

    /** Progress an objective takes from the player's state, or -1 if it is earned by deeds instead. */
    private static int state(ServerPlayer player, PlayerData data, QuestObjective objective) {
        return switch (objective.kind()) {
            case COLLECT -> {
                Item item = item(objective);
                yield item == null ? 0 : Math.min(objective.goal(), player.getInventory().countItem(item));
            }
            case BOUNTY -> data.bounty() >= objective.count() ? 1 : 0;
            case SAIL -> player.getVehicle() instanceof ShipEntity ? 1 : -1;
            case DISCOVER -> objective.target().isPresent()
                    ? (data.journey().hasFound(objective.target().get().getPath()) ? 1 : 0)
                    : Math.min(objective.goal(), data.journey().islands().size());
            default -> -1;
        };
    }

    private static void finishReady(ServerPlayer player) {
        RegistryAccess access = player.level().registryAccess();
        for (ResourceLocation id : List.copyOf(GLPlayer.data(player).journey().activeQuests())) {
            QuestDefinition quest = get(access, id);
            if (quest != null && done(GLPlayer.data(player).journey(), id, quest)) {
                complete(player, id, quest, false);
            }
        }
    }

    public static boolean done(Journey journey, ResourceLocation id, QuestDefinition quest) {
        for (int i = 0; i < quest.objectives().size(); i++) {
            if (journey.progress(id, i) < quest.objectives().get(i).goal()) {
                return false;
            }
        }
        return true;
    }

    private static void announce(ServerPlayer player, QuestObjective objective, int progress) {
        Component what = describe(player.level().registryAccess(), objective);
        if (progress >= objective.goal()) {
            player.sendSystemMessage(Component.literal("Objective done: ").withStyle(ChatFormatting.GREEN).append(what.copy()
                    .withStyle(ChatFormatting.WHITE)));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.6f, 1.2f);
        } else {
            player.displayClientMessage(what.copy().withStyle(ChatFormatting.YELLOW).append(Component.literal(" " + progress + "/"
                    + objective.goal()).withStyle(ChatFormatting.WHITE)), true);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Deeds
    // ---------------------------------------------------------------------------------------------------------

    /** {@code killer} defeated {@code victim}; crewmates fighting nearby share the credit. */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        List<ServerPlayer> credited = new ArrayList<>();
        credited.add(killer);
        for (ServerPlayer other : killer.serverLevel().players()) {
            if (other != killer && other.distanceTo(victim) < SHARE_RANGE && Crews.crewmates(killer, other)) {
                credited.add(other);
            }
        }
        for (ServerPlayer player : credited) {
            if (victim instanceof NpcEntity npc) {
                ResourceLocation kind = npc.npcId();
                if (kind != null) {
                    advance(player, QuestObjective.Kind.DEFEAT_NPC, kind, 1);
                }
                npc.faction().ifPresent(faction -> advance(player, QuestObjective.Kind.DEFEAT_FACTION, faction, 1));
            }
        }
    }

    /**
     * {@code boss} fell: everyone who fought it nearby, and the victor's crewmates, are credited for defeating it.
     */
    public static void onBossDefeated(BossEntity boss, @Nullable ServerPlayer killer, Collection<ServerPlayer> fighters) {
        ResourceLocation id = boss.bossId();
        if (id == null) {
            return;
        }
        Set<ServerPlayer> credited = new LinkedHashSet<>();
        if (killer != null) {
            credited.add(killer);
            for (ServerPlayer other : killer.serverLevel().players()) {
                if (other != killer && other.distanceTo(boss) < SHARE_RANGE && Crews.crewmates(killer, other)) {
                    credited.add(other);
                }
            }
        }
        for (ServerPlayer fighter : fighters) {
            if (!fighter.hasDisconnected() && fighter.level() == boss.level() && fighter.distanceTo(boss) < SHARE_RANGE) {
                credited.add(fighter);
            }
        }
        for (ServerPlayer player : credited) {
            advance(player, QuestObjective.Kind.DEFEAT_BOSS, id, 1);
            boss.faction().ifPresent(faction -> advance(player, QuestObjective.Kind.DEFEAT_FACTION, faction, 1));
        }
    }

    /** {@code player} found an island. */
    public static void onDiscover(ServerPlayer player, IslandType island) {
        advance(player, QuestObjective.Kind.DISCOVER, island.id(), 1);
        check(player);
    }

    /**
     * {@code player} spoke to {@code npc}: it may be someone a quest sends them to, or it may have a quest to
     * offer or one to ask after. @return whether that took the conversation (otherwise the islander talks or trades)
     */
    public static boolean interact(ServerPlayer player, NpcEntity npc) {
        ResourceLocation kind = npc.npcId();
        if (kind == null) {
            return false;
        }
        RegistryAccess access = player.level().registryAccess();
        Journey journey = GLPlayer.data(player).journey();
        // Someone a quest sent the player to.
        for (ResourceLocation id : journey.activeQuests()) {
            QuestDefinition quest = get(access, id);
            if (quest == null) {
                continue;
            }
            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective objective = quest.objectives().get(i);
                if (objective.kind() == QuestObjective.Kind.TALK && objective.matches(kind) && journey.progress(id, i) < objective.goal()) {
                    advance(player, QuestObjective.Kind.TALK, kind, 1);
                    return true;
                }
            }
        }
        long now = player.level().getGameTime();
        Map<String, Long> reminded = REMINDED.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        // A quest this islander has to offer.
        for (Map.Entry<ResourceLocation, QuestDefinition> e : all(access)) {
            QuestDefinition quest = e.getValue();
            if (quest.giver().map(kind::equals).orElse(false) && available(player, e.getKey(), quest)) {
                String key = "offer:" + e.getKey();
                if (reminded.getOrDefault(key, Long.MIN_VALUE) + REMIND_TICKS > now) {
                    return false;
                }
                reminded.put(key, now);
                offer(player, npc, e.getKey(), quest);
                return true;
            }
        }
        // A quest of theirs under way.
        for (ResourceLocation id : journey.activeQuests()) {
            QuestDefinition quest = get(access, id);
            if (quest != null && quest.giver().map(kind::equals).orElse(false)) {
                String key = "ask:" + id;
                if (reminded.getOrDefault(key, Long.MIN_VALUE) + REMIND_TICKS > now) {
                    return false;
                }
                reminded.put(key, now);
                player.sendSystemMessage(Component.literal("<").append(npc.getDisplayName()).append("> ")
                        .append(Component.literal("How goes it? ").withStyle(ChatFormatting.WHITE))
                        .append(quest.displayName().copy().withStyle(ChatFormatting.YELLOW)));
                for (int i = 0; i < quest.objectives().size(); i++) {
                    player.sendSystemMessage(objectiveLine(access, quest.objectives().get(i), journey.progress(id, i)));
                }
                return true;
            }
        }
        return false;
    }

    private static void offer(ServerPlayer player, NpcEntity npc, ResourceLocation id, QuestDefinition quest) {
        String line = quest.lines().offer().isEmpty() ? "I could use a hand." : quest.lines().offer();
        player.sendSystemMessage(Component.literal("<").append(npc.getDisplayName()).append("> ").append(Component.literal(line)
                .withStyle(ChatFormatting.WHITE)));
        MutableComponent accept = Component.literal("[Accept]").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/onepiece quest accept " + id))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, quest.description())));
        player.sendSystemMessage(Component.literal("Quest: ").withStyle(ChatFormatting.GOLD).append(quest.displayName().copy()
                .withStyle(ChatFormatting.YELLOW)).append(Component.literal("  ")).append(accept));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Words
    // ---------------------------------------------------------------------------------------------------------

    /** What an objective asks, in words ("Defeat 5 Fish-Man Pirates", "Find Shells Town"). */
    public static MutableComponent describe(RegistryAccess access, QuestObjective objective) {
        if (objective.text().isPresent()) {
            return objective.text().get().copy();
        }
        Component name = objective.target().map(target -> targetName(access, objective.kind(), target)).orElse(null);
        int count = objective.count();
        return switch (objective.kind()) {
            case DEFEAT_NPC -> Component.literal("Defeat " + (count > 1 ? count + " " : "")).append(orAny(name, "foes"));
            case DEFEAT_FACTION -> Component.literal("Defeat " + count + " of the ").append(orAny(name, "enemy"));
            case DEFEAT_BOSS -> Component.literal("Defeat ").append(orAny(name, "a boss"));
            case TALK -> Component.literal("Speak with ").append(orAny(name, "an islander"));
            case DISCOVER -> name != null ? Component.literal("Find ").append(name)
                    : Component.literal(count == 1 ? "Find an island" : "Find " + count + " islands");
            case COLLECT -> Component.literal("Gather " + count + " ").append(orAny(name, "items"));
            case BOUNTY -> Component.literal("Earn a bounty of " + Bounties.format(count));
            case SAIL -> Component.literal("Set out to sea aboard a ship");
        };
    }

    /** An objective with its progress, for the journal and for islanders asking after it. */
    public static MutableComponent objectiveLine(RegistryAccess access, QuestObjective objective, int progress) {
        boolean done = progress >= objective.goal();
        MutableComponent line = Component.literal(done ? "  [x] " : "  [ ] ").withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                .append(describe(access, objective).withStyle(done ? ChatFormatting.GRAY : ChatFormatting.WHITE));
        if (objective.goal() > 1) {
            line.append(Component.literal(" " + Math.min(progress, objective.goal()) + "/" + objective.goal()).withStyle(ChatFormatting.YELLOW));
        }
        return line;
    }

    private static Component orAny(@Nullable Component name, String fallback) {
        return name != null ? name : Component.literal(fallback);
    }

    @Nullable
    private static Component targetName(RegistryAccess access, QuestObjective.Kind kind, ResourceLocation target) {
        return switch (kind) {
            case DEFEAT_NPC, TALK -> access.registry(GLRegistries.NPC).map(r -> r.get(target)).map(NpcDefinition::displayName).orElse(null);
            case DEFEAT_FACTION -> access.registry(GLRegistries.FACTION).map(r -> r.get(target)).map(FactionDefinition::displayName)
                    .orElse(null);
            case DEFEAT_BOSS -> access.registry(GLRegistries.BOSS).map(r -> r.get(target)).map(BossDefinition::displayName).orElse(null);
            case DISCOVER -> {
                IslandType island = IslandType.byId(target);
                yield island == null ? null : island.displayName();
            }
            case COLLECT -> {
                Item item = BuiltInRegistries.ITEM.get(target);
                yield item == Items.AIR ? null : item.getDescription();
            }
            default -> null;
        };
    }

    @Nullable
    private static Item item(QuestObjective objective) {
        if (objective.target().isEmpty()) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(objective.target().get());
        return item == Items.AIR ? null : item;
    }

    private static Component quoted(RegistryAccess access, QuestDefinition quest, String line) {
        Component speaker = quest.giver().flatMap(giver -> Optional.ofNullable(access.registry(GLRegistries.NPC)
                .map(r -> r.get(giver)).map(NpcDefinition::displayName).orElse(null))).orElse(Component.literal("Log"));
        return Component.literal("<").append(speaker).append("> ").append(Component.literal(line).withStyle(ChatFormatting.WHITE));
    }

    // ---------------------------------------------------------------------------------------------------------

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK_TICKS != 0) {
            return;
        }
        if (!GLPlayer.data(player).journey().activeQuests().isEmpty()) {
            check(player);
        }
        if (player.tickCount % (CHECK_TICKS * 2) == 0) {
            LogPose.tick(player);
        }
    }
}
