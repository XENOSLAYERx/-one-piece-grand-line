package com.nerodev.grandline.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nerodev.grandline.registry.DefinitionType;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Command arguments that reference entries of a Grand Line content registry. */
final class RegistryArguments {
    private static final Dynamic2CommandExceptionType UNKNOWN = new Dynamic2CommandExceptionType(
            (type, id) -> Component.literal("Unknown " + type + ": " + id));

    private RegistryArguments() {}

    /**
     * Suggests registry ids. Matches the typed text against the full id or just the path, so typing {@code gum}
     * suggests {@code grandline:gum_gum}. Runs on the server, so it always reflects the server's datapacks.
     */
    static <T> SuggestionProvider<CommandSourceStack> suggest(DefinitionType<T> type) {
        return (ctx, builder) -> {
            String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (ResourceLocation id : type.sortedIds(ctx.getSource().registryAccess())) {
                if (id.toString().startsWith(typed) || id.getPath().startsWith(typed)) {
                    builder.suggest(id.toString());
                }
            }
            return builder.buildFuture();
        };
    }

    /** Reads a {@link ResourceLocationArgument} and resolves it against the registry, or fails the command. */
    static <T> Map.Entry<ResourceLocation, T> get(CommandContext<CommandSourceStack> ctx, String argument,
            DefinitionType<T> type) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, argument);
        return type.find(ctx.getSource().registryAccess(), id)
                .orElseThrow(() -> UNKNOWN.create(type.commandName(), id));
    }
}
