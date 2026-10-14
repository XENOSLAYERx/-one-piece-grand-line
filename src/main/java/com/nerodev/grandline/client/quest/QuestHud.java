package com.nerodev.grandline.client.quest;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.quest.Journey;
import com.nerodev.grandline.quest.QuestDefinition;
import com.nerodev.grandline.quest.QuestObjective;
import com.nerodev.grandline.quest.Quests;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** The tracked quest at the left edge of the screen (clear of toasts, boss bars and effects): its name and what is left to do. */
public final class QuestHud {
    public static final ResourceLocation ID = GrandLine.id("quest_tracker");
    private static final int WIDTH = 150;

    private QuestHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EFFECTS, ID, QuestHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        Journey journey = GLPlayer.data(mc.player).journey();
        Optional<ResourceLocation> tracked = journey.tracked();
        if (tracked.isEmpty()) {
            return;
        }
        QuestDefinition quest = Quests.get(mc.level.registryAccess(), tracked.get());
        if (quest == null) {
            return;
        }
        Font font = mc.font;
        int lines = 1 + quest.objectives().size();
        int x = 4;
        int y = Math.max(24, g.guiHeight() / 2 - 20 - lines * 10);
        g.fill(x - 3, y - 3, x + WIDTH + 1, y + lines * 10 + 1, 0x70000000);
        g.drawString(font, font.plainSubstrByWidth(quest.displayName().getString(), WIDTH), x, y,
                quest.category() == QuestDefinition.Category.MAIN ? 0xFFF0C050 : 0xFFF0F0D8, true);
        y += 10;
        for (int i = 0; i < quest.objectives().size(); i++) {
            QuestObjective objective = quest.objectives().get(i);
            int progress = journey.progress(tracked.get(), i);
            boolean done = progress >= objective.goal();
            String text = (done ? "✔ " : "- ") + Quests.describe(mc.level.registryAccess(), objective).getString()
                    + (objective.goal() > 1 ? " " + Math.min(progress, objective.goal()) + "/" + objective.goal() : "");
            g.drawString(font, font.plainSubstrByWidth(text, WIDTH), x, y, done ? 0xFF7AC87A : 0xFFE0E0E0, true);
            y += 10;
        }
    }

    /** The Log Pose needle: 0 points straight ahead, turning clockwise to 1; it spins when it has no heading. */
    public static float needle(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0f;
        }
        long time = mc.level == null ? 0 : mc.level.getGameTime();
        Optional<Journey.Heading> heading = GLPlayer.data(mc.player).journey().heading();
        Entity holder = entity != null ? entity : stack.getEntityRepresentation();
        if (heading.isEmpty() || holder == null) {
            return Mth.positiveModulo((time + seed) / 40f, 1f);
        }
        double toTarget = Math.atan2(heading.get().target().getZ() + 0.5 - holder.getZ(), heading.get().target().getX() + 0.5 - holder.getX())
                / (Math.PI * 2);
        double facing = Mth.positiveModulo(holder.getVisualRotationYInDegrees() / 360.0, 1.0);
        // A little wobble, as a needle floating in a glass globe has.
        double wobble = Math.sin(time * 0.15 + seed) * 0.006;
        return (float) Mth.positiveModulo(0.5 - (facing - 0.25 - toTarget) + wobble, 1.0);
    }
}
