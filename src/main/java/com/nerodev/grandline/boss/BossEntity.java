package com.nerodev.grandline.boss;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.bounty.Bounties;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.creator.Battles;
import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.npc.Reaction;
import com.nerodev.grandline.quest.Quests;
import com.nerodev.grandline.registry.GLRegistries;
import com.nerodev.grandline.weapon.WeaponItem;
import com.nerodev.grandline.weapon.WeaponKind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A boss: one entity type, given its identity, look and fight by a {@link BossDefinition}. The fight runs in phases;
 * each phase has its own moves (picked by weight from those whose range and cooldown allow), its own pace, and is
 * entered with an untouchable roar. Between moves the boss closes in or circles; it may sidestep a blow or raise a
 * guard that blunts frontal attacks and answers them. Beaten, it pays bounty, renown and standing to whoever landed
 * the final blow, and drops its signature weapon.
 */
public class BossEntity extends Monster {
    private static final EntityDataAccessor<String> BOSS = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PARTS = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> MOVE = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> MOVE_START = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.INT);

    /** The most a party can multiply a boss's health by. */
    private static final float MAX_PARTY_SCALE = 3f;
    private static final ResourceLocation PARTY_SCALING = GrandLine.id("party_scaling");

    /** How long without a fight before the boss walks home and recovers. */
    private static final int CALM_TICKS = 600;
    private static final int GRUDGE_TICKS = 2400;

    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Boss"), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);
    private String cachedId = "";
    @Nullable
    private BossDefinition cachedDefinition;

    @Nullable
    /** Tag on a boss wearing a gas mask (Krieg, once he has thrown his MH5). */
    public static final String GAS_MASK = "grandline_gas_mask";

    private BossMove move;
    private int moveTick;
    @Nullable
    private UUID moveTarget;
    private Vec3 locked = new Vec3(0, 0, 1);
    private Vec3 lockedSpot = Vec3.ZERO;
    private final Set<UUID> hits = new HashSet<>();
    private final Map<String, Long> readyAt = new HashMap<>();
    private int pause;
    private int pendingPhase;
    private long lastCombat = -CALM_TICKS;
    private int dodgeCooldown;
    private boolean engaged;
    private final Map<UUID, Long> grudges = new HashMap<>();
    /** Players who have struck the boss in this fight: they all share the credit when it falls. */
    private final Map<UUID, ServerPlayer> fighters = new HashMap<>();
    private float partyScale = 1f;
    private final List<UUID> summons = new ArrayList<>();
    private final Deque<String> history = new ArrayDeque<>();

    public BossEntity(EntityType<? extends BossEntity> type, Level level) {
        super(type, level);
        this.xpReward = 60;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    /** Spawns boss {@code id} at {@code pos}, living there. @return the boss, or null if there is no such boss */
    @Nullable
    public static BossEntity spawn(ServerLevelAccessor level, ResourceLocation id, BlockPos pos, float yaw, MobSpawnType reason) {
        BossDefinition def = level.registryAccess().registry(GLRegistries.BOSS).map(registry -> registry.get(id)).orElse(null);
        if (def == null) {
            return null;
        }
        EntityType<? extends BossEntity> type = def.body() == BossDefinition.Body.SEA_KING ? GLBosses.SEA_KING.get() : GLBosses.BOSS.get();
        BossEntity boss = type.create(level.getLevel());
        if (boss == null) {
            return null;
        }
        boss.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        boss.setYHeadRot(yaw);
        if (!boss.setDefinition(id)) {
            return null;
        }
        boss.restrictTo(pos, 24);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), reason, null);
        level.addFreshEntityWithPassengers(boss);
        return boss;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Identity
    // ---------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS, "");
        builder.define(SKIN, "marine_officer");
        builder.define(PARTS, "");
        builder.define(SCALE, 1f);
        builder.define(MOVE, "");
        builder.define(MOVE_START, 0L);
        builder.define(PHASE, 0);
    }

    @Nullable
    public ResourceLocation bossId() {
        return ResourceLocation.tryParse(entityData.get(BOSS));
    }

    @Nullable
    public BossDefinition definition() {
        String id = entityData.get(BOSS);
        if (!id.equals(cachedId)) {
            cachedId = id;
            ResourceLocation location = ResourceLocation.tryParse(id);
            cachedDefinition = location == null ? null
                    : level().registryAccess().registry(GLRegistries.BOSS).map(registry -> registry.get(location)).orElse(null);
        }
        return cachedDefinition;
    }

    /** Makes this boss {@code id}: name, look, size, stats and weapon. @return false if there is no such boss */
    public boolean setDefinition(ResourceLocation id) {
        entityData.set(BOSS, id.toString());
        BossDefinition def = definition();
        if (def == null) {
            return false;
        }
        entityData.set(SKIN, def.skin());
        entityData.set(PARTS, def.parts());
        entityData.set(SCALE, def.stats().scale());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(def.stats().health() * GLServerConfig.bossHealthMultiplier());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(def.stats().attack() * GLServerConfig.bossDamageMultiplier());
        getAttribute(Attributes.ARMOR).setBaseValue(def.stats().armor());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(def.stats().speed());
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(def.stats().knockbackResistance());
        def.weapon().ifPresent(weapon -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon)));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
        setCustomName(def.displayName().copy().withStyle(ChatFormatting.GOLD));
        bar.setColor(def.barColor());
        updateBarName();
        refreshDimensions();
        return true;
    }

    public String skin() {
        return entityData.get(SKIN);
    }

    public String parts() {
        return entityData.get(PARTS);
    }

    public float scale() {
        return entityData.get(SCALE);
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    /** The move being made (synced), or null between moves. */
    @Nullable
    public BossMove currentMove() {
        String id = entityData.get(MOVE);
        return id.isEmpty() ? null : BossMoves.get(id);
    }

    /** Ticks since the current move began, for posing the model. */
    public float moveTime(float partialTick) {
        return level().getGameTime() - entityData.get(MOVE_START) + partialTick;
    }

    public ServerBossEvent bar() {
        return bar;
    }

    /** The last few moves made, most recent last (shown by debug tools and checked by tests). */
    public List<String> history() {
        return List.copyOf(history);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(scale());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        BossDefinition def = definition();
        if (def != null) {
            def.weapon().ifPresent(weapon -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon)));
        }
        setHealth(getMaxHealth());
        return result;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Goals and hostility
    // ---------------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BossGoals.Fight(this));
        goalSelector.addGoal(3, new BossGoals.GoHome(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::hostileTo));
    }

    /** Bosses fight whoever wronged them, whoever their faction is set against, and (if aggressive) any stranger. */
    public boolean hostileTo(LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }
        Long until = grudges.get(target.getUUID());
        if (until != null && until > level().getGameTime()) {
            return true;
        }
        BossDefinition def = definition();
        if (!(target instanceof Player player) || def == null) {
            return false;
        }
        Reaction reaction = Factions.reaction(player, def.faction());
        return reaction == Reaction.HOSTILE || def.aggressive() && reaction != Reaction.FRIENDLY;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The fight
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isAlive()) {
            tickFight();
        }
    }

    private void tickFight() {
        BossDefinition def = definition();
        if (def == null) {
            return;
        }
        long now = level().getGameTime();
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            lastCombat = now;
            if (!engaged) {
                engaged = true;
                shout(def.lines().intro());
            }
        }
        int wanted = def.phaseAt(getHealth() / getMaxHealth());
        if (wanted > phase() && move != BossMoves.ROAR) {
            pendingPhase = wanted;
            startMove(BossMoves.ROAR, target);
        }
        if (move != null) {
            BossMove current = move;
            int t = moveTick++;
            current.action().tick(this, moveTarget(), t);
            if (move == current && moveTick >= current.length()) {
                finishMove();
            }
        }
        if (dodgeCooldown > 0) {
            dodgeCooldown--;
        }
        if (pause > 0 && move == null) {
            pause--;
        }
        bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 10 == 0) {
            updateBarPlayers();
            summons.removeIf(id -> !(((ServerLevel) level()).getEntity(id) instanceof LivingEntity alive) || !alive.isAlive());
        }
        if (engaged && now - lastCombat > CALM_TICKS && move == null) {
            engaged = false;
            fighters.clear();
            scaleForParty(1f);
        }
        if (engaged && tickCount % 100 == 0) {
            // A boss fought by a party grows tougher: half again its health for each fighter after the first.
            int party = level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32),
                    player -> player.isAlive() && !player.isCreative() && !player.isSpectator()).size();
            party = Math.max(party, (int) fighters.values().stream().filter(p -> !p.hasDisconnected() && p.isAlive()).count());
            float wantedScale = Math.min(MAX_PARTY_SCALE, 1f + 0.5f * Math.max(0, party - 1));
            if (wantedScale > partyScale) {
                scaleForParty(wantedScale);
            }
        }
        if (!engaged && getHealth() < getMaxHealth() && tickCount % 10 == 0) {
            heal(getMaxHealth() * 0.02f);
            int settled = def.phaseAt(getHealth() / getMaxHealth());
            if (settled < phase()) {
                setPhase(settled);
            }
        }
        if (phase() == def.phases().size() - 1 && def.phases().size() > 1 && tickCount % 5 == 0 && level() instanceof ServerLevel level) {
            Vfx.burst(level, this, position().add(0, getBbHeight() * 0.6, 0), 0xD02020, getBbWidth(), Kind.FLAME, 0);
        }
    }

    @Nullable
    private LivingEntity moveTarget() {
        if (moveTarget != null && level() instanceof ServerLevel level && level.getEntity(moveTarget) instanceof LivingEntity living
                && living.isAlive()) {
            return living;
        }
        return getTarget();
    }

    boolean inMove() {
        return move != null;
    }

    boolean resting() {
        return pause > 0;
    }

    float pace() {
        BossDefinition def = definition();
        return def == null ? 1f : def.phases().get(Math.min(phase(), def.phases().size() - 1)).pace();
    }

    /** Picks a move the boss can make now against a target {@code distance} away, by the phase's weights. */
    @Nullable
    BossMove pickMove(double distance) {
        BossDefinition def = definition();
        if (def == null || pause > 0) {
            return null;
        }
        long now = level().getGameTime();
        Map<String, Integer> weights = def.phases().get(Math.min(phase(), def.phases().size() - 1)).moves();
        List<BossMove> options = new ArrayList<>();
        List<Integer> optionWeights = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            BossMove candidate = BossMoves.get(entry.getKey());
            if (candidate == null || entry.getValue() <= 0 || !candidate.inRange(distance) || readyAt.getOrDefault(candidate.id(), 0L) > now) {
                continue;
            }
            if (candidate == BossMoves.REINFORCEMENTS && summons.size() >= 2) {
                continue;
            }
            options.add(candidate);
            optionWeights.add(entry.getValue());
            total += entry.getValue();
        }
        if (total == 0) {
            return null;
        }
        int roll = getRandom().nextInt(total);
        for (int i = 0; i < options.size(); i++) {
            roll -= optionWeights.get(i);
            if (roll < 0) {
                return options.get(i);
            }
        }
        return null;
    }

    void startMove(BossMove next, @Nullable LivingEntity target) {
        if (move == BossMoves.GUARD || move != null && move != next) {
            setNoGravity(false);
            setInvisible(false);
        }
        move = next;
        moveTick = 0;
        moveTarget = target == null ? null : target.getUUID();
        hits.clear();
        entityData.set(MOVE, next.id());
        entityData.set(MOVE_START, level().getGameTime());
        readyAt.put(next.id(), level().getGameTime() + (long) (next.cooldown() / Math.max(0.5f, pace())));
        history.addLast(next.id());
        while (history.size() > 8) {
            history.removeFirst();
        }
    }

    private void finishMove() {
        BossMove finished = move;
        move = null;
        entityData.set(MOVE, "");
        setNoGravity(false);
        setInvisible(false);
        pause = (int) (16 / Math.max(0.5f, pace()));
        if (finished == BossMoves.ROAR) {
            setPhase(pendingPhase);
        }
    }

    private void setPhase(int phase) {
        BossDefinition def = definition();
        entityData.set(PHASE, phase);
        if (def != null) {
            BossDefinition.Phase p = def.phases().get(Math.min(phase, def.phases().size() - 1));
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(def.stats().speed() * p.pace());
            if (phase > 0 && engaged) {
                shout(p.line());
            }
        }
        updateBarName();
    }

    /** Sets the boss's health to {@code scale} times its own, keeping how hurt it is. */
    void scaleForParty(float scale) {
        if (scale == partyScale) {
            return;
        }
        float fraction = getHealth() / getMaxHealth();
        var health = getAttribute(Attributes.MAX_HEALTH);
        health.removeModifier(PARTY_SCALING);
        if (scale > 1f) {
            health.addTransientModifier(new AttributeModifier(PARTY_SCALING, scale - 1f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        partyScale = scale;
        setHealth(fraction * getMaxHealth());
    }

    /** How much a party has toughened the boss (1 when fought alone). */
    public float partyScale() {
        return partyScale;
    }

    /** Starts {@code next} at once against the current target or the nearest player (for commands and tests). */
    public void perform(BossMove next) {
        LivingEntity target = getTarget();
        if (target == null) {
            target = level().getNearestPlayer(this, 24);
        }
        pause = 0;
        startMove(next, target);
    }

    /**
     * Drops the boss to the health where phase {@code phase} begins and starts the fight there, so the roar into it
     * follows on the next tick (for commands and tests).
     */
    public void forcePhase(int phase) {
        BossDefinition def = definition();
        if (def == null || phase <= 0 || phase >= def.phases().size()) {
            return;
        }
        setHealth(Math.min(getHealth(), def.phases().get(phase).threshold() * getMaxHealth() - 0.5f));
        engaged = true;
        lastCombat = level().getGameTime();
    }

    /** Whether a fight is on (the boss bar is shown and the boss does not recover). */
    public boolean engaged() {
        return engaged;
    }

    /** Jumps the current move ahead to tick {@code t} (a charge that hits a wall ends early). */
    void skipTo(int t) {
        moveTick = t;
    }

    boolean moveIsActive(int t) {
        return move != null && move.activeAt(t);
    }

    /** The direction a charge or dash was aimed in. */
    Vec3 lockedDirection() {
        return locked;
    }

    /** A spot on the ground a move aims at (a cannonball's landing), fixed when the move is wound up. */
    Vec3 lockedSpot() {
        return lockedSpot;
    }

    void lockSpot(Vec3 spot) {
        lockedSpot = spot;
    }

    void lockDirection(Vec3 direction) {
        if (direction.lengthSqr() > 1.0E-4) {
            locked = direction.normalize();
        }
    }

    /** True the first time {@code victim} is struck by the current move (so a dash hits each body once). */
    boolean firstHit(LivingEntity victim) {
        return hits.add(victim.getUUID());
    }

    Vec3 forward() {
        return Vec3.directionFromRotation(0, getYRot());
    }

    float attackDamage() {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    /** Hurts {@code victim} for {@code multiplier} times the boss's attack, as {@code type} damage. */
    void strike(LivingEntity victim, float multiplier, ResourceKey<DamageType> type) {
        DamageSource source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), this);
        if (victim.hurt(source, attackDamage() * multiplier)) {
            lastCombat = level().getGameTime();
        }
    }

    /** Everyone the boss's blows can reach within {@code radius} of {@code centre} (not its own Marines or summons). */
    List<LivingEntity> targetsAround(Vec3 centre, double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(radius, 2.5, radius),
                entity -> canHit(entity) && entity.position().distanceTo(centre) <= radius + entity.getBbWidth() / 2);
    }

    /** Everyone within {@code radius} in an arc of {@code degrees} in front of the boss. */
    List<LivingEntity> targetsInArc(double radius, double degrees) {
        Vec3 facing = forward();
        double cos = Math.cos(Math.toRadians(degrees / 2));
        return targetsAround(position(), radius).stream().filter(entity -> {
            Vec3 to = entity.position().subtract(position()).multiply(1, 0, 1);
            return to.lengthSqr() < 1 || to.normalize().dot(facing) >= cos;
        }).toList();
    }

    /** Everyone within {@code radius}, friend or foe (Kuro's Out of Bounds spares no one). */
    List<LivingEntity> everyoneAround(Vec3 centre, double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(radius, 2.5, radius),
                entity -> entity != this && entity.isAlive() && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()))
                        && entity.position().distanceTo(centre) <= radius + entity.getBbWidth() / 2);
    }

    private boolean canHit(LivingEntity entity) {
        if (entity == this || !entity.isAlive() || summons.contains(entity.getUUID())) {
            return false;
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        BossDefinition def = definition();
        return !(entity instanceof NpcEntity npc && def != null && def.faction().isPresent() && npc.faction().equals(def.faction()));
    }

    void summoned(NpcEntity npc) {
        summons.add(npc.getUUID());
    }

    /** Says a line to everyone nearby. */
    void shout(String line) {
        if (line == null || line.isEmpty() || !(level() instanceof ServerLevel level)) {
            return;
        }
        Component message = Component.literal("<").append(getDisplayName()).append("> ")
                .append(Component.literal(line).withStyle(ChatFormatting.RED));
        for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(this) < 48 * 48)) {
            player.sendSystemMessage(message);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Taking hits
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }
        if (move != null && move.shield() && moveTick < move.windup() + move.active() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            playSound(SoundEvents.ANVIL_LAND, 0.6f, 1.6f);
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && living != this) {
            if (living instanceof ServerPlayer player && !hostileTo(player) && !getTags().contains(Battles.TAG)) {
                provokedBy(player);
            }
            lastCombat = level().getGameTime();
            grudges.put(living.getUUID(), level().getGameTime() + GRUDGE_TICKS);
            if (living instanceof ServerPlayer player) {
                fighters.put(player.getUUID(), player);
            }
            BossDefinition def = definition();
            if (def != null && def.intangible() && source.is(AbilityDamage.PHYSICAL) && !LogiaFruit.canTouch(source)) {
                // A Logia body: the blow passes through smoke unless Armament Haki or Seastone backs it.
                if (level() instanceof ServerLevel level) {
                    Vfx.burst(level, this, position().add(0, getBbHeight() * 0.6, 0), 0xE8E8E8, 1.4f, Kind.SMOKE, 0);
                }
                if (living instanceof ServerPlayer player) {
                    player.displayClientMessage(Component.literal("The blow passes through smoke! Only Armament Haki or Seastone can strike a "
                            + "Logia.").withStyle(ChatFormatting.YELLOW), true);
                }
                return false;
            }
            if (move == BossMoves.PORCUPINE_CAPE && move.activeAt(moveTick) && source.getDirectEntity() == living) {
                // The spiked cape: the blow barely reaches him, and the spikes bite whoever struck.
                amount *= 0.2f;
                if (distanceTo(living) < 4.5) {
                    strike(living, 0.5f, AbilityDamage.STRIKE);
                    living.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 60, 0));
                    AbilityDamage.knockback(living, position(), 0.8, 0.25);
                }
                playSound(SoundEvents.ANVIL_LAND, 0.5f, 1.8f);
            }
            if (def != null && def.bladeproof() && cutBy(source, living)) {
                // The Chop-Chop body comes apart around the blade; only Armament Haki finds something to cut.
                amount *= 0.1f;
                if (level() instanceof ServerLevel level) {
                    Vfx.burst(level, this, position().add(0, getBbHeight() * 0.6, 0), 0xF4ECE4, 1.2f, Kind.SHARD, 0);
                }
                if (living instanceof ServerPlayer player) {
                    player.displayClientMessage(Component.literal("The blade passes right through! Only Haki can cut a Chop-Chop body.")
                            .withStyle(ChatFormatting.YELLOW), true);
                }
            }
            if (move == BossMoves.GUARD && inFront(living)) {
                amount *= 0.25f;
                playSound(SoundEvents.SHIELD_BLOCK, 1.2f, 0.7f);
                if (distanceTo(living) < 4.5) {
                    strike(living, 0.6f, AbilityDamage.STRIKE);
                    AbilityDamage.knockback(living, position(), 0.9, 0.3);
                }
                if (level() instanceof ServerLevel level) {
                    Vfx.burst(level, this, position().add(forward().scale(0.8)).add(0, getBbHeight() * 0.6, 0), 0xFFE070, 0.8f, Kind.SPARK,
                            Vfx.FLASH);
                }
            } else if (move == null && def != null && dodgeCooldown == 0 && distanceTo(living) < 5
                    && getRandom().nextFloat() < def.stats().dodge()) {
                dodge(living);
                return false;
            }
        }
        boolean hurt = super.hurt(source, amount);
        BossDefinition def = definition();
        if (hurt && move == null && attacker instanceof LivingEntity living && def != null && getRandom().nextFloat() < def.stats().guard()
                && readyAt.getOrDefault(BossMoves.GUARD.id(), 0L) <= level().getGameTime()) {
            startMove(BossMoves.GUARD, living);
        }
        return hurt;
    }

    /** Don Krieg's gas mask, put on for his MH5: poison gas and its sickness do not touch him. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (getTags().contains(GAS_MASK) && (effect.is(MobEffects.POISON) || effect.is(MobEffects.CONFUSION))) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    /**
     * {@code player} struck first: the boss's people take it as an assault (striking a Marine captain is a crime
     * that raises a bounty) and those nearby join the fight.
     */
    private void provokedBy(ServerPlayer player) {
        Optional<ResourceLocation> faction = faction();
        faction.ifPresent(f -> GLPlayer.edit(player, data -> data.addReputation(f, -Factions.ASSAULT_PENALTY * 3)));
        if (faction.map(Factions.MARINES::equals).orElse(false)) {
            Bounties.crime(player, Bounties.MARINE_ASSAULT * 4, false, "attacking a Marine captain");
        }
        Factions.alarm(this, faction, player);
    }

    /** Whether {@code source} is a cut: a slashing technique, or a blow from a blade that isn't clad in Armament Haki. */
    private static boolean cutBy(DamageSource source, LivingEntity attacker) {
        if (attacker instanceof Player player && HakiManager.armamentActive(player)) {
            return false;
        }
        if (source.is(AbilityDamage.SLASH)) {
            return true;
        }
        if (source.getDirectEntity() != attacker) {
            return false;
        }
        Item held = attacker.getMainHandItem().getItem();
        return held instanceof SwordItem || held instanceof AxeItem || held instanceof WeaponItem weapon
                && (weapon.kind() == WeaponKind.SWORD || weapon.kind() == WeaponKind.POLEARM);
    }

    private boolean inFront(LivingEntity attacker) {
        Vec3 to = attacker.position().subtract(position()).multiply(1, 0, 1);
        return to.lengthSqr() < 0.01 || to.normalize().dot(forward()) > 0.2;
    }

    /** A quick sidestep away from a blow. */
    private void dodge(LivingEntity attacker) {
        Vec3 away = position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-away.z, 0, away.x).scale(getRandom().nextBoolean() ? 1 : -1);
        setDeltaMovement(side.scale(0.9).add(away.scale(0.3)).add(0, 0.25, 0));
        dodgeCooldown = 50;
        playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1f, 0.6f);
        if (level() instanceof ServerLevel level) {
            Vfx.burst(level, this, position().add(0, 0.3, 0), 0xD8D0C0, 1f, Kind.SMOKE, 0);
        }
        history.addLast("dodge");
        while (history.size() > 8) {
            history.removeFirst();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Bar, defeat and saving
    // ---------------------------------------------------------------------------------------------------------

    private void updateBarName() {
        BossDefinition def = definition();
        if (def == null) {
            return;
        }
        BossDefinition.Phase p = def.phases().get(Math.min(phase(), def.phases().size() - 1));
        Component name = def.displayName().copy().append(Component.literal(" - ")).append(def.title());
        bar.setName(def.phases().size() > 1 && phase() > 0 ? name.copy().append(Component.literal(" (")).append(p.name()).append(")") : name);
    }

    private void updateBarPlayers() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
            if (!engaged || !player.isAlive() || player.level() != level || player.distanceToSqr(this) > 48 * 48) {
                bar.removePlayer(player);
            }
        }
        if (engaged) {
            for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(this) < 40 * 40)) {
                bar.addPlayer(player);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) {
            return;
        }
        bar.removeAllPlayers();
        BossDefinition def = definition();
        if (def == null) {
            return;
        }
        shout(def.lines().defeat());
        if (!getTags().contains(Battles.TAG)) {
            Quests.onBossDefeated(this, source.getEntity() instanceof ServerPlayer killer ? killer : null, fighters.values());
            if (getTags().contains(BossRespawns.RESIDENT_TAG) && level() instanceof ServerLevel level && hasRestriction() && bossId() != null) {
                BossRespawns.schedule(level, bossId(), getRestrictCenter());
            }
        }
        if (source.getEntity() instanceof ServerPlayer victor && !getTags().contains(Battles.TAG)) {
            BossDefinition.Rewards rewards = def.rewards();
            if (rewards.bounty() > 0) {
                Bounties.crime(victor, rewards.bounty(), false, "defeating " + def.displayName().getString());
            }
            if (rewards.renown() > 0) {
                Crews.addRenown(victor, rewards.renown());
            }
            if (!rewards.reputation().isEmpty()) {
                GLPlayer.edit(victor, data -> rewards.reputation().forEach(data::addReputation));
            }
            if (victor.getServer() != null) {
                victor.getServer().getPlayerList().broadcastSystemMessage(Component.literal(victor.getGameProfile().getName() + " has defeated ")
                        .withStyle(ChatFormatting.GOLD).append(def.displayName().copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                        .append(Component.literal("!").withStyle(ChatFormatting.GOLD)), false);
            }
        }
    }

    /** Battle-test bosses are practice and drop nothing. */
    @Override
    protected boolean shouldDropLoot() {
        return !getTags().contains(Battles.TAG);
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        ResourceLocation id = bossId();
        return id == null ? super.getDefaultLootTable()
                : ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "entities/boss/" + id.getPath()));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Boss", entityData.get(BOSS));
        tag.putInt("Phase", phase());
        if (hasRestriction()) {
            BlockPos home = getRestrictCenter();
            tag.putIntArray("Home", new int[] {home.getX(), home.getY(), home.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Boss"));
        if (id != null) {
            setDefinition(id);
        }
        int[] home = tag.getIntArray("Home");
        if (home.length == 3) {
            restrictTo(new BlockPos(home[0], home[1], home[2]), 24);
        }
        setPhase(tag.getInt("Phase"));
    }

    /** The boss's name for messages. */
    public Component bossName() {
        BossDefinition def = definition();
        return def == null ? Component.literal(String.valueOf(bossId())) : def.displayName();
    }

    public Optional<ResourceLocation> faction() {
        BossDefinition def = definition();
        return def == null ? Optional.empty() : def.faction();
    }
}
