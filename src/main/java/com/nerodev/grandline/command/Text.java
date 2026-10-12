package com.nerodev.grandline.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/** Shared chat formatting for command output, so every command looks the same. */
final class Text {
    private Text() {}

    static MutableComponent header(String title) {
        return Component.literal("==== ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(" ====").withStyle(ChatFormatting.DARK_AQUA));
    }

    static MutableComponent header(Component title) {
        return Component.literal("==== ").withStyle(ChatFormatting.DARK_AQUA)
                .append(title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(" ====").withStyle(ChatFormatting.DARK_AQUA));
    }

    static MutableComponent line(String label, Component value) {
        return Component.literal("  " + label + ": ").withStyle(ChatFormatting.GRAY).append(value.copy());
    }

    static MutableComponent line(String label, Object value) {
        return line(label, Component.literal(String.valueOf(value)).withStyle(ChatFormatting.WHITE));
    }

    static MutableComponent yesNo(boolean value) {
        return Component.literal(value ? "yes" : "no").withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /** Text that types {@code command} into the chat box when clicked. */
    static MutableComponent suggest(String display, String command, ChatFormatting color) {
        return Component.literal(display).withStyle(style -> style
                .withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to type " + command))));
    }

    /** Text that runs {@code command} when clicked. */
    static MutableComponent run(String display, String command, ChatFormatting color) {
        return Component.literal(display).withStyle(style -> style
                .withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to run " + command))));
    }

    static void send(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }
}
