package com.nerodev.grandline.ship;

import com.nerodev.grandline.bounty.Bounties;
import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.crew.JollyRoger;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * A ship: a custom entity (not a re-skinned boat) whose kind comes from a {@link ShipDefinition}. The server sails it:
 * the crew member at the helm steers with the movement keys, and the hull keeps its momentum, turns slower at low
 * speed and floats at its draft. Crew stand at fixed places on deck; anyone aboard can fire a broadside toward the
 * side they look at. The hull has health, can burn, sinks at zero (spilling its cargo), and is repaired with planks.
 * <p>
 * Controls: right-click to board, sneak to leave; sneak-right-click opens the hold; planks repair, a water bucket puts
 * out a fire, and sneak-right-click with an empty bottle (on an empty ship) bottles it up to carry.
 */
public class ShipEntity extends Entity {
    public static final int MAX_PARTS = 8;
    public static final int MAX_CARGO = 54;
    public static final ResourceLocation DEFAULT_TYPE = ResourceLocation.fromNamespaceAndPath("grandline", "raft");

    private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> BURNING = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HURT = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> PORT_READY = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> STARBOARD_READY = SynchedEntityData.defineId(ShipEntity.class,
            EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> BURST_READY = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.LONG);
    /** The Jolly Roger of the crew that owns this ship, or "" for the ship's own colours. */
    private static final EntityDataAccessor<String> CREW_FLAG = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.STRING);

    /** How fast a cannonball leaves the muzzle, in blocks per tick. */
    static final double CANNON_SPEED = 2.2;
    /** Ticks between two Coups de Burst. */
    public static final int BURST_COOLDOWN = 600;

    private final ShipPart[] parts = new ShipPart[MAX_PARTS];
    private final SimpleContainer cargo = new SimpleContainer(MAX_CARGO);
    private Optional<UUID> crew = Optional.empty();
    /** Commands the ship when islanders crew it (server only); null for a ship sailed by players. */
    @Nullable
    private ShipCaptain captain;
    private double speed;
    private int burstTicks;
    /** Yaw the ship turned this tick, passed on to the crew. */
    private float yawDelta;
    /** Client: yaw change received since the last tick. */
    private float pendingYaw;

    @Nullable
    private String cachedType;
    @Nullable
    private ShipDefinition cachedDefinition;

    public ShipEntity(EntityType<? extends ShipEntity> type, Level level) {
        super(type, level);
        for (int i = 0; i < MAX_PARTS; i++) {
            parts[i] = new ShipPart(this, i);
        }
        // Reserve one id per part right after the ship's own, the way the ender dragon does, so clients agree.
        setId(ENTITY_COUNTER.getAndAdd(MAX_PARTS + 1) + 1);
        this.blocksBuilding = true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Definition and data
    // ---------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, DEFAULT_TYPE.toString());
        builder.define(HEALTH, 40f);
        builder.define(BURNING, 0);
        builder.define(HURT, 0);
        builder.define(SPEED, 0f);
        builder.define(PORT_READY, 0L);
        builder.define(STARBOARD_READY, 0L);
        builder.define(BURST_READY, 0L);
        builder.define(CREW_FLAG, "");
    }

    public ResourceLocation typeId() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(TYPE));
        return id == null ? DEFAULT_TYPE : id;
    }

    /** Sets the kind of ship (and restores full health). */
    public void setShipType(ResourceLocation type) {
        entityData.set(TYPE, type.toString());
        ShipDefinition definition = definition();
        if (definition != null) {
            setHealth(definition.maxHealth());
        }
    }

    /** This ship's definition, or null if its type is not (or no longer) defined. */
    @Nullable
    public ShipDefinition definition() {
        String type = entityData.get(TYPE);
        if (!type.equals(cachedType)) {
            cachedType = type;
            ResourceLocation id = ResourceLocation.tryParse(type);
            cachedDefinition = id == null ? null
                    : level().registryAccess().registry(GLRegistries.SHIP).map(registry -> registry.get(id)).orElse(null);
            configure();
        }
        return cachedDefinition;
    }

    private void configure() {
        refreshDimensions();
        ShipDefinition def = cachedDefinition;
        int segments = def == null ? 1 : def.segments();
        for (int i = 0; i < MAX_PARTS; i++) {
            parts[i].configure(def != null && i < segments, def == null ? 1 : def.width(),
                    def == null ? 1 : def.hullHeight() + (def.castle() ? 0.6f : 0.4f));
        }
        if (def != null) {
            placeParts(def);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (TYPE.equals(key)) {
            cachedType = null;
        }
    }

    public float health() {
        return entityData.get(HEALTH);
    }

    public void setHealth(float health) {
        ShipDefinition def = definition();
        entityData.set(HEALTH, Mth.clamp(health, 0, def == null ? health : def.maxHealth()));
    }

    public float maxHealth() {
        ShipDefinition def = definition();
        return def == null ? 1 : def.maxHealth();
    }

    public int burning() {
        return entityData.get(BURNING);
    }

    public void setBurning(int ticks) {
        entityData.set(BURNING, Math.max(0, ticks));
    }

    public float speed() {
        return entityData.get(SPEED);
    }

    public int hurtTicks() {
        return entityData.get(HURT);
    }

    /** Game time the side's cannons are loaded again (side 1 port, -1 starboard). */
    public long readyAt(int side) {
        return entityData.get(side > 0 ? PORT_READY : STARBOARD_READY);
    }

    public long burstReadyAt() {
        return entityData.get(BURST_READY);
    }

    public SimpleContainer cargo() {
        return cargo;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        ShipDefinition def = definition();
        return def == null ? EntityDimensions.fixed(1f, 1f) : EntityDimensions.fixed(def.length(), def.hullHeight() + 1f);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        ShipDefinition def = definition();
        return def == null ? super.getBoundingBoxForCulling() : getBoundingBox().expandTowards(0, def.mastHeight() + 3, 0).inflate(1);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Parts
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) {
                parts[i].setId(id + i + 1);
            }
        }
    }

    /** Lays the hull segments out along the keel. */
    private void placeParts(ShipDefinition def) {
        int segments = def.segments();
        Vec3 forward = forward(getYRot());
        float spacing = def.length() / segments;
        for (int i = 0; i < MAX_PARTS; i++) {
            ShipPart part = parts[i];
            double offset = i < segments ? (i - (segments - 1) / 2.0) * spacing : 0;
            Vec3 at = position().add(forward.scale(offset));
            part.xo = part.getX();
            part.yo = part.getY();
            part.zo = part.getZ();
            part.xOld = part.getX();
            part.yOld = part.getY();
            part.zOld = part.getZ();
            part.setPos(at.x, at.y, at.z);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Geometry helpers
    // ---------------------------------------------------------------------------------------------------------

    /** Unit vector toward the bow for a heading. */
    public static Vec3 forward(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    /** Unit vector toward port (the left side) for a heading. */
    public static Vec3 port(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(r), 0, Mth.sin(r));
    }

    /** A point given in ship space (x to port, y up from the keel, z toward the bow) in the world. */
    public Vec3 toWorld(Vec3 local) {
        return toWorld(local, position(), getYRot());
    }

    public static Vec3 toWorld(Vec3 local, Vec3 origin, float yaw) {
        return origin.add(port(yaw).scale(local.x)).add(0, local.y, 0).add(forward(yaw).scale(local.z));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Ticking
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        ShipDefinition def = definition();
        super.tick();
        if (def == null) {
            if (!level().isClientSide && tickCount > 40) {
                discard();
            }
            return;
        }
        if (level().isClientSide) {
            yawDelta = pendingYaw;
            pendingYaw = 0;
            clientTick(def);
        } else {
            serverTick(def);
        }
        placeParts(def);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        pendingYaw += Mth.wrapDegrees(yRot - getYRot());
        super.lerpTo(x, y, z, yRot, xRot, steps);
    }

    /** The crew member at the helm, if any. */
    @Nullable
    public Entity helmsman() {
        return getFirstPassenger();
    }

    private void serverTick(ShipDefinition def) {
        float forwardInput = 0;
        float turnInput = 0;
        if (helmsman() instanceof Player helm) {
            forwardInput = Mth.clamp(helm.zza, -1, 1);
            turnInput = Mth.clamp(helm.xxa, -1, 1);
        } else if (captain != null) {
            if (captain.crew().isEmpty()) {
                abandoned(def);
            } else if (captain.lonely()) {
                sailAway();
                return;
            } else {
                ShipCaptain.Helm helm = captain.steer(def);
                forwardInput = helm.forward();
                turnInput = helm.turn();
            }
        }
        double surface = waterSurface(def);
        boolean afloat = !Double.isNaN(surface);
        boolean storm = afloat && level().isThundering()
                && level().isRainingAt(BlockPos.containing(getX(), surface + def.hullHeight() + 2, getZ()));
        float max = def.maxSpeed() * (burstTicks > 0 ? 2.6f : storm ? 0.85f : 1f);
        if (afloat || burstTicks > 0) {
            if (forwardInput > 0.01f) {
                speed += def.acceleration() * forwardInput;
            } else if (forwardInput < -0.01f) {
                speed += def.acceleration() * forwardInput * 0.6;
            } else {
                speed *= 0.992;
            }
        } else {
            speed *= 0.6;
        }
        if (burstTicks > 0) {
            burstTicks--;
            speed = Math.max(speed, max * 0.9);
        }
        speed = Mth.clamp(speed, -def.maxSpeed() * 0.3, max);

        float turnFactor = 0.3f + 0.7f * Math.min(1f, (float) Math.abs(speed) / def.maxSpeed() * 1.5f);
        float turn = afloat ? -turnInput * def.turnRate() * turnFactor : 0;
        if (storm) {
            // Gusts push the bow about.
            turn += Mth.sin(tickCount * 0.031f + getId()) * 0.45f;
        }
        setYRot(Mth.wrapDegrees(getYRot() + turn));
        yawDelta = turn;

        Vec3 horizontal = forward(getYRot()).scale(speed);
        double vy = getDeltaMovement().y;
        if (afloat && burstTicks < 30) {
            double swell = storm ? Math.sin(tickCount * 0.12 + getId()) * 0.32 + Math.sin(tickCount * 0.05) * 0.18
                    : Math.sin(tickCount * 0.07) * 0.04;
            double target = surface - def.draft() + swell;
            vy = Mth.clamp((target - getY()) * 0.3, -0.25, 0.25);
        } else {
            vy = Math.max(vy - 0.06, -2);
        }

        if (horizontal.lengthSqr() > 1.0E-6 && blocked(def, horizontal, 0)) {
            if (Math.abs(speed) > def.maxSpeed() * 0.6) {
                hurt(damageSources().flyIntoWall(), (float) Math.abs(speed) * 30f);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.NEUTRAL, 1f, 0.6f);
            }
            speed = -speed * 0.2;
            horizontal = Vec3.ZERO;
        }
        if (!afloat && vy < 0 && blocked(def, new Vec3(0, vy, 0), vy)) {
            vy = 0;
        }
        setDeltaMovement(horizontal.x, vy, horizontal.z);
        setPos(getX() + horizontal.x, getY() + vy, getZ() + horizontal.z);
        entityData.set(SPEED, (float) speed);

        int hurt = entityData.get(HURT);
        if (hurt > 0) {
            entityData.set(HURT, hurt - 1);
        }
        if (storm && tickCount % 40 == 0 && random.nextFloat() < 0.004f) {
            strikeMast(def);
        }
        int burning = burning();
        if (burning > 0) {
            boolean rain = level().isRainingAt(BlockPos.containing(getX(), getY() + def.hullHeight() + 1, getZ()));
            setBurning(burning - (rain ? 6 : 1));
            if (tickCount % 20 == 0) {
                hurt(damageSources().onFire(), 2f);
            }
        }
    }

    /** Height of the water surface under the ship's centre, or NaN if it is not in water. */
    public double waterSurface(ShipDefinition def) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int top = Mth.floor(getY() + def.draft() + 1.5);
        int bottom = Mth.floor(getY() - 1.5);
        for (int y = top; y >= bottom; y--) {
            pos.set(getX(), y, getZ());
            FluidState fluid = level().getFluidState(pos);
            if (fluid.is(FluidTags.WATER)) {
                return y + fluid.getHeight(level(), pos);
            }
        }
        return Double.NaN;
    }

    /**
     * Whether moving the hull by {@code delta} runs into blocks or another ship. Only the hull above the waterline
     * counts for sideways moves, so ships cross shallows but stop at the shore.
     */
    private boolean blocked(ShipDefinition def, Vec3 delta, double vertical) {
        double waterline = getY() + def.draft() + 0.15;
        for (ShipPart part : parts) {
            if (!part.active()) {
                continue;
            }
            AABB box = part.getBoundingBox().move(delta).deflate(0.05);
            if (vertical == 0) {
                box = new AABB(box.minX, Math.max(box.minY, waterline), box.minZ, box.maxX, Math.max(box.maxY, waterline + 0.1), box.maxZ);
            }
            if (!level().noCollision(this, box)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return other.canBeCollidedWith() && !(other instanceof ShipPart part && part.getParent() == this)
                && !isPassengerOfSameVehicle(other);
    }

    private void clientTick(ShipDefinition def) {
        float speed = speed();
        Vec3 forward = forward(getYRot());
        if (Math.abs(speed) > 0.05f && tickCount % 2 == 0) {
            double waterline = getY() + def.draft() + 0.1;
            Vec3 stern = position().add(forward.scale(-def.length() / 2));
            Vec3 bow = position().add(forward.scale(def.length() / 2));
            for (int i = 0; i < 2; i++) {
                double spread = (random.nextDouble() - 0.5) * def.width();
                Vec3 side = port(getYRot()).scale(spread);
                level().addParticle(ParticleTypes.SPLASH, stern.x + side.x, waterline, stern.z + side.z, 0, 0.1, 0);
                level().addParticle(ParticleTypes.BUBBLE, stern.x + side.x, waterline - 0.3, stern.z + side.z, 0, 0, 0);
                level().addParticle(ParticleTypes.SPLASH, bow.x + side.x * 0.4, waterline, bow.z + side.z * 0.4, 0, 0.2, 0);
            }
        }
        if (burning() > 0) {
            for (int i = 0; i < 3; i++) {
                Vec3 local = new Vec3((random.nextDouble() - 0.5) * def.width(), def.hullHeight() + random.nextDouble() * 0.6,
                        (random.nextDouble() - 0.5) * def.length());
                Vec3 at = toWorld(local);
                level().addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, 0, 0.04, 0);
                if (random.nextInt(3) == 0) {
                    level().addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.3, at.z, 0, 0.06, 0);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Crew
    // ---------------------------------------------------------------------------------------------------------

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        ShipDefinition def = definition();
        return def != null && getPassengers().size() < def.crew();
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        ShipDefinition def = definition();
        if (def == null || !hasPassenger(passenger)) {
            return;
        }
        List<Vec3> seats = def.seats();
        int index = Math.min(getPassengers().indexOf(passenger), seats.size() - 1);
        Vec3 at = toWorld(seats.get(Math.max(0, index)));
        move.accept(passenger, at.x, at.y, at.z);
        if (yawDelta != 0 && (!(passenger instanceof Player player) || level().isClientSide && player.isLocalPlayer())) {
            passenger.setYRot(passenger.getYRot() + yawDelta);
            passenger.setYHeadRot(passenger.getYHeadRot() + yawDelta);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        ShipDefinition def = definition();
        if (def == null) {
            return super.getDismountLocationForPassenger(passenger);
        }
        return passenger.position().add(0, 0.2, 0);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Interaction
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ShipDefinition def = definition();
        if (def == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean client = level().isClientSide;
        if (captain != null && !captain.crew().isEmpty()) {
            if (!client) {
                player.displayClientMessage(Component.literal("Her crew will not let you aboard: clear the deck first")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (player.isSecondaryUseActive()) {
            if (stack.is(Items.GLASS_BOTTLE)) {
                if (!client) {
                    bottle(player, stack);
                }
                return InteractionResult.sidedSuccess(client);
            }
            if (def.storageRows() > 0) {
                if (!client) {
                    openCargo(player, def);
                }
                return InteractionResult.sidedSuccess(client);
            }
            return InteractionResult.PASS;
        }
        if (stack.is(ItemTags.PLANKS) && health() < def.maxHealth()) {
            if (!client) {
                repair(def, Math.max(4f, def.maxHealth() * 0.05f));
                stack.consume(1, player);
                level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1f, 0.9f);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (stack.is(Items.WATER_BUCKET) && burning() > 0) {
            if (!client) {
                setBurning(0);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1f, 1f);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (stack.is(GLShips.CANNONBALL.get())) {
            if (!client) {
                ItemStack left = cargo.addItem(stack.copy());
                int loaded = stack.getCount() - left.getCount();
                stack.shrink(loaded);
                player.displayClientMessage(Component.literal("Loaded " + loaded + " cannonballs into the hold"), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (canAddPassenger(player) && !player.isPassenger()) {
            if (!client && crew.isPresent() && getPassengers().isEmpty() && !Crews.crewOf(player).equals(crew)) {
                player.displayClientMessage(Component.literal("This ship flies another crew's colours: its crew must take the helm first"),
                        true);
                return InteractionResult.FAIL;
            }
            if (!client) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(client);
        }
        return InteractionResult.PASS;
    }

    private void openCargo(Player player, ShipDefinition def) {
        int rows = Mth.clamp(def.storageRows(), 1, 6);
        MenuType<?> menu = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ChestMenu(menu, id, inventory, cargo, rows),
                def.displayName().copy().append(" - Hold")));
    }

    /** Restores {@code amount} hull health. */
    public void repair(ShipDefinition def, float amount) {
        setHealth(health() + amount);
        if (level() instanceof ServerLevel level) {
            Vfx.burst(level, null, position().add(0, def.hullHeight(), 0), 0xC8A070, def.width() / 2, Kind.SHARD, 0);
        }
    }

    /** Puts an empty ship back in a bottle (its cargo spills on the deck). */
    private void bottle(Player player, ItemStack bottle) {
        if (isVehicle()) {
            player.displayClientMessage(Component.literal("Everyone has to leave the ship first").withStyle(ChatFormatting.RED), true);
            return;
        }
        Containers.dropContents(level(), this, cargo);
        ItemStack ship = ShipItem.of(typeId(), health());
        bottle.consume(1, player);
        if (!player.getInventory().add(ship)) {
            player.drop(ship, false);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1f, 0.8f);
        discard();
    }

    @Override
    public ItemStack getPickResult() {
        return ShipItem.of(typeId(), maxHealth());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Damage
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker != null && hasPassenger(attacker) && source.getDirectEntity() == attacker) {
            return false;
        }
        if (attacker instanceof Player player && player.getAbilities().instabuild && player.isShiftKeyDown()
                && source.getDirectEntity() == player) {
            ejectPassengers();
            discard();
            return true;
        }
        float scaled = source.is(DamageTypeTags.IS_EXPLOSION) || source.getDirectEntity() instanceof AbilityProjectile ? amount
                : amount * 0.5f;
        if (source.is(DamageTypeTags.IS_FIRE)) {
            setBurning(Math.max(burning(), 200));
        }
        setHealth(health() - scaled);
        entityData.set(HURT, 10);
        if (captain != null) {
            captain.attackedBy(attacker);
        }
        if (health() <= 0) {
            sink(source);
        }
        return true;
    }

    /** The hull gives way: the crew is thrown off, the cargo spills and the wreck sinks. */
    public void sink(@Nullable DamageSource source) {
        ShipDefinition def = definition();
        ejectPassengers();
        Containers.dropContents(level(), this, cargo);
        if (level() instanceof ServerLevel level && def != null) {
            int planks = Math.max(2, (int) (def.length() * def.width() / 6));
            spawnAtLocation(new ItemStack(Items.OAK_PLANKS, Math.min(64, planks)), def.hullHeight());
            spawnAtLocation(new ItemStack(Items.STICK, Math.min(64, planks / 2 + 1)), def.hullHeight());
            Vfx.burst(level, null, position().add(0, def.hullHeight(), 0), 0x8A6A44, def.length() / 2, Kind.SHARD, Vfx.RING | Vfx.HEAVY);
            Vfx.burst(level, null, position().add(0, def.hullHeight(), 0), 0x505050, def.length() / 3, Kind.SMOKE, 0);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.NEUTRAL, 1.5f, 0.5f);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1f, 0.6f);
            Component name = def.displayName();
            if (source != null && source.getEntity() instanceof ServerPlayer killer) {
                killer.displayClientMessage(Component.literal("You sank the ").append(name).withStyle(ChatFormatting.GOLD), false);
                if (def.sail() == SailStyle.MARINE) {
                    Bounties.crime(killer, Bounties.MARINE_SHIP_SUNK, false, "sinking a Marine ship");
                }
                if (captain != null) {
                    Factions.onShipSunk(killer, captain.faction());
                }
                Crews.addRenown(killer, Math.max(1, (long) def.maxHealth() / 20));
            }
        }
        discard();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Cannons and the Coup de Burst
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Fires a broadside from the side {@code gunner} is looking toward. Each cannon takes one cannonball from the hold
     * or, failing that, from the gunner (creative gunners need none).
     *
     * @return how many cannons fired
     */
    public int fire(ServerPlayer gunner) {
        ShipDefinition def = definition();
        if (def == null || def.cannons() <= 0) {
            gunner.displayClientMessage(Component.literal("This ship has no cannons").withStyle(ChatFormatting.RED), true);
            return 0;
        }
        Vec3 look = gunner.getLookAngle();
        Vec3 port = port(getYRot());
        int side = look.dot(port) >= 0 ? 1 : -1;
        long now = level().getGameTime();
        if (now < readyAt(side)) {
            gunner.displayClientMessage(Component.literal((side > 0 ? "Port" : "Starboard") + " cannons are reloading"), true);
            return 0;
        }
        int fired = 0;
        for (Vec3 local : def.cannonPositions(side)) {
            if (!takeCannonball(gunner)) {
                break;
            }
            Vec3 muzzle = toWorld(local);
            Vec3 aim = port.scale(side).add(0, Mth.clamp(look.y, -0.25, 0.5) + 0.06, 0).add(forward(getYRot()).scale(look.dot(
                    forward(getYRot())) * 0.3)).normalize();
            AbilityProjectile.spawn(gunner, GLShips.CANNONBALL_SHOT, muzzle, aim.scale(CANNON_SPEED).add(getDeltaMovement()), 0.45f, 0x2A2A2E,
                    def.cannonDamage(), 60).withGravity(0.025);
            ServerLevel level = (ServerLevel) level();
            Vfx.impact(level, this, muzzle.add(aim.scale(0.6)), 0xF0C060, 1.2f, Kind.SPARK);
            Vfx.impact(level, this, muzzle.add(aim.scale(0.9)), 0xD8D8D8, 1.6f, Kind.SMOKE);
            fired++;
        }
        if (fired == 0) {
            gunner.displayClientMessage(Component.literal("No cannonballs aboard").withStyle(ChatFormatting.RED), true);
            return 0;
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.65f);
        entityData.set(side > 0 ? PORT_READY : STARBOARD_READY, now + def.cannonReload());
        return fired;
    }

    private boolean takeCannonball(ServerPlayer gunner) {
        if (gunner.getAbilities().instabuild) {
            return true;
        }
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack stack = cargo.getItem(i);
            if (stack.is(GLShips.CANNONBALL.get())) {
                stack.shrink(1);
                cargo.setChanged();
                return true;
            }
        }
        int slot = gunner.getInventory().findSlotMatchingItem(new ItemStack(GLShips.CANNONBALL.get()));
        if (slot >= 0) {
            gunner.getInventory().removeItem(slot, 1);
            return true;
        }
        return false;
    }

    /** Coup de Burst: the helmsman blasts the ship into the air and far ahead. @return true if it went off */
    public boolean burst(ServerPlayer helmsman) {
        ShipDefinition def = definition();
        if (def == null || !def.burst() || helmsman() != helmsman) {
            return false;
        }
        long now = level().getGameTime();
        if (now < burstReadyAt()) {
            helmsman.displayClientMessage(Component.literal("The cola tanks are still refilling"), true);
            return false;
        }
        entityData.set(BURST_READY, now + BURST_COOLDOWN);
        burstTicks = 50;
        speed = def.maxSpeed() * 2.4;
        setDeltaMovement(getDeltaMovement().x, 0.95, getDeltaMovement().z);
        ServerLevel level = (ServerLevel) level();
        Vec3 stern = toWorld(new Vec3(0, def.hullHeight() * 0.5, -def.length() / 2));
        Vfx.burst(level, this, stern, 0x6AC8F0, 3f, Kind.FLAME, Vfx.FLASH | Vfx.SHOCKWAVE);
        Vfx.burst(level, this, stern, 0xE8F4FF, 2.5f, Kind.SMOKE, Vfx.HEAVY);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2f, 0.5f);
        helmsman.displayClientMessage(Component.literal("COUP DE BURST!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Ships crewed by islanders
    // ---------------------------------------------------------------------------------------------------------

    /** The captain of the islanders crewing this ship; null for a ship sailed by players (or taken from its crew). */
    @Nullable
    public ShipCaptain captain() {
        return captain;
    }

    /** Puts the ship under the command of an islander crew of {@code encounter}'s kind, sailing {@code course}. */
    public void commandBy(Encounter encounter, float course) {
        captain = new ShipCaptain(this, encounter, course);
        setCrewFlag(encounter.flag());
    }

    /**
     * The crew fires the {@code side} broadside (1 port, -1 starboard) at {@code target}: the guns are laid for the
     * range, a little wide, and reload more slowly than a player crew's.
     *
     * @return how many cannons fired
     */
    public int fireAt(LivingEntity gunner, int side, Vec3 target) {
        ShipDefinition def = definition();
        long now = level().getGameTime();
        if (def == null || def.cannons() <= 0 || now < readyAt(side) || !(level() instanceof ServerLevel level)) {
            return 0;
        }
        int fired = 0;
        for (Vec3 local : def.cannonPositions(side)) {
            Vec3 muzzle = toWorld(local);
            Vec3 flat = target.subtract(muzzle).multiply(1, 0, 1);
            double distance = Math.max(1, flat.length());
            // Rise enough for the ball to fall onto the target at this range (flight time ~ distance / speed).
            double ticks = distance / CANNON_SPEED;
            double rise = (target.y - muzzle.y + 0.5 * 0.025 * ticks * ticks * 1.05) / distance;
            Vec3 aim = flat.normalize().add(0, rise, 0);
            ShipShot shot = new ShipShot(level, gunner, this, def.cannonDamage());
            shot.setPos(muzzle);
            shot.shoot(aim.x, aim.y, aim.z, (float) CANNON_SPEED, 2.5f);
            shot.setDeltaMovement(shot.getDeltaMovement().add(getDeltaMovement()));
            level.addFreshEntity(shot);
            Vfx.impact(level, this, muzzle.add(flat.normalize().scale(0.6)), 0xF0C060, 1.2f, Kind.SPARK);
            Vfx.impact(level, this, muzzle.add(flat.normalize().scale(0.9)), 0xD8D8D8, 1.6f, Kind.SMOKE);
            fired++;
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 0.65f);
        entityData.set(side > 0 ? PORT_READY : STARBOARD_READY, now + def.cannonReload() * 3L / 2);
        return fired;
    }

    /** The last of the crew has fallen: the ship drifts, anyone's for the taking. */
    private void abandoned(ShipDefinition def) {
        captain = null;
        if (level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(this) < 48 * 48)) {
                player.sendSystemMessage(Component.literal("The ").append(def.displayName())
                        .append(" has lost her crew: she is yours for the taking.").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** No one has come near for a long while: the ship and her crew sail off over the horizon. */
    private void sailAway() {
        for (Entity passenger : List.copyOf(getPassengers())) {
            if (!(passenger instanceof Player)) {
                passenger.discard();
            }
        }
        discard();
    }

    /** Lightning finds the mast: the ship catches fire and everyone aboard is shaken. */
    private void strikeMast(ShipDefinition def) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
        if (bolt == null) {
            return;
        }
        bolt.moveTo(getX(), getY() + def.hullHeight() + def.mastHeight(), getZ());
        bolt.setVisualOnly(true);
        level().addFreshEntity(bolt);
        setBurning(Math.max(burning(), 200));
        hurt(damageSources().lightningBolt(), def.maxHealth() * 0.05f);
        for (Entity passenger : getPassengers()) {
            passenger.hurt(damageSources().lightningBolt(), 3f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Saving
    // ---------------------------------------------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Type")) {
            entityData.set(TYPE, tag.getString("Type"));
        }
        if (tag.contains("Health")) {
            entityData.set(HEALTH, tag.getFloat("Health"));
        }
        setBurning(tag.getInt("Burning"));
        cargo.fromTag(tag.getList("Cargo", 10), registryAccess());
        crew = tag.hasUUID("Crew") ? Optional.of(tag.getUUID("Crew")) : Optional.empty();
        entityData.set(CREW_FLAG, tag.getString("CrewFlag"));
        captain = tag.contains("Captain") ? ShipCaptain.load(this, tag.getCompound("Captain")) : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Type", entityData.get(TYPE));
        tag.putFloat("Health", health());
        tag.putInt("Burning", burning());
        tag.put("Cargo", cargo.createTag(registryAccess()));
        crew.ifPresent(id -> tag.putUUID("Crew", id));
        tag.putString("CrewFlag", entityData.get(CREW_FLAG));
        if (captain != null) {
            tag.put("Captain", captain.save());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Crew
    // ---------------------------------------------------------------------------------------------------------

    /** The crew that claimed this ship (server side). */
    public Optional<UUID> crew() {
        return crew;
    }

    public void setCrew(Optional<UUID> crew) {
        this.crew = crew;
    }

    /** The Jolly Roger this ship flies instead of its own colours, if a crew claimed it. */
    public Optional<JollyRoger> crewFlag() {
        return Optional.ofNullable(JollyRoger.byName(entityData.get(CREW_FLAG)));
    }

    public void setCrewFlag(Optional<JollyRoger> flag) {
        entityData.set(CREW_FLAG, flag.map(JollyRoger::getSerializedName).orElse(""));
    }
}
