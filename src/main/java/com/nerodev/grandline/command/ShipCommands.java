package com.nerodev.grandline.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.registry.DefinitionType;
import com.nerodev.grandline.ship.Encounter;
import com.nerodev.grandline.ship.GLShips;
import com.nerodev.grandline.ship.SeaEncounters;
import com.nerodev.grandline.ship.ShipDefinition;
import com.nerodev.grandline.ship.ShipEntity;
import com.nerodev.grandline.ship.ShipItem;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** {@code /onepiece ship}: list, give, spawn, repair and remove ships. */
final class ShipCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece ship list", "Every kind of ship", PermissionTier.PLAYER),
            new HelpEntry("/onepiece ship give <ship> [players]", "Give a ship in a bottle", PermissionTier.CREATOR),
            new HelpEntry("/onepiece ship spawn <ship>", "Launch a ship right where you look", PermissionTier.CREATOR),
            new HelpEntry("/onepiece ship repair", "Fully repair the ship you are on (or the nearest)", PermissionTier.CREATOR),
            new HelpEntry("/onepiece ship remove", "Remove the nearest ship", PermissionTier.CREATOR),
            new HelpEntry("/onepiece ship fire|burst", "Fire a broadside or Coup de Burst from the ship you are on", PermissionTier.CREATOR),
            new HelpEntry("/onepiece ship encounter <marine_patrol|pirate_raider|merchant>",
                    "A crewed ship comes over the horizon (or right ahead, if you look at open water)", PermissionTier.CREATOR));

    private ShipCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("ship")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(creator("give").then(Commands.argument("ship", ResourceLocationArgument.id())
                        .suggests(RegistryArguments.suggest(DefinitionType.SHIP))
                        .executes(ctx -> give(ctx, List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> give(ctx, EntityArgument.getPlayers(ctx, "players"))))))
                .then(creator("spawn").then(Commands.argument("ship", ResourceLocationArgument.id())
                        .suggests(RegistryArguments.suggest(DefinitionType.SHIP)).executes(ShipCommands::spawn)))
                .then(creator("repair").executes(ctx -> repair(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(creator("remove").executes(ctx -> remove(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(creator("fire").executes(ctx -> fire(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(creator("burst").executes(ctx -> burst(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(encounters()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> encounters() {
        LiteralArgumentBuilder<CommandSourceStack> node = creator("encounter");
        for (Encounter kind : Encounter.values()) {
            node.then(Commands.literal(kind.getSerializedName())
                    .executes(ctx -> encounter(ctx.getSource(), ctx.getSource().getPlayerOrException(), kind)));
        }
        return node;
    }

    /** A crewed ship of {@code kind}: 24 blocks ahead if there is open water there, else over the horizon. */
    private static int encounter(CommandSourceStack source, ServerPlayer player, Encounter kind) {
        ServerLevel level = player.serverLevel();
        float yaw = player.getYRot();
        Vec3 ahead = player.position().add(ShipEntity.forward(yaw).scale(24));
        ShipEntity ship = SeaEncounters.launch(level, kind, ahead, yaw + 90);
        if (ship == null) {
            ship = SeaEncounters.launchNear(level, player, kind);
        }
        if (ship == null) {
            source.sendFailure(Component.literal("There is no open water near enough for a ship"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("A " + kind.title().toLowerCase(Locale.ROOT) + " sets sail"), true);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static int list(CommandSourceStack source) {
        Text.send(source, Text.header("Ships"));
        var registry = source.getServer().registryAccess().registryOrThrow(DefinitionType.SHIP.key());
        List<Map.Entry<ResourceLocation, ShipDefinition>> ships = registry.entrySet().stream()
                .map(e -> Map.entry(e.getKey().location(), e.getValue()))
                .sorted(Comparator.comparing(e -> e.getValue().length()))
                .toList();
        for (Map.Entry<ResourceLocation, ShipDefinition> e : ships) {
            ShipDefinition ship = e.getValue();
            Text.send(source, Component.literal("  ").append(ship.displayName().copy().withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format(" %s - %.0f blocks, hull %.0f, %d cannons a side, crew %d",
                            e.getKey(), ship.length(), ship.maxHealth(), ship.cannons(), ship.crew())).withStyle(ChatFormatting.GRAY)));
        }
        return ships.size();
    }

    private static Map.Entry<ResourceLocation, ShipDefinition> ship(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return RegistryArguments.get(ctx, "ship", DefinitionType.SHIP);
    }

    private static int give(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        Map.Entry<ResourceLocation, ShipDefinition> ship = ship(ctx);
        for (ServerPlayer player : targets) {
            ItemStack bottle = ShipItem.of(ship.getKey(), ship.getValue().maxHealth());
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        Component name = ship.getValue().displayName();
        ctx.getSource().sendSuccess(() -> Component.literal("Gave ").append(name).append(" to " + targets.size() + " player(s)"), true);
        return targets.size();
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Map.Entry<ResourceLocation, ShipDefinition> ship = ship(ctx);
        float yaw = source.getRotation().y;
        ShipEntity entity = launch(source.getLevel(), ship.getKey(), source.getPosition().add(ShipEntity.forward(yaw)
                .scale(ship.getValue().width() + 2)), yaw);
        Component name = ship.getValue().displayName();
        ctx.getSource().sendSuccess(() -> Component.literal("Launched ").append(name), true);
        return entity == null ? 0 : 1;
    }

    /** Puts a ship of {@code type} in the world at {@code at}, resting at its draft if there is water. */
    static ShipEntity launch(ServerLevel level, ResourceLocation type, Vec3 at, float yaw) {
        ShipEntity ship = GLShips.SHIP.get().create(level);
        if (ship == null) {
            return null;
        }
        ship.setShipType(type);
        ship.moveTo(at.x, at.y, at.z, yaw, 0);
        ShipDefinition def = ship.definition();
        if (def != null) {
            double surface = ship.waterSurface(def);
            if (!Double.isNaN(surface)) {
                ship.moveTo(at.x, surface - def.draft(), at.z, yaw, 0);
            }
        }
        level.addFreshEntity(ship);
        return ship;
    }

    private static Optional<ShipEntity> nearest(ServerPlayer player) {
        if (player.getVehicle() instanceof ShipEntity ship) {
            return Optional.of(ship);
        }
        return player.serverLevel().getEntitiesOfClass(ShipEntity.class, new AABB(player.blockPosition()).inflate(24)).stream()
                .min(Comparator.comparingDouble(player::distanceToSqr));
    }

    private static int repair(CommandSourceStack source, ServerPlayer player) {
        Optional<ShipEntity> ship = nearest(player);
        if (ship.isEmpty() || ship.get().definition() == null) {
            source.sendFailure(Component.literal("No ship nearby"));
            return 0;
        }
        ship.get().repair(ship.get().definition(), ship.get().maxHealth());
        ship.get().setBurning(0);
        source.sendSuccess(() -> Component.literal("Repaired the ship"), false);
        return 1;
    }

    private static int fire(CommandSourceStack source, ServerPlayer player) {
        if (!(player.getVehicle() instanceof ShipEntity ship)) {
            source.sendFailure(Component.literal("You are not aboard a ship"));
            return 0;
        }
        return ship.fire(player);
    }

    private static int burst(CommandSourceStack source, ServerPlayer player) {
        if (!(player.getVehicle() instanceof ShipEntity ship)) {
            source.sendFailure(Component.literal("You are not aboard a ship"));
            return 0;
        }
        return ship.burst(player) ? 1 : 0;
    }

    private static int remove(CommandSourceStack source, ServerPlayer player) {
        Optional<ShipEntity> ship = nearest(player);
        if (ship.isEmpty()) {
            source.sendFailure(Component.literal("No ship nearby"));
            return 0;
        }
        ship.get().ejectPassengers();
        ship.get().discard();
        source.sendSuccess(() -> Component.literal("Removed the ship"), false);
        return 1;
    }
}
