package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nerodev.grandline.bounty.Bounties;
import com.nerodev.grandline.bounty.BountyHunts;
import com.nerodev.grandline.crew.Crew;
import com.nerodev.grandline.crew.CrewData;
import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.crew.JollyRoger;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

/** {@code /onepiece crew}: found and run a crew; and {@code /onepiece bounty hunt}, which sends hunters at once. */
final class CrewCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece crew create <name>", "Found a crew and become its captain", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew info [crew]", "Show a crew: captain, members, bounty, renown, ship", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew list", "Every crew on the server", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew invite <player>", "Invite someone aboard (captain)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew accept", "Accept an invitation", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew leave", "Leave your crew", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew kick <player>", "Remove a crewmate (captain)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew captain <player>", "Hand the crew to another member (captain)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew flag <emblem> [#colour]", "Choose your Jolly Roger and crew colour (captain)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew ship", "Claim the ship you are aboard for the crew (captain)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew dismiss", "Send every recruited islander home (captain)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece crew disband confirm", "Disband your crew (captain)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece bounty hunt [players]", "Send bounty hunters after a wanted player now", PermissionTier.CREATOR));

    private static final SuggestionProvider<CommandSourceStack> CREWS = (ctx, builder) -> SharedSuggestionProvider.suggest(
            CrewData.get(ctx.getSource().getServer()).all().stream().map(Crew::name).map(name -> name.contains(" ") ? "\"" + name + "\"" : name),
            builder);
    private static final SuggestionProvider<CommandSourceStack> EMBLEMS = (ctx, builder) -> SharedSuggestionProvider.suggest(
            Arrays.stream(JollyRoger.values()).map(JollyRoger::getSerializedName), builder);

    private CrewCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("crew")
                .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> report(ctx, Crews.create(player(ctx), StringArgumentType.getString(ctx, "name")),
                                "Crew founded! Invite your crewmates with /onepiece crew invite."))))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), Crews.of(player(ctx))))
                        .then(Commands.argument("crew", StringArgumentType.string()).suggests(CREWS)
                                .executes(ctx -> info(ctx.getSource(), CrewData.get(ctx.getSource().getServer())
                                        .byName(StringArgumentType.getString(ctx, "crew"))))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> report(ctx, Crews.invite(player(ctx), EntityArgument.getPlayer(ctx, "player")), "Invitation sent."))))
                .then(Commands.literal("accept").executes(ctx -> report(ctx, Crews.accept(player(ctx)), "Welcome aboard!")))
                .then(Commands.literal("leave").executes(ctx -> report(ctx, Crews.leave(player(ctx)), "You left the crew.")))
                .then(Commands.literal("kick").then(Commands.argument("member", StringArgumentType.word()).suggests(MEMBERS)
                        .executes(ctx -> kick(ctx, StringArgumentType.getString(ctx, "member")))))
                .then(Commands.literal("captain").then(Commands.argument("member", StringArgumentType.word()).suggests(MEMBERS)
                        .executes(ctx -> promote(ctx, StringArgumentType.getString(ctx, "member")))))
                .then(Commands.literal("flag").then(Commands.argument("emblem", StringArgumentType.word()).suggests(EMBLEMS)
                        .executes(ctx -> flag(ctx, null))
                        .then(Commands.argument("color", StringArgumentType.word())
                                .executes(ctx -> flag(ctx, StringArgumentType.getString(ctx, "color"))))))
                .then(Commands.literal("ship").executes(ctx -> report(ctx, Crews.claimShip(player(ctx)), "This ship now flies your colours.")))
                .then(Commands.literal("dismiss").executes(ctx -> report(ctx, Crews.dismiss(player(ctx)), "Your recruits have gone home.")))
                .then(Commands.literal("disband")
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.literal("This disbands your crew for good. Run /onepiece crew disband confirm."));
                            return 0;
                        })
                        .then(Commands.literal("confirm").executes(ctx -> report(ctx, Crews.disband(player(ctx)), "The crew is no more.")))));

        root.then(Commands.literal("bounty").then(Commands.literal("hunt")
                .requires(source -> Permissions.has(source, PermissionTier.CREATOR))
                .executes(ctx -> hunt(ctx.getSource(), List.of(player(ctx))))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> hunt(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"))))));
    }

    /** Names of the caller's crewmates. */
    private static final SuggestionProvider<CommandSourceStack> MEMBERS = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        return SharedSuggestionProvider.suggest(player == null ? List.of()
                : Crews.of(player).map(crew -> crew.members().values().stream().toList()).orElse(List.of()), builder);
    };

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrException();
    }

    /** Reports an operation: its refusal as a failure, or {@code done}. */
    private static int report(CommandContext<CommandSourceStack> ctx, @Nullable String refusal, String done) {
        if (refusal != null) {
            ctx.getSource().sendFailure(Component.literal(refusal));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(done).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    @Nullable
    private static UUID member(Crew crew, String name) {
        for (Map.Entry<UUID, String> entry : crew.members().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static int kick(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer captain = player(ctx);
        Optional<Crew> crew = Crews.of(captain);
        UUID member = crew.map(c -> member(c, name)).orElse(null);
        return report(ctx, member == null ? name + " is not one of your crew." : Crews.kick(captain, member, name), name + " has been put ashore.");
    }

    private static int promote(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer captain = player(ctx);
        Optional<Crew> crew = Crews.of(captain);
        UUID member = crew.map(c -> member(c, name)).orElse(null);
        return report(ctx, member == null ? name + " is not one of your crew." : Crews.promote(captain, member), name + " is captain now.");
    }

    private static int flag(CommandContext<CommandSourceStack> ctx, @Nullable String color) throws CommandSyntaxException {
        String emblem = StringArgumentType.getString(ctx, "emblem");
        JollyRoger roger = JollyRoger.byName(emblem);
        if (roger == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown emblem: " + emblem));
            return 0;
        }
        ServerPlayer captain = player(ctx);
        int rgb = Crews.of(captain).map(Crew::color).orElse(0xC62828);
        if (color != null) {
            Optional<TextColor> parsed = TextColor.parseColor(color).result();
            if (parsed.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Colours look like #C62828 or red."));
                return 0;
            }
            rgb = parsed.get().getValue();
        }
        return report(ctx, Crews.setFlag(captain, roger, rgb), "Your crew now flies the " + roger.label() + ".");
    }

    private static int info(CommandSourceStack source, Optional<Crew> crew) {
        if (crew.isEmpty()) {
            source.sendFailure(Component.literal("No such crew."));
            return 0;
        }
        Crew c = crew.get();
        Text.send(source, Text.header(Component.literal("Crew: ").append(Crews.name(c))));
        Text.send(source, Text.line("Captain", c.members().getOrDefault(c.captain(), "?")));
        Text.send(source, Text.line("Members", c.members().size() + ": " + String.join(", ", c.members().values())));
        Text.send(source, Text.line("Jolly Roger", c.jollyRoger().label()));
        Text.send(source, Text.line("Bounty", Bounties.format(Crews.totalBounty(source.getServer(), c)) + " (crewmates online)"));
        Text.send(source, Text.line("Renown", c.renown()));
        Text.send(source, Text.line("Ship", c.ship().isPresent() ? "claimed" : "none"));
        Text.send(source, Text.line("Recruits", c.recruits().size()));
        return 1;
    }

    private static int list(CommandSourceStack source) {
        Text.send(source, Text.header("Crews"));
        for (Crew crew : CrewData.get(source.getServer()).all()) {
            Text.send(source, Component.literal("  ").append(Crews.name(crew)).append(Component.literal(" - captain "
                    + crew.members().getOrDefault(crew.captain(), "?") + ", " + crew.members().size() + " members, renown " + crew.renown())
                    .withStyle(ChatFormatting.GRAY)));
        }
        return CrewData.get(source.getServer()).all().size();
    }

    private static int hunt(CommandSourceStack source, Collection<ServerPlayer> players) {
        int total = 0;
        for (ServerPlayer player : players) {
            List<NpcEntity> party = BountyHunts.hunt(player);
            total += party.size();
            if (party.isEmpty()) {
                source.sendFailure(Component.literal(player.getGameProfile().getName() + " is not wanted enough to hunt, or there is no "
                        + "ground nearby for hunters to stand on."));
            }
        }
        int sent = total;
        source.sendSuccess(() -> Component.literal("Sent " + sent + " hunters"), true);
        return total;
    }
}
