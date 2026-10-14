package com.nerodev.grandline.crew;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * A pirate crew (or any band of players sailing together): its name, Jolly Roger and colour, captain, members,
 * renown, the ship it sails and the islanders it has recruited. Saved with the world in {@link CrewData}.
 */
public final class Crew {
    public static final Codec<Crew> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(c -> c.id),
            Codec.STRING.fieldOf("name").forGetter(c -> c.name),
            UUIDUtil.CODEC.fieldOf("captain").forGetter(c -> c.captain),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("members").forGetter(Crew::membersForCodec),
            JollyRoger.CODEC.optionalFieldOf("jolly_roger", JollyRoger.CLASSIC).forGetter(c -> c.jollyRoger),
            Codec.INT.optionalFieldOf("color", 0xC62828).forGetter(c -> c.color),
            Codec.LONG.optionalFieldOf("renown", 0L).forGetter(c -> c.renown),
            UUIDUtil.CODEC.optionalFieldOf("ship").forGetter(c -> c.ship),
            UUIDUtil.CODEC.listOf().optionalFieldOf("recruits", List.of()).forGetter(c -> List.copyOf(c.recruits))
    ).apply(i, Crew::new));

    private final UUID id;
    private String name;
    private UUID captain;
    private final Map<UUID, String> members = new LinkedHashMap<>();
    private JollyRoger jollyRoger;
    private int color;
    private long renown;
    private Optional<UUID> ship;
    private final Set<UUID> recruits = new LinkedHashSet<>();

    private Crew(UUID id, String name, UUID captain, Map<String, String> members, JollyRoger jollyRoger, int color, long renown,
            Optional<UUID> ship, List<UUID> recruits) {
        this.id = id;
        this.name = name;
        this.captain = captain;
        members.forEach((uuid, player) -> this.members.put(UUID.fromString(uuid), player));
        this.jollyRoger = jollyRoger;
        this.color = color;
        this.renown = renown;
        this.ship = ship;
        this.recruits.addAll(recruits);
    }

    /** A new crew with {@code captain} as its only member. */
    public static Crew found(String name, UUID captain, String captainName) {
        return new Crew(UUID.randomUUID(), name, captain, Map.of(captain.toString(), captainName), JollyRoger.CLASSIC, 0xC62828, 0L,
                Optional.empty(), List.of());
    }

    private Map<String, String> membersForCodec() {
        Map<String, String> out = new LinkedHashMap<>();
        members.forEach((uuid, player) -> out.put(uuid.toString(), player));
        return out;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public UUID captain() {
        return captain;
    }

    void setCaptain(UUID captain) {
        this.captain = captain;
    }

    public boolean isCaptain(UUID player) {
        return captain.equals(player);
    }

    /** Members by UUID, with the name each had when last seen; the captain first. */
    public Map<UUID, String> members() {
        return Collections.unmodifiableMap(members);
    }

    public boolean has(UUID player) {
        return members.containsKey(player);
    }

    void add(UUID player, String name) {
        members.put(player, name);
    }

    void remove(UUID player) {
        members.remove(player);
    }

    public JollyRoger jollyRoger() {
        return jollyRoger;
    }

    void setJollyRoger(JollyRoger jollyRoger) {
        this.jollyRoger = jollyRoger;
    }

    public int color() {
        return color;
    }

    void setColor(int color) {
        this.color = color;
    }

    public long renown() {
        return renown;
    }

    void addRenown(long amount) {
        this.renown = Math.max(0, renown + amount);
    }

    public Optional<UUID> ship() {
        return ship;
    }

    void setShip(Optional<UUID> ship) {
        this.ship = ship;
    }

    public Set<UUID> recruits() {
        return Collections.unmodifiableSet(recruits);
    }

    void addRecruit(UUID npc) {
        recruits.add(npc);
    }

    boolean removeRecruit(UUID npc) {
        return recruits.remove(npc);
    }
}
