package com.nerodev.grandline.crew;

import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.ship.ShipEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Crews: founding, inviting, joining, leaving, captaincy, the Jolly Roger, the crew's ship and its recruits. Every
 * rule lives here so commands, menus and tests share it; each operation returns null on success or the reason it
 * was refused.
 */
public final class Crews {
    public static final Pattern NAME = Pattern.compile("[A-Za-z0-9 '\\-]{3,24}");
    /** How long an invitation stays open, in ticks. */
    public static final long INVITE_TICKS = 6000;
    /** Emeralds it costs to recruit an islander, plus one per four points of their health. */
    public static final int RECRUIT_BASE_COST = 6;

    /** Open invitations: invited player to (crew, expiry game time). Not saved: invitations lapse on restart. */
    private static final Map<UUID, Invite> INVITES = new HashMap<>();

    private record Invite(UUID crew, long until) {}

    private Crews() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Crews::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                sync(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(EntityLeaveLevelEvent.class, event -> {
            if (event.getEntity() instanceof NpcEntity npc && npc.crew().isPresent() && npc.getRemovalReason() != null
                    && npc.getRemovalReason().shouldDestroy() && npc.getServer() != null) {
                recruitLost(npc.getServer(), npc);
            }
        });
    }

    /** Crewmates' blows (and their recruits') glance off each other. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && friendlyFire(event.getEntity(), event.getSource().getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Recruits stand by their crew: they turn on whoever attacks a crewmate, and join any fight a crewmate starts. */
    public static void rally(LivingEntity victim, LivingEntity attacker) {
        Optional<UUID> victimCrew = crewOf(victim);
        Optional<UUID> attackerCrew = crewOf(attacker);
        if (victimCrew.isPresent() && !victimCrew.equals(attackerCrew)) {
            call(victim, victimCrew.get(), attacker);
        }
        if (attackerCrew.isPresent() && !attackerCrew.equals(victimCrew)) {
            call(attacker, attackerCrew.get(), victim);
        }
    }

    private static void call(LivingEntity around, UUID crew, LivingEntity enemy) {
        for (NpcEntity recruit : around.level().getEntitiesOfClass(NpcEntity.class, around.getBoundingBox().inflate(24),
                npc -> npc.isAlive() && npc != enemy && npc.crew().equals(Optional.of(crew)))) {
            recruit.provoke(enemy);
        }
    }

    public static Optional<Crew> of(Player player) {
        MinecraftServer server = player.getServer();
        return server == null ? Optional.empty() : CrewData.get(server).of(player.getUUID());
    }

    /** Whether two players (or a player and a recruit, or two recruits) sail with the same crew. */
    public static boolean crewmates(@Nullable Entity a, @Nullable Entity b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        Optional<UUID> crewA = crewOf(a);
        return crewA.isPresent() && crewA.equals(crewOf(b));
    }

