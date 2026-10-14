package com.nerodev.grandline.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.network.OpenGuidePayload;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.registry.DefinitionType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@code /onepiece profile}: text profile. {@code /onepiece guide}: the Guide screen, whose Profile page shows the same
 * data graphically.
 */
final class ProfileCommand {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece profile", "Show your race, faction and bounty", PermissionTier.PLAYER),
            new HelpEntry("/onepiece profile <player>", "Show another player's profile", PermissionTier.MODERATOR),
            new HelpEntry("/onepiece guide", "Open the Guide: your profile and everything in the mod", PermissionTier.PLAYER));

    private ProfileCommand() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("profile")
                .executes(ctx -> sendProfile(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> Permissions.has(source, PermissionTier.MODERATOR))
                        .executes(ctx -> sendProfile(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        root.then(Commands.literal("guide").executes(ctx -> openGuide(ctx.getSource().getPlayerOrException())));
    }

    /** Opens the Guide, telling the client what only the server knows: creator rights and the player's crew. */
    static int openGuide(ServerPlayer player) {
        boolean creator = Permissions.has(player, PermissionTier.CREATOR);
        OpenGuidePayload payload = Crews.of(player)
                .map(crew -> new OpenGuidePayload(creator, crew.name(), crew.members().getOrDefault(crew.captain(), "?"),
                        crew.members().size(), Crews.totalBounty(player.server, crew)))
                .orElseGet(() -> new OpenGuidePayload(creator, "", "", 0, 0L));
        PacketDistributor.sendToPlayer(player, payload);
        return 1;
    }

    static int sendProfile(CommandSourceStack source, ServerPlayer player) {
        PlayerData data = GLPlayer.data(player);
        RegistryAccess access = source.registryAccess();
        Text.send(source, Text.header(player.getGameProfile().getName() + "'s profile"));
        Text.send(source, Text.line("Race", nameOf(access, DefinitionType.RACE, data.race())));
        Text.send(source, Text.line("Faction", data.faction()
                .map(id -> factionName(access, id))
                .orElse(Component.literal("None").withStyle(ChatFormatting.GRAY))));
        Text.send(source, Text.line("Bounty", formatBounty(data.bounty())));
        return 1;
    }

    /** Everything stored for the player, for {@code /onepiece debug player}. */
    static int sendDetailed(CommandSourceStack source, ServerPlayer player) {
        sendProfile(source, player);
        PlayerData data = GLPlayer.data(player);
        Text.send(source, Text.line("Death mode", data.deathMode().display()));

        for (UnlockCategory category : UnlockCategory.values()) {
            Set<ResourceLocation> ids = data.unlocked(category);
            String list = ids.isEmpty() ? "-" : ids.stream().map(ResourceLocation::toString).sorted()
                    .collect(Collectors.joining(", "));
            Text.send(source, Text.line("Unlocked " + category.getSerializedName() + " (" + ids.size() + ")", list));
        }

        Map<String, Double> mastery = data.allMastery();
        if (mastery.isEmpty()) {
            Text.send(source, Text.line("Mastery", "-"));
        } else {
            mastery.forEach((key, value) -> Text.send(source,
                    Text.line("Mastery " + key, String.format(Locale.ROOT, "%.1f", value))));
        }
        return 1;
    }

    static <T> MutableComponent nameOf(RegistryAccess access, DefinitionType<T> type, ResourceLocation id) {
        Optional<Map.Entry<ResourceLocation, T>> entry = type.find(access, id);
        if (entry.isEmpty()) {
            return Component.literal(id + " (missing)").withStyle(ChatFormatting.RED);
        }
        return type.name().apply(entry.get().getValue()).copy().withStyle(ChatFormatting.WHITE);
    }

    static MutableComponent factionName(RegistryAccess access, ResourceLocation id) {
        return DefinitionType.FACTION.find(access, id)
                .map(entry -> entry.getValue().displayName().copy()
                        .withStyle(style -> style.withColor(entry.getValue().color())))
                .orElseGet(() -> Component.literal(id + " (missing)").withStyle(ChatFormatting.RED));
    }

    static MutableComponent formatBounty(long bounty) {
        return Component.literal(String.format(Locale.ROOT, "฿ %,d", bounty)).withStyle(ChatFormatting.GOLD);
    }
}
