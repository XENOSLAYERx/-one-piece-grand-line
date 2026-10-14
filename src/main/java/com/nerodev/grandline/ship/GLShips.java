package com.nerodev.grandline.ship;

import com.mojang.serialization.Codec;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Ships: the entity, the bottle they travel in, cannonballs and the cannonball's flight. */
public final class GLShips {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GrandLine.MODID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,
            GrandLine.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GrandLine.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GrandLine.MODID);

    public static final Supplier<EntityType<ShipEntity>> SHIP = ENTITIES.register("ship",
            () -> EntityType.Builder.<ShipEntity>of(ShipEntity::new, MobCategory.MISC)
                    .sized(2f, 1f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("ship"));

    /** The kind of ship in a bottle. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SHIP_TYPE = COMPONENTS.register(
            "ship_type", () -> DataComponentType.<ResourceLocation>builder().persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC).build());
    /** The hull health of the ship in a bottle. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> SHIP_HEALTH = COMPONENTS.register(
            "ship_health", () -> DataComponentType.<Float>builder().persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT).build());

    public static final DeferredItem<ShipItem> SHIP_ITEM = ITEMS.register("ship_in_a_bottle", () -> new ShipItem(new Item.Properties()));
    public static final DeferredItem<Item> CANNONBALL = ITEMS.register("cannonball", () -> new Item(new Item.Properties()) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag f) {
            tooltip.add(Component.literal("Ammunition for ship cannons: keep it in the hold or on you").withStyle(ChatFormatting.GRAY));
        }
    });

    public static final ResourceLocation CANNONBALL_SHOT = GrandLine.id("cannonball");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("ships", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.grandline.ships"))
            .icon(() -> ShipItem.of(GrandLine.id("galleon"), 1))
            .displayItems((params, output) -> {
                params.holders().lookup(GLRegistries.SHIP).ifPresent(lookup -> lookup.listElements()
                        .sorted((a, b) -> Float.compare(a.value().length(), b.value().length()))
                        .forEach(holder -> output.accept(ShipItem.of(holder.key().location(), holder.value().maxHealth()))));
                output.accept(CANNONBALL.get());
            })
            .build());

    private GLShips() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        ProjectileBehavior.Registry.register(CANNONBALL_SHOT, new Cannonball());
    }

    /** True if {@code entity} belongs to (or is aboard) the ship {@code shooter} is on. */
    static boolean ownShip(ServerPlayer shooter, Entity entity) {
        Entity ship = shooter.getVehicle();
        if (ship == null) {
            return false;
        }
        Entity root = entity instanceof ShipPart part ? part.getParent() : entity;
        return root == ship || root.getVehicle() == ship;
    }

    /**
     * A cannonball: flies in an arc, passes over its own ship, and bursts on the first creature, ship or block it
     * hits, damaging everything (and every hull) around it. Sometimes it sets a hull alight.
     */
    static final class Cannonball implements ProjectileBehavior {
        private static final double RADIUS = 2.6;

        @Override
        public boolean onHitEntity(AbilityProjectile shot, Entity target) {
            ServerPlayer owner = shot.ownerPlayer();
            if (owner == null || ownShip(owner, target) || target == owner) {
                return true;
            }
            burst(shot);
            return false;
        }

        @Override
        public void onHitBlock(AbilityProjectile shot, BlockHitResult hit) {
            shot.setPos(hit.getLocation());
            burst(shot);
        }

        @Override
        public void onExpire(AbilityProjectile shot) {
            burst(shot);
        }

        private void burst(AbilityProjectile shot) {
            ServerPlayer owner = shot.ownerPlayer();
            if (owner == null) {
                return;
            }
            ServerLevel level = (ServerLevel) shot.level();
            Vec3 at = shot.position();
            for (LivingEntity target : AbilityDamage.around(owner, at, RADIUS)) {
                if (!ownShip(owner, target)) {
                    target.invulnerableTime = 0;
                    AbilityDamage.hurt(owner, target, AbilityDamage.BLAST, shot.damage() * 0.7f, shot);
                    AbilityDamage.knockback(target, at, 0.8, 0.3);
                }
            }
            for (ShipEntity ship : level.getEntitiesOfClass(ShipEntity.class, new AABB(at, at).inflate(30))) {
                if (ship == owner.getVehicle()) {
                    continue;
                }
                boolean hitHull = false;
                for (var part : ship.getParts()) {
                    if (part instanceof ShipPart segment && segment.active() && segment.getBoundingBox().inflate(RADIUS).contains(at)) {
                        hitHull = true;
                        break;
                    }
                }
                if (hitHull) {
                    ship.hurt(AbilityDamage.source(owner, AbilityDamage.BLAST, shot), shot.damage());
                    if (level.random.nextFloat() < 0.2f) {
                        ship.setBurning(Math.max(ship.burning(), 160));
                    }
                }
            }
            boolean wet = level.getFluidState(BlockPos.containing(at)).is(FluidTags.WATER);
            Vfx.burst(level, owner, at, wet ? 0xC8E8F8 : 0xF0A040, (float) RADIUS, wet ? Kind.DROP : Kind.FLAME, Vfx.FLASH | Vfx.HEAVY);
            Vfx.burst(level, owner, at, 0x606060, (float) RADIUS * 0.8f, Kind.SMOKE, 0);
            level.playSound(null, at.x, at.y, at.z, wet ? SoundEvents.GENERIC_SPLASH : SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.4f, wet ? 0.6f : 0.9f);
        }
    }
}
