package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.forms;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mogu Mogu no Mi. Become a mole: dig through soft ground as if it were water, burst up under foes, and drop them
 * into pits. Earth, sand, gravel and clay give way; stone does not.
 */
public class MoleFruit extends ZoanFruit {
    public static final int COLOR = 0x5A4A4A;
    public static final int PINK = 0xE8A8A8;
    static final String M = "fruit/grandline:mole";

    /**
     * @hybrid Mole-Woman | Grow digging claws: you dig much faster and hit harder.
     * @beast Mole Form | Become a mole: low to the ground, quick, and digs faster still.
     */
    static final ZoanForms FORMS = forms("mole", COLOR, () -> SoundEvents.ROOTED_DIRT_BREAK, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.BLOCK_BREAK_SPEED, 1.5, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 2, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.BLOCK_BREAK_SPEED, 3, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.2, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(0.8f, 0.6f, 0.45f));

    public static final DeferredHolder<Ability, MoleClaws> MOLE_CLAWS = ability("mole_claws", MoleClaws::new);
    public static final DeferredHolder<Ability, Burrow> BURROW = ability("mole_burrow", Burrow::new);
    public static final DeferredHolder<Ability, DigDown> DIG_DOWN = ability("mole_dig_down", DigDown::new);
    public static final DeferredHolder<Ability, MolePistol> MOLE_PISTOL = ability("mole_pistol", MolePistol::new);
    public static final DeferredHolder<Ability, PitTrap> PIT_TRAP = ability("mole_pit_trap", PitTrap::new);
    public static final DeferredHolder<Ability, Earthquake> EARTHQUAKE = ability("mole_earthquake", Earthquake::new);
    public static final DeferredHolder<Ability, Ambush> AMBUSH = ability("mole_ambush", Ambush::new);

    public MoleFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Mogu Mogu no Mi", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), MOLE_CLAWS, BURROW, DIG_DOWN, MOLE_PISTOL, PIT_TRAP)
                .awakened(EARTHQUAKE, AMBUSH), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    /** Ground a mole can dig through: air, plants, and anything a shovel takes that isn't a container or machine. */
    static boolean soft(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || (state.is(BlockTags.MINEABLE_WITH_SHOVEL) && !state.hasBlockEntity());
    }

    /** The farthest open spot along {@code dir} reachable by digging only through soft ground. */
    @Nullable
    static Vec3 tunnel(ServerPlayer player, Vec3 dir, double range) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        Vec3 best = null;
        for (double d = 1; d <= range; d += 0.5) {
            Vec3 to = from.add(dir.scale(d));
            AABB box = player.getBoundingBox().move(to.subtract(from));
            if (!BlockPos.betweenClosedStream(box.deflate(0.01)).allMatch(p -> soft(level, p))) {
                break;
            }
            if (level.noCollision(player, box)) {
                best = to;
            }
        }
        return best;
    }

    static void emerge(ServerPlayer player, Vec3 to) {
        Vec3 from = player.position();
        player.teleportTo(to.x, to.y, to.z);
        player.resetFallDistance();
        Vfx.burst(player.serverLevel(), player, from, 0x8A6A4A, 1.4f, Kind.SHARD, Vfx.GROUND);
        Vfx.burst(player.serverLevel(), player, to, 0x8A6A4A, 1.6f, Kind.SHARD, Vfx.GROUND);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Rake with digging claws. Needs a form.
     *
     * @name Mole Claws
     */
    public static final class MoleClaws extends Ability {
        MoleClaws() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(25).stamina(6).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.2, 45)) {
                ZoanKit.maul(player, target, 7f * FORMS.boost(player) * ctx.power(), 2, 0);
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.2)), 0x8A6A4A, 1.2f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.CLAW.get(), 1f, 0.8f);
        }
    }

    /** Dig straight ahead through soft ground (earth, sand, gravel) and come out the other side, up to ten blocks. */
    public static final class Burrow extends Ability {
        Burrow() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(60).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return tunnel(player, horizontal(player), 10) == null ? Optional.of(Component.literal("The ground ahead is too hard"))
                    : Optional.empty();
        }

        static Vec3 horizontal(ServerPlayer player) {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vec3 to = tunnel(ctx.player(), horizontal(ctx.player()), 10);
            if (to != null) {
                emerge(ctx.player(), to);
            }
            ctx.sound(SoundEvents.ROOTED_DIRT_BREAK, 1.4f, 0.8f);
        }
    }

    /** Dig straight down through soft ground into the open space below it. */
    public static final class DigDown extends Ability {
        DigDown() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, COLOR).cooldown(80).stamina(8).mastery(M, 10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return tunnel(player, new Vec3(0, -1, 0), 10) == null ? Optional.of(Component.literal("The ground below is too hard"))
                    : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vec3 to = tunnel(ctx.player(), new Vec3(0, -1, 0), 10);
            if (to != null) {
                emerge(ctx.player(), to);
            }
            ctx.sound(SoundEvents.ROOTED_DIRT_BREAK, 1.4f, 0.6f);
        }
    }

    /**
     * Burst up out of the ground under a nearby foe and uppercut it into the air. Needs a form.
     *
     * @name Mole Pistol
     */
    public static final class MolePistol extends Ability {
        MolePistol() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, PINK).cooldown(120).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 8).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target nearby"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 8).ifPresent(target -> {
                Vec3 to = target.position().add(player.position().subtract(target.position()).normalize().scale(1.2));
                emerge(player, new Vec3(to.x, target.getY(), to.z));
                Kit.hit(player, target, AbilityDamage.STRIKE, 12f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, new Vec3(0, 1.2, 0));
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1f, 1f);
        }
    }

    /** Hollow out the ground beneath a foe: it drops into a pit and is stuck there for four seconds. */
    public static final class PitTrap extends Ability {
        PitTrap() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(200).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.MUD, 80);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                Kit.hit(player, target, AbilityDamage.CRUSH, 4f * ctx.power());
                Vfx.burst(ctx.level(), player, target.position(), 0x8A6A4A, 1.6f, Kind.SHARD, Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ROOTED_DIRT_BREAK, 1.6f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Tunnel in a ring beneath everyone around you and collapse it: they are thrown up and then stuck. */
    public static final class Earthquake extends Ability {
        Earthquake() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 10f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.9, 0));
                Kit.root(target, Vfx.StatusLook.MUD, 40);
            }
            Vfx.burst(ctx.level(), player, player.position(), 0x8A6A4A, 8f, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.6f, 0.6f);
        }
    }

    /** Vanish into the earth and come up beneath a foe up to twenty blocks away, smashing it skyward. */
    public static final class Ambush extends Ability {
        Ambush() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, PINK).cooldown(600).stamina(26).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 20).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 20).ifPresent(target -> {
                emerge(player, target.position().add(player.position().subtract(target.position()).normalize().scale(1.2)));
                Kit.hit(player, target, AbilityDamage.STRIKE, 16f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.4, 0));
                Vfx.eruption(ctx.level(), player, target.position(), 0x8A6A4A, 3f, Kind.SHARD);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.2f, 1f);
        }
    }
}