    /** The crew an entity belongs to: a player's crew, or the crew that recruited an islander. */
    public static Optional<UUID> crewOf(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return GLPlayer.data(player).crew();
        }
        if (entity instanceof NpcEntity npc) {
            return npc.crew();
        }
        return Optional.empty();
    }

    /** Copies the player's crew (from the world's crew data, which is the truth) into their synced player data. */
    public static void sync(ServerPlayer player) {
        Optional<UUID> crew = of(player).map(Crew::id);
        if (!GLPlayer.data(player).crew().equals(crew)) {
            GLPlayer.edit(player, data -> data.setCrew(crew));
        }
        of(player).ifPresent(c -> {
            if (!player.getGameProfile().getName().equals(c.members().get(player.getUUID()))) {
                c.add(player.getUUID(), player.getGameProfile().getName());
                CrewData.get(player.getServer()).changed();
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------
    // Membership
    // ---------------------------------------------------------------------------------------------------------

    @Nullable
    public static String create(ServerPlayer founder, String name) {
        CrewData data = CrewData.get(founder.getServer());
        if (data.of(founder.getUUID()).isPresent()) {
            return "You already sail with a crew. Leave it first.";
        }
        if (!NAME.matcher(name).matches()) {
            return "Crew names are 3 to 24 letters, digits, spaces, hyphens or apostrophes.";
        }
        if (data.byName(name).isPresent()) {
            return "There is already a crew called " + name + ".";
        }
        Crew crew = Crew.found(name.trim(), founder.getUUID(), founder.getGameProfile().getName());
        data.put(crew);
        sync(founder);
        return null;
    }

    @Nullable
    public static String invite(ServerPlayer captain, ServerPlayer target) {
        Optional<Crew> crew = of(captain);
        if (crew.isEmpty() || !crew.get().isCaptain(captain.getUUID())) {
            return "Only a captain can invite.";
        }
        if (of(target).isPresent()) {
            return target.getGameProfile().getName() + " already sails with a crew.";
        }
        if (crew.get().members().size() >= GLServerConfig.maxCrewSize()) {
            return "Your crew is full.";
        }
        INVITES.put(target.getUUID(), new Invite(crew.get().id(), captain.level().getGameTime() + INVITE_TICKS));
        target.sendSystemMessage(Component.literal(captain.getGameProfile().getName() + " invites you to join ").withStyle(ChatFormatting.GOLD)
                .append(name(crew.get())).append(Component.literal(". ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("[Accept]").withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/onepiece crew accept")))));
        return null;
    }

    @Nullable
    public static String accept(ServerPlayer player) {
        Invite invite = INVITES.remove(player.getUUID());
        if (invite == null || invite.until() < player.level().getGameTime()) {
            return "You have no open invitation.";
        }
        CrewData data = CrewData.get(player.getServer());
        Optional<Crew> crew = data.byId(invite.crew());
        if (crew.isEmpty()) {
            return "That crew has disbanded.";
        }
        if (data.of(player.getUUID()).isPresent()) {
            return "You already sail with a crew.";
        }
        if (crew.get().members().size() >= GLServerConfig.maxCrewSize()) {
            return "That crew is full.";
        }
        crew.get().add(player.getUUID(), player.getGameProfile().getName());
        data.changed();
        sync(player);
        announce(player.getServer(), crew.get(), Component.literal(player.getGameProfile().getName() + " joined the crew!"));
        return null;
    }

    /** Leaves the crew; a captain who leaves hands the crew to the next member, and the last one out disbands it. */
    @Nullable
    public static String leave(ServerPlayer player) {
        CrewData data = CrewData.get(player.getServer());
        Optional<Crew> crew = data.of(player.getUUID());
        if (crew.isEmpty()) {
            return "You don't sail with a crew.";
        }
        Crew c = crew.get();
        c.remove(player.getUUID());
        if (c.members().isEmpty()) {
            disbandNow(player.getServer(), c);
        } else {
            if (c.isCaptain(player.getUUID())) {
                c.setCaptain(c.members().keySet().iterator().next());
                announce(player.getServer(), c, Component.literal(c.members().get(c.captain()) + " is the new captain."));
            }
            data.changed();
            announce(player.getServer(), c, Component.literal(player.getGameProfile().getName() + " left the crew."));
        }
        sync(player);
        return null;
    }

    @Nullable
    public static String kick(ServerPlayer captain, UUID member, String memberName) {
        Optional<Crew> crew = of(captain);
        if (crew.isEmpty() || !crew.get().isCaptain(captain.getUUID())) {
            return "Only a captain can remove crewmates.";
        }
        if (member.equals(captain.getUUID()) || !crew.get().has(member)) {
            return memberName + " is not one of your crew.";
        }
        crew.get().remove(member);
        CrewData.get(captain.getServer()).changed();
        ServerPlayer kicked = captain.getServer().getPlayerList().getPlayer(member);
        if (kicked != null) {
            sync(kicked);
            kicked.sendSystemMessage(Component.literal("You were removed from " + crew.get().name() + ".").withStyle(ChatFormatting.RED));
        }
        announce(captain.getServer(), crew.get(), Component.literal(memberName + " was removed from the crew."));
        return null;
    }

    @Nullable
    public static String promote(ServerPlayer captain, UUID member) {
        Optional<Crew> crew = of(captain);
        if (crew.isEmpty() || !crew.get().isCaptain(captain.getUUID())) {
            return "Only the captain can hand over the crew.";
        }
        if (!crew.get().has(member) || member.equals(captain.getUUID())) {
            return "That player is not one of your crew.";
        }
        crew.get().setCaptain(member);
        CrewData.get(captain.getServer()).changed();
        announce(captain.getServer(), crew.get(), Component.literal(crew.get().members().get(member) + " is the new captain."));
        return null;
    }

    @Nullable
    public static String disband(ServerPlayer captain) {
        Optional<Crew> crew = of(captain);
        if (crew.isEmpty() || !crew.get().isCaptain(captain.getUUID())) {
            return "Only the captain can disband the crew.";
        }
        announce(captain.getServer(), crew.get(), Component.literal("The crew has disbanded."));
        disbandNow(captain.getServer(), crew.get());
        return null;
    }

    private static void disbandNow(MinecraftServer server, Crew crew) {
        CrewData data = CrewData.get(server);
        data.remove(crew.id());
        for (UUID member : crew.members().keySet()) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
                sync(online);
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (UUID recruit : crew.recruits()) {
                if (level.getEntity(recruit) instanceof NpcEntity npc) {
                    npc.leaveCrew();
                }
            }
            crew.ship().ifPresent(ship -> {
                if (level.getEntity(ship) instanceof ShipEntity s) {
                    s.setCrewFlag(Optional.empty());
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Flag, ship, renown
    // ---------------------------------------------------------------------------------------------------------

    @Nullable
    public static String setFlag(ServerPlayer captain, JollyRoger roger, int color) {
        Optional<Crew> crew = of(captain);
        if (crew.isEmpty() || !crew.get().isCaptain(captain.getUUID())) {
            return "Only the captain chooses the Jolly Roger.";
        }
        crew.get().setJollyRoger(roger);
        crew.get().setColor(color & 0xFFFFFF);
        CrewData.get(captain.getServer()).changed();
        crew.get().ship().ifPresent(id -> {
            if (captain.serverLevel().getEntity(id) instanceof ShipEntity ship) {
                ship.setCrewFlag(Optional.of(roger));
            }
        });
        return null;
    }

    /** Makes the ship the captain is aboard the crew's ship: it flies the crew's Jolly Roger and only crewmates steer it. */
    @Nullable
    public static String claimShip(ServerPlayer captain) {
        Optional<Crew> crew = of(captain);
        if (crew.isEmpty() || !crew.get().isCaptain(captain.getUUID())) {
            return "Only the captain can claim a ship.";
        }
        if (!(captain.getVehicle() instanceof ShipEntity ship)) {
            return "Board the ship you want to claim first.";
        }
        Optional<UUID> owner = ship.crew();
        if (owner.isPresent() && !owner.get().equals(crew.get().id())) {
            return "This ship already flies another crew's colours.";
        }
        crew.get().ship().ifPresent(old -> {
            if (captain.serverLevel().getEntity(old) instanceof ShipEntity previous && previous != ship) {
                previous.setCrew(Optional.empty());
                previous.setCrewFlag(Optional.empty());
            }
        });
        crew.get().setShip(Optional.of(ship.getUUID()));
        ship.setCrew(Optional.of(crew.get().id()));
        ship.setCrewFlag(Optional.of(crew.get().jollyRoger()));
        CrewData.get(captain.getServer()).changed();
        return null;
    }

    /** Fame from a deed by one of the crew. */
    public static void addRenown(ServerPlayer member, long amount) {
        of(member).ifPresent(crew -> {
            crew.addRenown(amount);
            CrewData.get(member.getServer()).changed();
        });
    }

    /** The crew's total bounty: the sum of its members' (online or remembered from their saved data when online). */
    public static long totalBounty(MinecraftServer server, Crew crew) {
        long total = 0;
        for (UUID member : crew.members().keySet()) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
                total += GLPlayer.data(online).bounty();
            }
        }
        return total;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Recruits
    // ---------------------------------------------------------------------------------------------------------

    public static int recruitCost(NpcEntity npc) {
        return RECRUIT_BASE_COST + (int) (npc.getMaxHealth() / 4);
    }

    /** Takes an islander into the player's crew, paid in emeralds from the player's inventory. */
    @Nullable
    public static String recruit(ServerPlayer player, NpcEntity npc) {
        Optional<Crew> crew = of(player);
        if (crew.isEmpty()) {
            return "You need a crew before anyone will join you.";
        }
        if (npc.crew().isPresent()) {
            return npc.crew().equals(Optional.of(crew.get().id())) ? "Already one of your crew." : "Already sails with another crew.";
        }
        if (!npc.recruitable(player)) {
            return "They won't follow you.";
        }
        if (crew.get().recruits().size() >= GLServerConfig.maxRecruits()) {
            return "Your crew can't take on more islanders.";
        }
        int cost = recruitCost(npc);
        if (player.getInventory().countItem(Items.EMERALD) < cost && !player.getAbilities().instabuild) {
            return "It takes " + cost + " emeralds to win them over.";
        }
        if (!player.getAbilities().instabuild) {
            ContainerHelper.clearOrCountMatchingItems(player.getInventory(),
                    stack -> stack.is(Items.EMERALD), cost, false);
        }
        crew.get().addRecruit(npc.getUUID());
        CrewData.get(player.getServer()).changed();
        npc.joinCrew(crew.get().id(), crew.get().captain());
        announce(player.getServer(), crew.get(), npc.getDisplayName().copy().append(Component.literal(" joined the crew!")));
        return null;
    }

    /** Sends every recruit of the player's crew home, wherever they now stand. */
    @Nullable
    public static String dismiss(ServerPlayer captain) {
        Optional<Crew> crew = of(captain);
        if (crew.isEmpty() || !crew.get().isCaptain(captain.getUUID())) {
            return "Only the captain can dismiss recruits.";
        }
        for (UUID recruit : List.copyOf(crew.get().recruits())) {
            crew.get().removeRecruit(recruit);
            for (ServerLevel level : captain.getServer().getAllLevels()) {
                if (level.getEntity(recruit) instanceof NpcEntity npc) {
                    npc.leaveCrew();
                }
            }
        }
        CrewData.get(captain.getServer()).changed();
        return null;
    }

    /** A recruit died (or was removed): it no longer counts against the crew. */
    public static void recruitLost(MinecraftServer server, NpcEntity npc) {
        npc.crew().flatMap(id -> CrewData.get(server).byId(id)).ifPresent(crew -> {
            if (crew.removeRecruit(npc.getUUID())) {
                CrewData.get(server).changed();
            }
        });
    }

    /** The captain an islander follows, if they are online in the same world. */
    @Nullable
    public static ServerPlayer captainOf(NpcEntity npc) {
        if (npc.getServer() == null) {
            return null;
        }
        Optional<Crew> crew = npc.crew().flatMap(id -> CrewData.get(npc.getServer()).byId(id));
        if (crew.isEmpty()) {
            return null;
        }
        ServerPlayer captain = npc.getServer().getPlayerList().getPlayer(crew.get().captain());
        return captain != null && captain.level() == npc.level() ? captain : null;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** The crew's name in its colour. */
    public static MutableComponent name(Crew crew) {
        return Component.literal(crew.name()).withStyle(style -> style.withColor(crew.color()).withBold(true));
    }

    /** Sends a message to every online member. */
    public static void announce(MinecraftServer server, Crew crew, Component message) {
        Component line = Component.literal("[").append(name(crew)).append("] ").append(message.copy().withStyle(ChatFormatting.GRAY));
        for (UUID member : crew.members().keySet()) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
                online.sendSystemMessage(line);
            }
        }
    }

    /** Whether {@code attacker} may hurt {@code victim}: crewmates (and their recruits) can't, unless the config allows it. */
    public static boolean friendlyFire(LivingEntity victim, @Nullable Entity attacker) {
        return !GLServerConfig.crewFriendlyFire() && crewmates(victim, attacker);
    }
}
