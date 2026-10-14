package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Root of the {@code /onepiece} command tree.
 * <p>
 * No {@code /op} alias: vanilla already owns {@code /op} (grant operator), so an alias would break it.
 */
public final class OnePieceCommand {
    public static final String ROOT = "onepiece";
    static final int HELP_PAGE_SIZE = 8;

    private static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece help [page]", "Show the commands you can use", PermissionTier.PLAYER));

    private OnePieceCommand() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT)
                .executes(ctx -> sendHelp(ctx.getSource(), 1));

        root.then(Commands.literal("help")
                .executes(ctx -> sendHelp(ctx.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> sendHelp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))));

        ProfileCommand.register(root);
        IdentityCommands.register(root);
        AbilityCommands.register(root);
        FruitCommands.register(root);
        HakiCommands.register(root);
        StyleCommands.register(root);
        WeaponCommands.register(root);
        CreatorCommands.register(root);
        ShipCommands.register(root);
        IslandCommands.register(root);
        NpcCommands.register(root);
        BossCommands.register(root);
        CrewCommands.register(root);
        DebugCommand.register(root);

        event.getDispatcher().register(root);
    }

    /** Every help entry, in display order. New command classes append their entries here. */
    static List<HelpEntry> allHelpEntries() {
        List<HelpEntry> entries = new ArrayList<>(HELP);
        entries.addAll(ProfileCommand.HELP);
        entries.addAll(IdentityCommands.HELP);
        entries.addAll(FruitCommands.HELP);
        entries.addAll(HakiCommands.HELP);
        entries.addAll(StyleCommands.HELP);
        entries.addAll(WeaponCommands.HELP);
        entries.addAll(CreatorCommands.HELP);
        entries.addAll(ShipCommands.HELP);
        entries.addAll(IslandCommands.HELP);
        entries.addAll(NpcCommands.HELP);
        entries.addAll(BossCommands.HELP);
        entries.addAll(CrewCommands.HELP);
        entries.addAll(AbilityCommands.HELP);
        entries.addAll(DebugCommand.HELP);
        return entries;
    }

    static int sendHelp(CommandSourceStack source, int requestedPage) {
        PermissionTier tier = Permissions.tierOf(source);
        List<HelpEntry> visible = allHelpEntries().stream()
                .filter(entry -> tier.atLeast(entry.tier()))
                .toList();

        int pages = Math.max(1, (visible.size() + HELP_PAGE_SIZE - 1) / HELP_PAGE_SIZE);
        int page = Math.min(requestedPage, pages);
        int from = (page - 1) * HELP_PAGE_SIZE;
        int to = Math.min(from + HELP_PAGE_SIZE, visible.size());

        Text.send(source, Text.header("One Piece: Grand Line " + GrandLine.version()));
        Text.send(source, Component.literal("  Your tier: ").withStyle(ChatFormatting.GRAY)
                .append(tier.display())
                .append(Component.literal("   Page " + page + "/" + pages).withStyle(ChatFormatting.DARK_GRAY)));

        for (HelpEntry entry : visible.subList(from, to)) {
            Text.send(source, Component.literal("  ")
                    .append(Text.suggest(entry.usage(), entry.commandPrefix(), ChatFormatting.AQUA))
                    .append(Component.literal(" - " + entry.description()).withStyle(ChatFormatting.GRAY)));
        }

        if (page < pages) {
            Text.send(source, Component.literal("  ")
                    .append(Text.run("[Next page]", "/" + ROOT + " help " + (page + 1), ChatFormatting.YELLOW)));
        }
        return visible.size();
    }
}
