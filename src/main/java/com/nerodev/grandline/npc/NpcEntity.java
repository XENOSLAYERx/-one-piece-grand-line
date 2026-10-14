package com.nerodev.grandline.npc;

import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.quest.Quests;
import com.nerodev.grandline.registry.GLRegistries;
import com.nerodev.grandline.weapon.GunItem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * An islander: Marine, pirate, revolutionary, bounty hunter, villager or merchant. One entity type serves them all;
 * the {@link NpcDefinition} it is spawned with decides its looks, stats, gear, behaviour, words and wares. The AI is
 * rebuilt from the definition's {@link NpcRole} whenever the definition is set.
 */
public class NpcEntity extends PathfinderMob implements Merchant {
    /** Chance an islander drops each piece of the gear their definition gives them. */
    public static final float EQUIPMENT_DROP_CHANCE = 0.06f;

    private static final EntityDataAccessor<String> NPC = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> AIMING = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);

    /** How long being wronged is remembered, in ticks. */
    public static final int GRUDGE_TICKS = 1200;

    private String cachedId = "";
    @Nullable
    private NpcDefinition cachedDefinition;
    private final Map<UUID, Long> grudges = new HashMap<>();
    private long nextSpeech;
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    private long offersDay = -1;
    private boolean offersForMember;
    @Nullable
    private UUID crewId;
    @Nullable
    private UUID quarry;
    private int lostTrail;
    private int huntTicks;

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
        }
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.ARMOR, 0)
                .add(NeoForgeMod.SWIM_SPEED, 1);
    }

    /**
     * Spawns an islander of kind {@code id} standing at {@code pos} and living there.
     *
     * @return the islander, or null if no such kind exists
     */
    @Nullable
    public static NpcEntity spawn(ServerLevelAccessor level, ResourceLocation id, BlockPos pos, float yaw, MobSpawnType reason) {
        NpcEntity npc = GLNpcs.NPC.get().create(level.getLevel());
        if (npc == null) {
            return null;
        }
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        npc.setYHeadRot(yaw);
        if (!npc.setDefinition(id)) {
            return null;
        }
        npc.setHome(pos);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), reason, null);
        npc.setPersistenceRequired();
        level.addFreshEntityWithPassengers(npc);
        return npc;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Definition
    // ---------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NPC, "");
        builder.define(SKIN, "villager_a");
        builder.define(AIMING, false);
    }

    @Nullable
    public ResourceLocation npcId() {
        return ResourceLocation.tryParse(entityData.get(NPC));
    }

    /** The islander's kind, or null if it is unknown (for example a removed datapack). */
    @Nullable
    public NpcDefinition definition() {
        String id = entityData.get(NPC);
        if (!id.equals(cachedId)) {
            cachedId = id;
            ResourceLocation location = ResourceLocation.tryParse(id);
            cachedDefinition = location == null ? null
                    : level().registryAccess().registry(GLRegistries.NPC).map(registry -> registry.get(location)).orElse(null);
        }
        return cachedDefinition;
    }

    /** Makes this islander one of kind {@code id}: name, looks, stats and AI. @return false if there is no such kind */
    public boolean setDefinition(ResourceLocation id) {
        entityData.set(NPC, id.toString());
        NpcDefinition def = definition();
        if (def == null) {
            return false;
        }
        entityData.set(SKIN, def.skin());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(def.health());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(def.attack());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(def.speed());
        getAttribute(Attributes.ARMOR).setBaseValue(def.armor());
        getAttribute(NeoForgeMod.SWIM_SPEED).setBaseValue(def.swimmer() ? 2.2 : 1);
        setCustomName(def.displayName().copy().withStyle(style -> style.withColor(Factions.color(level().registryAccess(), def.faction()))));
        if (!level().isClientSide) {
            rebuildGoals(def);
        }
        offers = null;
        return true;
    }

    public String skin() {
        return entityData.get(SKIN);
    }

    public Optional<ResourceLocation> faction() {
        NpcDefinition def = definition();
        return def == null ? Optional.empty() : def.faction();
    }

    public NpcRole role() {
        NpcDefinition def = definition();
        return def == null ? NpcRole.CIVILIAN : def.role();
    }

    public boolean aiming() {
        return entityData.get(AIMING);
    }

    void setAiming(boolean aiming) {
        entityData.set(AIMING, aiming);
    }

    /** Where this islander lives: the centre of their patrol, post or stroll. */
    public BlockPos home() {
        return hasRestriction() ? getRestrictCenter() : blockPosition();
    }

    public void setHome(BlockPos home) {
        NpcDefinition def = definition();
        int radius = def == null ? 8 : role().patrols() ? def.patrolRadius() + 4 : role().fighter() ? def.patrolRadius() : def.wanderRadius();
        restrictTo(home, Math.max(2, radius));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        NpcDefinition def = definition();
        if (def != null) {
            for (Map.Entry<EquipmentSlot, Item> entry : def.equipment().entrySet()) {
                setItemSlot(entry.getKey(), new ItemStack(entry.getValue()));
                setDropChance(entry.getKey(), EQUIPMENT_DROP_CHANCE);
            }
            setLeftHanded(false);
        }
        setHealth(getMaxHealth());
        return result;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Behaviour
    // ---------------------------------------------------------------------------------------------------------

    private void rebuildGoals(NpcDefinition def) {
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        if (def.role().fighter()) {
            if (getMainHandItem().getItem() instanceof GunItem || def.equipment().get(EquipmentSlot.MAINHAND) instanceof GunItem) {
                goalSelector.addGoal(2, new NpcGoals.Shoot(this));
            } else {
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
            }
            goalSelector.addGoal(3, new NpcGoals.FollowCaptain(this));
            goalSelector.addGoal(3, new NpcGoals.Track(this));
            goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8));
            if (def.role().patrols()) {
                goalSelector.addGoal(5, new NpcGoals.Patrol(this));
            } else {
                goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
            }
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::hostileTo));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, NpcEntity.class, 10, true, false, this::hostileTo));
            if (def.role().patrols()) {
                targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                        target -> target instanceof Enemy && !(target instanceof Creeper)));
            }
        } else {
            goalSelector.addGoal(1, new PanicGoal(this, 1.3));
            goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LivingEntity.class, 10f, 0.9, 1.2, this::fears));
            goalSelector.addGoal(2, new NpcGoals.Trade(this));
            goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.7));
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.55));
        }
    }

    /** Whether this islander, a fighter, wants to attack {@code target}. */
    public boolean hostileTo(LivingEntity target) {
        if (target == this || !target.isAlive() || Crews.crewmates(this, target)) {
            return false;
        }
        if (holdsGrudge(target) || target.getUUID().equals(quarry)) {
            return true;
        }
        if (target instanceof Player player) {
            return Factions.reaction(player, faction()) == Reaction.HOSTILE;
        }
        if (target instanceof NpcEntity other) {
            return Factions.enemies(level().registryAccess(), faction(), other.faction());
        }
        return false;
    }

    /** Whether this islander, a civilian or merchant, runs from {@code entity}. */
    public boolean fears(LivingEntity entity) {
        if (entity == this || !entity.isAlive()) {
            return false;
        }
        if (holdsGrudge(entity) || entity instanceof Enemy) {
            return true;
        }
        if (entity instanceof Player player) {
            return Factions.reaction(player, faction()) == Reaction.HOSTILE;
        }
        return entity instanceof NpcEntity other && other.role().fighter() && other.getTarget() != null
                && !(other.getTarget() instanceof Enemy);
    }

    /** How this islander feels about a player. */
    public Reaction reactionTo(Player player) {
        if (Crews.crewmates(this, player)) {
            return Reaction.FRIENDLY;
        }
        return holdsGrudge(player) || player.getUUID().equals(quarry) ? Reaction.HOSTILE : Factions.reaction(player, faction());
    }

    public boolean holdsGrudge(LivingEntity entity) {
        Long until = grudges.get(entity.getUUID());
        return until != null && until > level().getGameTime();
    }

    /** Remembers being wronged by {@code enemy}; fighters turn on them at once. */
    public void provoke(LivingEntity enemy) {
        if (enemy == this || !enemy.isAlive() || Crews.crewmates(this, enemy)) {
            return;
        }
        grudges.put(enemy.getUUID(), level().getGameTime() + GRUDGE_TICKS);
        if (role().fighter() && (getTarget() == null || !getTarget().isAlive())) {
            setTarget(enemy);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount % 100 == 0 && !grudges.isEmpty()) {
            long now = level().getGameTime();
            for (Iterator<Long> it = grudges.values().iterator(); it.hasNext(); ) {
                if (it.next() <= now) {
                    it.remove();
                }
            }
        }
        NpcDefinition def = definition();
        if (def != null && def.swimmer() && isInWater()) {
            setAirSupply(getMaxAirSupply());
        }
        if (!level().isClientSide && quarry != null && tickCount % 20 == 0) {
            followTrail();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Hunting and crews
    // ---------------------------------------------------------------------------------------------------------

    /** Sets this islander on {@code target}'s trail: no home, no rest until the target is caught or the trail goes cold. */
    public void hunt(ServerPlayer target) {
        quarry = target.getUUID();
        lostTrail = 0;
        huntTicks = 0;
        clearRestriction();
        provoke(target);
    }

    @Nullable
    public UUID quarry() {
        return quarry;
    }

    /** The hunted player, if they are around. */
    @Nullable
    ServerPlayer quarryPlayer() {
        if (quarry == null || getServer() == null) {
            return null;
        }
        ServerPlayer target = getServer().getPlayerList().getPlayer(quarry);
        return target != null && target.isAlive() && target.level() == level() && !target.isSpectator() && !target.isCreative()
                ? target : null;
    }

    private void followTrail() {
        ServerPlayer target = quarryPlayer();
        boolean near = target != null && distanceToSqr(target) < 128 * 128;
        lostTrail = near ? 0 : lostTrail + 20;
        huntTicks += 20;
        if (near && (getTarget() == null || !getTarget().isAlive()) && distanceToSqr(target) < 40 * 40) {
            setTarget(target);
        }
        if (lostTrail > 600 || huntTicks > 24000) {
            discard();
        }
    }

    public Optional<UUID> crew() {
        return Optional.ofNullable(crewId);
    }

    /** Whether {@code player} could win this islander over: a fighter, not hunting, not set against them, and no deserter. */
    public boolean recruitable(Player player) {
        if (!role().fighter() || crewId != null || quarry != null || reactionTo(player) == Reaction.HOSTILE) {
            return false;
        }
        return faction().map(f -> !f.equals(Factions.MARINES) || Factions.member(player, Factions.MARINES)).orElse(true);
    }

    /** Joins a crew: no more home or beat, they follow the captain and fight beside the crew. */
    public void joinCrew(UUID crew, UUID captain) {
        crewId = crew;
        grudges.remove(captain);
        clearRestriction();
        if (getTarget() != null && Crews.crewmates(this, getTarget())) {
            setTarget(null);
        }
        setPersistenceRequired();
    }

    /** Leaves the crew and settles where they stand. */
    public void leaveCrew() {
        crewId = null;
        if (isPassenger()) {
            stopRiding();
        }
        setHome(blockPosition());
    }

    /** Fires the held gun at {@code target}. */
    void shoot(LivingEntity target, GunItem gun) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = getEyePosition().add(getLookAngle().scale(0.6));
        Vec3 aim = target.getEyePosition().subtract(0, target.getBbHeight() * 0.25, 0).subtract(from);
        double spread = 0.04 + aim.length() * 0.004;
        Vec3 velocity = aim.normalize().add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread)
                .normalize().scale(gun.speed() * 0.9);
        NpcShot shot = new NpcShot(level, this, gun.damage() * 0.6f);
        shot.setPos(from.x, from.y, from.z);
        shot.setDeltaMovement(velocity);
        level.addFreshEntity(shot);
        swing(InteractionHand.MAIN_HAND);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.35f, 1.8f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 0.8f, 0.6f);
        level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 6, 0.08, 0.08, 0.08, 0.01);
    }

    /** A point of this islander's beat: {@code index} of six spread around home, on the ground. */
    BlockPos beat(int index) {
        NpcDefinition def = definition();
        int radius = def == null ? 8 : def.patrolRadius();
        double angle = (index / 6.0) * Mth.TWO_PI + (getUUID().getLeastSignificantBits() & 255) / 255.0;
        BlockPos home = home();
        int x = home.getX() + (int) Math.round(Math.cos(angle) * radius);
        int z = home.getZ() + (int) Math.round(Math.sin(angle) * radius);
        return level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, home.getY(), z));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Talk and trade
    // ---------------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        NpcDefinition def = definition();
        if (def == null || !isAlive() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Reaction reaction = reactionTo(player);
        getLookControl().setLookAt(player);
        if (player.isSecondaryUseActive() && def.role().fighter() && player instanceof ServerPlayer recruiter && crewId == null) {
            String refusal = Crews.recruit(recruiter, this);
            if (refusal != null) {
                recruiter.displayClientMessage(Component.literal(refusal).withStyle(ChatFormatting.RED), true);
                say(player, reaction);
            } else {
                say(player, Reaction.FRIENDLY);
            }
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer talker && reaction != Reaction.HOSTILE && getTarget() == null && Quests.interact(talker, this)) {
            return InteractionResult.CONSUME;
        }
        if (def.role() == NpcRole.MERCHANT && reaction != Reaction.HOSTILE && tradingPlayer == null && getTarget() == null) {
            MerchantOffers available = offersFor(player);
            if (!available.isEmpty()) {
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
                say(player, reaction);
                return InteractionResult.CONSUME;
            }
        }
        say(player, reaction);
        return InteractionResult.CONSUME;
    }

    /** Says one of this islander's lines to {@code player}. */
    public void say(Player player, Reaction reaction) {
        NpcDefinition def = definition();
        long now = level().getGameTime();
        if (def == null || now < nextSpeech) {
            return;
        }
        String line = def.lines().pick(reaction, random);
        if (line == null) {
            return;
        }
        nextSpeech = now + 20;
        player.sendSystemMessage(Component.literal("<").append(getDisplayName()).append("> ")
                .append(Component.literal(line).withStyle(reaction.color())));
    }

    /** The offers shown to {@code player}: members of the merchant's faction see more, friends pay less. */
    public MerchantOffers offersFor(Player player) {
        NpcDefinition def = definition();
        boolean member = def != null && def.faction().isPresent() && Factions.member(player, def.faction().get());
        long day = level().getDayTime() / 24000L;
        if (offers == null || offersDay != day || offersForMember != member) {
            offers = new MerchantOffers();
            offersDay = day;
            offersForMember = member;
            if (def != null) {
                for (NpcDefinition.Trade trade : def.trades()) {
                    if (trade.membersOnly() && !member) {
                        continue;
                    }
                    offers.add(new MerchantOffer(new ItemCost(trade.buy(), trade.buyCount()),
                            trade.buyB().map(item -> new ItemCost(item, trade.buyBCount())),
                            new ItemStack(trade.sell(), trade.sellCount()), trade.maxUses(), 1, 0.05f));
                }
            }
        }
        boolean friendly = Factions.reaction(player, faction()) == Reaction.FRIENDLY;
        for (MerchantOffer offer : offers) {
            offer.setSpecialPriceDiff(friendly ? -Math.max(0, offer.getItemCostA().count() / 5) : 0);
        }
        return offers;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            offers = new MerchantOffers();
        }
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {}

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        if (tradingPlayer instanceof ServerPlayer player) {
            Factions.onTrade(player, this);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {}

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {}

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.EXPERIENCE_ORB_PICKUP;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Misc
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && tradingPlayer != null) {
            tradingPlayer.closeContainer();
            tradingPlayer = null;
        }
        return hurt;
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
        tag.putString("Npc", entityData.get(NPC));
        BlockPos home = home();
        tag.putIntArray("Home", new int[] {home.getX(), home.getY(), home.getZ()});
        if (crewId != null) {
            tag.putUUID("Crew", crewId);
        }
        if (quarry != null) {
            tag.putUUID("Quarry", quarry);
            tag.putInt("HuntTicks", huntTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Npc"));
        if (id != null) {
            setDefinition(id);
        }
        int[] home = tag.getIntArray("Home");
        if (home.length == 3) {
            setHome(new BlockPos(home[0], home[1], home[2]));
        }
        crewId = tag.hasUUID("Crew") ? tag.getUUID("Crew") : null;
        quarry = tag.hasUUID("Quarry") ? tag.getUUID("Quarry") : null;
        huntTicks = tag.getInt("HuntTicks");
        if (crewId != null || quarry != null) {
            clearRestriction();
        }
    }
}
