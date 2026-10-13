package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Shima Shima no Mi. Merge with the land and command it as your own body: fists of earth, walls, ground that swallows
 * foes, and at last a whole island that fights for you.
 */
public class IslandFruit extends DevilFruit {
    public static final int COLOR = 0x5A8A4A;
    public static final int EARTH = 0x8A6A3A;
    static final String M = "fruit/grandline:island";

    public static final DeferredHolder<Ability, LandFist> LAND_FIST = ability("island_land_fist", LandFist::new);
    public static final DeferredHolder<Ability, EarthWall> EARTH_WALL = ability("island_earth_wall", EarthWall::new);
    public static final DeferredHolder<Ability, LandGrab> LAND_GRAB = ability("island_land_grab", LandGrab::new);
    public static final DeferredHolder<Ability, TerrainHeave> TERRAIN_HEAVE = ability("island_terrain_heave", TerrainHeave::new);
    public static final DeferredHolder<Ability, IslandBody> ISLAND_BODY = ability("island_body", IslandBody::new);
    public static final DeferredHolder<Ability, LivingIsland> LIVING_ISLAND = ability("island_living_island", LivingIsland::new);
    public static final DeferredHolder<Ability, IslandCrush> ISLAND_CRUSH = ability("island_crush", IslandCrush::new);

    public static final ResourceLocation ISLAND = GrandLine.id("island_living");

    public IslandFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Shima Shima no Mi", COLOR)
                .abilities(LAND_FIST, EARTH_WALL, LAND_GRAB, TERRAIN_HEAVE, ISLAND_BODY)
                .awakened(LIVING_ISLAND, ISLAND_CRUSH));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(ISLAND, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.level().random.nextFloat() < 0.5f) {
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
                Kit.launch(target, new Vec3(0, 0.8, 0));
                Vfx.eruption((ServerLevel) p.level(), p, target.position(), EARTH, 2f, Kind.SHARD);
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** A fist of earth bursts from the ground under a foe and hurls it upward. */
    public static final class LandFist extends Ability {
        LandFist() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, EARTH).cooldown(50).stamina(9).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.0, 0));
                Vfx.eruption(ctx.level(), player, target.position(), EARTH, 2.5f, Kind.SHARD);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(SoundEvents.ROOTED_DIRT_BREAK, 1.6f, 0.6f);
        }
    }

    /** Raise a wall of packed earth in front of you. */
    public static final class EarthWall extends Ability {
        EarthWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(140).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(3));
            Kit.place(ctx.level(), Kit.wall(center, dir, 7, 4), Blocks.PACKED_MUD.defaultBlockState(), 220);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), EARTH, 3f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ROOTED_DIRT_PLACE, 1.6f, 0.6f);
        }
    }

    /** The ground opens and closes around a foe's legs: it is stuck fast and crushed. */
    public static final class LandGrab extends Ability {
        LandGrab() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, EARTH).cooldown(180).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.MUD, 80);
                Kit.hit(player, target, AbilityDamage.CRUSH, 6f * ctx.power());
                Vfx.burst(ctx.level(), player, target.position(), EARTH, 1.6f, Kind.SHARD, Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.MUD_BREAK, 1.6f, 0.6f);
        }
    }

    /** Heave the land around you: every foe nearby is thrown into the air and battered by the rising ground. */
    public static final class TerrainHeave extends Ability {
        TerrainHeave() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(200).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 8f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.1, 0));
            }
            for (Vec3 at : Kit.circle(player.position(), 4, 8, 0)) {
                Vfx.eruption(ctx.level(), player, Kit.ground(ctx.level(), at, 4), EARTH, 2f, Kind.SHARD);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 0.6f);
        }
    }

    /**
     * Become one with the land: very tough, immovable, and slowly healing while you stand on the ground.
     *
     * @name Island Body
     */
    public static final class IslandBody extends Buffs.Toggle {
        IslandBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(240).stamina(12).staminaPerTick(0.15f)
                            .maxActive(600).mastery(M, 35).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("island_body_armor"), 12, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("island_body_kb"), 1, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("island_body_speed"), -0.2, Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.status(ctx.level(), ctx.player(), Vfx.StatusLook.STONE, 6);
            ctx.sound(SoundEvents.ROOTED_DIRT_PLACE, 1.4f, 0.5f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 40 == 0 && player.onGround()) {
                player.heal(1f);
                Vfx.impact(ctx.level(), player, player.position(), COLOR, 1f, Kind.LEAF);
            }
            return true;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Wake the island around you for fifteen seconds: fists of earth keep bursting up under every foe on it. */
    public static final class LivingIsland extends Ability {
        LivingIsland() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, COLOR).cooldown(1000).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, ISLAND);
            AbilityProjectile.zone(player, ISLAND, player.position(), 14 * ctx.power(), COLOR, 6f * ctx.power(), 300);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 14f, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.4f);
        }
    }

    /**
     * Bring a mountain of earth down on the spot you point at: everything there is crushed and buried.
     *
     * @name Island Crush
     */
    public static final class IslandCrush extends Ability {
        IslandCrush() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, EARTH).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 28), 8);
            for (LivingEntity target : AbilityDamage.around(player, at, 6 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 20f * ctx.power());
                Kit.root(target, Vfx.StatusLook.MUD, 40);
            }
            Vfx.eruption(ctx.level(), player, at, EARTH, 7f, Kind.SHARD);
            Vfx.burst(ctx.level(), player, at, EARTH, 6f, Kind.SMOKE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.5f);
        }
    }
}
