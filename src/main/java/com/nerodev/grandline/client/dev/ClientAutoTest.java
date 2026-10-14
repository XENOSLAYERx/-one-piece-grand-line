package com.nerodev.grandline.client.dev;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.client.quest.QuestJournalScreen;
import com.nerodev.grandline.client.screen.AbilityScreen;
import com.nerodev.grandline.client.screen.GuideScreen;
import com.nerodev.grandline.network.AbilityKeyPayload;
import com.nerodev.grandline.network.DodgePayload;
import com.nerodev.grandline.network.GuardPayload;
import com.nerodev.grandline.network.OpenGuidePayload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Developer/CI tool: runs a plain-text script against a live client so visuals (HUD, particles, animations) can be
 * checked with screenshots. Enabled only when the {@code grandline.autotest} system property points to a script;
 * it does nothing in normal play.
 *
 * <pre>
 * wait 40                    # ticks
 * cmd onepiece unlock all    # server command as the player, op level 4
 * press 1 / release 1        # ability slot key (1-5), same packet as the real key
 * look 90 10                 # yaw pitch
 * body 40                    # turn the body this many degrees from the head (three-quarter views)
 * guard on|off               # raise or lower the guard (same packet as the real key)
 * dodge                      # dodge left (same packet as the real key)
 * camera front|back|first
 * jump
 * attack                     # left-click whatever is under the crosshair
 * menu / close               # ability screen
 * guide Haki|Armament Haki    # the Guide on a category (and entry), as a creator
 * shot name                  # screenshots/name.png
 * quit
 * </pre>
 */
public final class ClientAutoTest {
    private static final CommandSource LOG_SOURCE = new CommandSource() {
        @Override
        public void sendSystemMessage(Component message) {
            GrandLine.LOGGER.info("[autotest] > {}", message.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    };

    private static Deque<String> steps;
    private static int waitTicks = 60;
    private static boolean started;

    private ClientAutoTest() {}

    public static boolean init() {
        String path = System.getProperty("grandline.autotest");
        if (path == null || path.isBlank()) {
            return false;
        }
        try {
            List<String> lines = Files.readAllLines(Path.of(path));
            steps = new ArrayDeque<>();
            for (String line : lines) {
                String trimmed = line.strip();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    steps.add(trimmed);
                }
            }
            GrandLine.LOGGER.info("[autotest] loaded {} steps from {}", steps.size(), path);
            return true;
        } catch (IOException e) {
            GrandLine.LOGGER.error("[autotest] cannot read script {}", path, e);
            return false;
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (steps == null || mc.player == null || mc.level == null) {
            return;
        }
        if (!started) {
            started = true;
            GrandLine.LOGGER.info("[autotest] in world, starting script");
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        while (steps != null && !steps.isEmpty() && waitTicks == 0) {
            String step = steps.poll();
            GrandLine.LOGGER.info("[autotest] {}", step);
            try {
                run(mc, step);
            } catch (RuntimeException e) {
                GrandLine.LOGGER.error("[autotest] step failed: {}", step, e);
            }
        }
    }

    private static void run(Minecraft mc, String step) {
        String[] parts = step.split("\\s+", 2);
        String arg = parts.length > 1 ? parts[1] : "";
        switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "wait" -> waitTicks = Integer.parseInt(arg);
            case "cmd" -> serverCommand(mc, arg);
            case "press" -> PacketDistributor.sendToServer(new AbilityKeyPayload(Integer.parseInt(arg) - 1, true));
            case "release" -> PacketDistributor.sendToServer(new AbilityKeyPayload(Integer.parseInt(arg) - 1, false));
            case "look" -> {
                String[] angles = arg.split("\\s+");
                mc.player.setYRot(Float.parseFloat(angles[0]));
                mc.player.setXRot(Float.parseFloat(angles[1]));
                mc.player.yHeadRot = mc.player.getYRot();
                mc.player.yBodyRot = mc.player.getYRot();
            }
            case "body" -> {
                // Standing still, the body keeps whatever yaw it has as long as it stays within 75 degrees of the head.
                mc.player.yBodyRot = mc.player.getYRot() + Float.parseFloat(arg);
                mc.player.yBodyRotO = mc.player.yBodyRot;
            }
            case "guard" -> PacketDistributor.sendToServer(new GuardPayload("on".equals(arg)));
            case "dodge" -> PacketDistributor.sendToServer(new DodgePayload((byte) 0, (byte) 1));
            case "camera" -> mc.options.setCameraType(switch (arg) {
                case "front" -> CameraType.THIRD_PERSON_FRONT;
                case "back" -> CameraType.THIRD_PERSON_BACK;
                default -> CameraType.FIRST_PERSON;
            });
            case "jump" -> mc.player.jumpFromGround();
            case "attack" -> {
                // A real left click: the server sees an ordinary melee hit on whatever is under the crosshair.
                if (mc.crosshairPickEntity != null && mc.gameMode != null) {
                    mc.gameMode.attack(mc.player, mc.crosshairPickEntity);
                }
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
            case "menu" -> mc.setScreen(new AbilityScreen());
            case "journal" -> mc.setScreen(new QuestJournalScreen());
            case "guide" -> {
                String[] page = arg.split("\\|", 2);
                mc.setScreen(new GuideScreen(new OpenGuidePayload(true, "", "", 0, 0L)).showing(page[0],
                        page.length > 1 ? page[1] : null));
            }
            case "close" -> mc.setScreen(null);
            case "shot" -> Screenshot.grab(mc.gameDirectory, arg + ".png", mc.getMainRenderTarget(),
                    message -> GrandLine.LOGGER.info("[autotest] {}", message.getString()));
            case "quit" -> {
                GrandLine.LOGGER.info("[autotest] done");
                steps = null;
                mc.stop();
            }
            default -> GrandLine.LOGGER.warn("[autotest] unknown step {}", step);
        }
    }

    private static void serverCommand(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                // Feedback goes to the log (the CI client hides chat so screenshots stay clean).
                CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSource(LOG_SOURCE);
                server.getCommands().performPrefixedCommand(source, command);
            }
        });
    }
}
