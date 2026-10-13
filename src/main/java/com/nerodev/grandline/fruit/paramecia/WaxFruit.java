package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.props;

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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Doru Doru no Mi. Candle wax that sets harder than steel: shackles, walls, armour and arrows. It is still wax, so fire
 * melts both the wax and its maker.
 */
public class WaxFruit extends DevilFruit {
    public static final int COLOR = 0xF4F0E8;
    public static final int SHADE = 0xC8C0A8;
    static final String M = "fruit/grandline:wax";

    public static final DeferredHolder<Ability, CandleLock> CANDLE_LOCK = ability("wax_candle_lock", CandleLock::new);
    public static final DeferredHolder<Ability, WaxArrows> ARROWS = ability("wax_arrows", WaxArrows::new);
    public static final DeferredHolder<Ability, CandleWall> CANDLE_WALL = ability("wax_candle_wall", CandleWall::new);
    public static final DeferredHolder<Ability, CandleArmor> CANDLE_ARMOR = ability("wax_candle_armor", CandleArmor::new);
    public static final DeferredHolder<Ability, ServiceSet> SERVICE_SET = ability("wax_service_set", ServiceSet::new);
    public static final DeferredHolder<Ability, CandleHouse> CANDLE_HOUSE = ability("wax_candle_house", CandleHouse::new);
    public static final DeferredHolder<Ability, CandleChampion> CHAMPION = ability("wax_candle_champion", CandleChampion::new);

    public static final ResourceLocation ARROW = GrandLine.id("wax_arrow");

    public WaxFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Doru Doru no Mi", COLOR)
                .abilities(CANDLE_LOCK, ARROWS, CANDLE_WALL, CANDLE_ARMOR, SERVICE_SET)
                .awakened(CANDLE_HOUSE, CHAMPION));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(ARROW, Projectiles.bolt(AbilityDamage.STRIKE,
                (p, target) -> Kit.root(target, Vfx.StatusLook.WAX, 25)));
    }

    /** Wax melts: fire hurts its maker half again as much. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * 1.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Seal a foe's feet in hardened wax. */
    public static final class CandleLock extends Ability {
        CandleLock() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(100).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 18).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 18).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.WAX, 90);
                Vfx.trail(ctx.level(), player, ctx.handPos(), target.position(), COLOR, 0.6f, Kind.DROP);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.HONEYCOMB_WAX_ON, 1.2f, 0.8f);
        }
    }

    /**
     * Loose three arrows of hard wax; each one pins its target where it stands.
     *
     * @name Doru Doru Arts: Arrow
     */
    public static final class WaxArrows extends Ability {
        WaxArrows() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, SHADE).cooldown(60).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.08)).normalize();
                AbilityProjectile.spawn(player, ARROW, ctx.handPos(), dir.scale(2.0), 0.3f, COLOR, 5f * ctx.power(), 25)
                        .withGravity(0.02);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ARROW_SHOOT, 1f, 0.8f);
        }
    }

    /** Raise a wall of wax harder than steel in front of you. */
    public static final class CandleWall extends Ability {
        CandleWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(200).stamina(16).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(3));
            Kit.place(ctx.level(), Kit.wall(center, dir, 7, 4), Blocks.HONEYCOMB_BLOCK.defaultBlockState(), 220);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), COLOR, 3.5f, Kind.DROP, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.HONEYCOMB_WAX_ON, 1.4f, 0.6f);
        }
    }

    /** Coat yourself in armour of hardened wax: blows hurt far less, but you move a little slower. */
    public static final class CandleArmor extends Buffs.Toggle {
        CandleArmor() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(600).mastery(M, 20).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("wax_armor_armor"), 10, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("wax_armor_toughness"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("wax_armor_knockback"), 0.5, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("wax_armor_speed"), -0.15, Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.status(ctx.level(), ctx.player(), Vfx.StatusLook.WAX, 8);
            ctx.sound(SoundEvents.HONEYCOMB_WAX_ON, 1.2f, 1f);
        }

        @Override
        protected void stopped(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.4f, Kind.SHARD, 0);
        }
    }

    /**
     * Seal a foe entirely in wax on a spinning cake stand: it cannot move or act.
     *
     * @name Candle Service Set
     */
    public static final class ServiceSet extends Ability {
        ServiceSet() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, SHADE).cooldown(260).stamina(20).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.WAX, 100);
                Kit.hit(player, target, AbilityDamage.CRUSH, 4f * ctx.power());
                Vfx.spiral(ctx.level(), target, target.position().add(0, 0.2, 0), COLOR, 1.5f, Kind.DROP);
            });
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.HONEYCOMB_WAX_ON, 1.4f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Wall yourself and everyone near you inside a great dome of wax. */
    public static final class CandleHouse extends Ability {
        CandleHouse() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, SHADE).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.place(ctx.level(), Kit.dome(player.blockPosition(), 6), Blocks.HONEYCOMB_BLOCK.defaultBlockState(), 300);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 6f, Kind.DROP, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.HONEYCOMB_WAX_ON, 2f, 0.4f);
        }
    }

    /** Build a giant suit of wax around yourself and fight from inside it. */
    public static final class CandleChampion extends Buffs.Toggle {
        CandleChampion() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(800).stamina(30).staminaPerTick(0.3f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("wax_champion_scale"), 0.8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("wax_champion_damage"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("wax_champion_armor"), 12, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("wax_champion_knockback"), 0.8,
                                    Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("wax_champion_speed"), -0.1,
                                    Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), COLOR, 3f, Kind.DROP, Vfx.HEAVY);
            ctx.sound(SoundEvents.HONEYCOMB_WAX_ON, 2f, 0.5f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.4f, Kind.DROP);
            }
            return true;
        }
    }
}
