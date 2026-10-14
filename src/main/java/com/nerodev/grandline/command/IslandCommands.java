package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.world.island.IslandPiece;
import com.nerodev.grandline.world.island.IslandPlan;
import com.nerodev.grandline.world.island.IslandType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/** {@code /onepiece island}: list the islands, find the nearest of a kind, go there, or raise one on the spot. */
final class IslandCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece island list", "The islands that rise from the sea", PermissionTier.PLAYER),
            new HelpEntry("/onepiece island locate <island>", "Find the nearest island of a kind", PermissionTier.CREATOR),
            new HelpEntry("/onepiece island teleport <island>", "Go to the nearest island of a kind", PermissionTier.CREATOR),
            new HelpEntry("/onepiece teleport <island>", "Same as island teleport", PermissionTier.CREATOR),
            new HelpEntry("/onepiece island place <island> [seed]", "Raise an island right here, in a sea at your feet",
                    PermissionTier.CREATOR));

    /** Searched this many chunks out, like {@code /locate}. */
    private static final int SEARCH_RADIUS = 100;

    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            name -> Component.literal("Unknown island: " + name));
    private static final SuggestionProvider<CommandSourceStack> ISLANDS = (ctx, builder) -> SharedSuggestionProvider.suggest(
            Arrays.stream(IslandType.values()).map(IslandType::getSerializedName), builder);

    private IslandCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("island")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(creator("locate").then(Commands.argument("island", StringArgumentType.word()).suggests(ISLANDS)
                        .executes(ctx -> locate(ctx, false))))
                .then(creator("teleport").then(Commands.argument("island", StringArgumentType.word()).suggests(ISLANDS)
                        .executes(ctx -> locate(ctx, true))))
                .then(creator("place").then(Commands.argument("island", StringArgumentType.word()).suggests(ISLANDS)
                        .executes(ctx -> place(ctx, ctx.getSource().getLevel().random.nextLong()))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(ctx -> place(ctx, LongArgumentType.getLong(ctx, "seed")))))));
        // The short form creators reach for: /onepiece teleport <island>.
        root.then(creator("teleport").then(Commands.argument("island", StringArgumentType.word()).suggests(ISLANDS)
                .executes(ctx -> locate(ctx, true))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static IslandType island(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "island");
        IslandType island = IslandType.byName(name);
        if (island == null) {
            throw UNKNOWN.create(name);
        }
        return island;
    }

    private static int list(CommandSourceStack source) {
        Text.send(source, Text.header("Islands"));
        for (IslandType island : IslandType.values()) {
            Text.send(source, Component.literal("  ").append(island.displayName().copy().withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" " + island.getSerializedName() + " - about " + island.radius() * 2 + " blocks across, "
                            + island.sites().size() + " buildings").withStyle(ChatFormatting.GRAY)));
            Text.send(source, Component.literal("    ").append(Component.translatable("island.grandline." + island.getSerializedName()
                    + ".desc").withStyle(ChatFormatting.DARK_GRAY)));
        }
        return IslandType.values().length;
    }

    private static int locate(CommandContext<CommandSourceStack> ctx, boolean teleport) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        IslandType island = island(ctx);
        ServerLevel level = source.getLevel();
        Optional<Holder.Reference<Structure>> structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getHolder(ResourceKey.create(Registries.STRUCTURE, island.id()));
        if (structure.isEmpty()) {
            source.sendFailure(Component.literal("The structure " + island.id() + " is missing (is its data pack disabled?)"));
            return 0;
        }
        BlockPos from = BlockPos.containing(source.getPosition());
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level,
                HolderSet.direct(structure.get()), from, SEARCH_RADIUS, false);
        if (found == null) {
            source.sendFailure(Component.literal("No ").append(island.displayName()).append(" anywhere near (islands only rise in "
                    + "open ocean)"));
            return 0;
        }
        BlockPos at = found.getFirst();
        int distance = (int) Math.sqrt(Math.pow(at.getX() - from.getX(), 2) + Math.pow(at.getZ() - from.getZ(), 2));
        if (teleport) {
            ServerPlayer player = source.getPlayerOrException();
            level.getChunk(at.getX() >> 4, at.getZ() >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
            player.teleportTo(level, at.getX() + 0.5, y, at.getZ() + 0.5, player.getYRot(), player.getXRot());
            source.sendSuccess(() -> Component.literal("Welcome to ").append(island.displayName()), true);
            return distance;
        }
        String command = "/onepiece island teleport " + island.getSerializedName();
        source.sendSuccess(() -> island.displayName().copy().withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" is at " + at.getX() + ", " + at.getZ() + " (" + distance + " blocks away) ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Text.run("[go]", command, ChatFormatting.AQUA)), false);
        return distance;
    }

    private static int place(CommandContext<CommandSourceStack> ctx, long seed) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        IslandType island = island(ctx);
        BlockPos centre = BlockPos.containing(source.getPosition());
        IslandPlan plan = IslandPiece.placeNow(source.getLevel(), island, centre, seed);
        int top = plan.height(centre.getX(), centre.getZ());
        if (source.getEntity() instanceof ServerPlayer player) {
            player.teleportTo(source.getLevel(), centre.getX() + 0.5, top + 1, centre.getZ() + 0.5, player.getYRot(), player.getXRot());
        }
        source.sendSuccess(() -> Component.literal("Raised ").append(island.displayName())
                .append(" (seed " + seed + ", " + plan.buildings.size() + " buildings)"), true);
        return plan.buildings.size();
    }
}
