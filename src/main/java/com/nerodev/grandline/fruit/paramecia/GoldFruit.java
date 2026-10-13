package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

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
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Goru Goru no Mi. Control gold: it erupts from the ground as spears and fists, coats you as armour, shackles foes, and
 * rises as a golden dragon.
 */
public class GoldFruit extends DevilFruit {
    public static final int COLOR = 0xF0C030;
    public static final int PALE = 0xFFF0A0;
    static final String M = "fruit/grandline:gold";

    public static final DeferredHolder<Ability, GoldSpears> GOLD_SPEARS = ability("gold_spears", GoldSpears::new);
    public static final DeferredHolder<Ability, GoldCoat> GOLD_COAT = ability("gold_coat", GoldCoat::new);
    public static final DeferredHolder<Ability, GoldenShackle> GOLDEN_SHACKLE = ability("gold_shackle", GoldenShackle::new);
    public static final DeferredHolder<Ability, GoldFist> GOLD_FIST = ability("gold_fist", GoldFist::new);
    public static final DeferredHolder<Ability, GoldWall> GOLD_WALL = ability("gold_wall", GoldWall::new);
    public static final DeferredHolder<Ability, GoldDragon> GOLD_DRAGON = ability("gold_dragon", GoldDragon::new);
    public static final DeferredHolder<Ability, GoldenPrison> GOLDEN_PRISON = ability("gold_prison", GoldenPrison::new);

    public static final ResourceLocation DRAGON = GrandLine.id("gold_dragon");

    public GoldFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Goru Goru no Mi", COLOR)
                .abilities(GOLD_SPEARS, GOLD_COAT, GOLDEN_SHACKLE, GOLD_FIST, GOLD_WALL)
                .awakened(GOLD_DRAGON, GOLDEN_PRISON));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(DRAGON, Projectiles.piercing(AbilityDamage.BLAST,
                (p, target) -> Kit.launch(target, p.getDeltaMovement().normalize().scale(1.2).add(0, 0.6, 0))));
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Raise a line of golden spears from the ground toward where you look.
     *
     * @name Golden Spears
     */
    public static final class GoldSpears extends Ability {
        GoldSpears() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(50).stamina(9).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 10 * ctx.power(), 1.4)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.7, 0));
            }
            Vec3 dir = ctx.horizontalLook();
            for (int i = 1; i <= 5; i++) {
                Vec3 at = Kit.ground(ctx.level(), player.position().add(dir.scale(i * 2.0)), 4);
                Vfx.eruption(ctx.level(), player, at, COLOR, 1.6f, Kind.SHARD);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.CHAIN_PLACE, 1.4f, 0.7f);
        }
    }

    /**
     * Coat yourself in a shell of gold: heavy, hard armour.
     *
     * @name Gold Coat
     */
    public static final class GoldCoat extends Buffs.Toggle {
        GoldCoat() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, PALE).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(600).masteryGain(0.7),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("gold_coat_armor"), 10, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("gold_coat_toughness"), 3, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("gold_coat_kb"), 0.3, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.status(ctx.level(), ctx.player(), Vfx.StatusLook.GOLD, 6);
            ctx.sound(SoundEvents.ARMOR_EQUIP_GOLD.value(), 1.4f, 0.8f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 14 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 0.8f, Kind.SPARK);
            }
            return true;
        }
    }

    /**
     * Lock a foe in a shell of gold: it can't move or act for three and a half seconds.
     *
     * @name Golden Shackle
     */
    public static final class GoldenShackle extends Ability {
        GoldenShackle() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(220).stamina(16).mastery(M, 10).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.GOLD, 70);
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), COLOR, 0.3f, Kind.SPARK,
                        Vfx.BEAM_GLOW);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.CHAIN_PLACE, 1.6f, 0.6f);
        }
    }

    /**
     * A giant golden fist bursts from the ground beneath a foe and hurls it skyward.
     *
     * @name Gold Fist
     */
    public static final class GoldFist extends Ability {
        GoldFist() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, PALE).cooldown(160).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 14f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.3, 0));
                Vfx.eruption(ctx.level(), player, target.position(), COLOR, 3.5f, Kind.SHARD);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(SoundEvents.ANVIL_LAND, 1f, 0.7f);
        }
    }

    /**
     * Raise a wall of solid gold in front of you.
     *
     * @name Gold Wall
     */
    public static final class GoldWall extends Ability {
        GoldWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(200).stamina(14).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(3));
            Kit.place(ctx.level(), Kit.wall(center, dir, 7, 4), Blocks.GOLD_BLOCK.defaultBlockState(), 200);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), COLOR, 3f, Kind.SPARK, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ANVIL_PLACE, 1f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Raise a great golden dragon and send it roaring through everything in front of you.
     *
     * @name Golden Dragon
     */
    public static final class GoldDragon extends Ability {
        GoldDragon() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, DRAGON, 1.1, 3f, COLOR, 18f, 40);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ENDER_DRAGON_GROWL, 1f, 1.4f);
        }
    }

    /**
     * Close every foe around you in a prison of gold: none can move or act for four seconds.
     *
     * @name Golden Prison
     */
    public static final class GoldenPrison extends Ability {
        GoldenPrison() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, PALE).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                Kit.encase(target, Vfx.StatusLook.GOLD, 80);
                Kit.hit(player, target, AbilityDamage.CRUSH, 6f * ctx.power());
            }
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 10f, Kind.SPARK, Vfx.RING | Vfx.GROUND | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ANVIL_LAND, 1.6f, 0.5f);
        }
    }
}
