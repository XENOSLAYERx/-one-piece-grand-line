package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Hobi Hobi no Mi. One touch turns a living thing into a toy: small, harmless and bound to obey the one who made it.
 * Everyone else forgets the toy was ever anything else.
 */
public class HobbyFruit extends DevilFruit {
    public static final int COLOR = 0xF0A8D0;
    public static final int BLUE = 0x8AD0F0;
    static final String M = "fruit/grandline:hobby";

    static final Buffs.Mod TOY_SIZE = Buffs.mod(Attributes.SCALE, GrandLine.id("hobby_toy_scale"), -0.5, Operation.ADD_MULTIPLIED_TOTAL);
    static final Buffs.Mod TOY_WEAK = Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("hobby_toy_damage"), -0.5,
            Operation.ADD_MULTIPLIED_TOTAL);
    public static final ResourceLocation TOY = GrandLine.id("hobby_toy");
    public static final ResourceLocation SOLDIER = GrandLine.id("hobby_soldier");

    public static final DeferredHolder<Ability, ToyTouch> TOY_TOUCH = ability("hobby_toy_touch", ToyTouch::new);
    public static final DeferredHolder<Ability, ToySoldiers> TOY_SOLDIERS = ability("hobby_toy_soldiers", ToySoldiers::new);
    public static final DeferredHolder<Ability, Contract> CONTRACT = ability("hobby_contract", Contract::new);
    public static final DeferredHolder<Ability, ToyBox> TOY_BOX = ability("hobby_toy_box", ToyBox::new);
    public static final DeferredHolder<Ability, Forget> FORGET = ability("hobby_forget", Forget::new);
    public static final DeferredHolder<Ability, ToyWorld> TOY_WORLD = ability("hobby_toy_world", ToyWorld::new);
    public static final DeferredHolder<Ability, ToyParade> TOY_PARADE = ability("hobby_toy_parade", ToyParade::new);

    public HobbyFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Hobi Hobi no Mi", COLOR)
                .abilities(TOY_TOUCH, TOY_SOLDIERS, CONTRACT, TOY_BOX, FORGET)
                .awakened(TOY_WORLD, TOY_PARADE));
    }

    static void init() {}

    static void behaviors() {}

    /** Turns a creature into a toy: a beast becomes yours to command for a while; a player is shrunk and feeble. */
    static boolean toyify(ServerPlayer player, LivingEntity target, int ticks) {
        Buffs.timed(target, TOY_SIZE, ticks);
        Buffs.timed(target, TOY_WEAK, ticks);
        Vfx.burst(player.serverLevel(), target, target.getBoundingBox().getCenter(), COLOR, 1.4f, Kind.STAR, Vfx.FLASH);
        if (target instanceof Mob mob) {
            return Minions.bind(player, TOY, mob, ticks);
        }
        if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        }
        return true;
    }

    static void spawnSoldiers(ServerPlayer player, int count, int ticks) {
        for (Vec3 at : Kit.circle(player.position(), 2, count, player.getYRot() * Math.PI / 180)) {
            Minions.spawn(player, SOLDIER, EntityType.ZOMBIE, at, ticks, mob -> {
                mob.setBaby(true);
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                mob.setDropChance(EquipmentSlot.MAINHAND, 0f);
                mob.setDropChance(EquipmentSlot.HEAD, 0f);
                mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, false, false));
            });
        }
    }

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, TOY);
        Minions.dismiss(player, SOLDIER);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Touch a creature and it becomes a toy: small, weak, and (if it's a beast) yours to command for thirty seconds. */
    public static final class ToyTouch extends Ability {
        ToyTouch() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(300).stamina(18).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.target(ctx.player(), 4.5).ifPresent(target -> toyify(ctx.player(), target, 600));
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.2f, 1.4f);
        }
    }

    /** Wind up three little toy soldiers that march into battle for you. */
    public static final class ToySoldiers extends Ability {
        ToySoldiers() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, BLUE).cooldown(500).stamina(18).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Minions.dismiss(ctx.player(), SOLDIER);
            spawnSoldiers(ctx.player(), 3, 400);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), 1.2f, 1.2f);
        }
    }

    /** Remind every toy of its contract: all your toys and soldiers fight harder and faster for ten seconds. */
    public static final class Contract extends Ability {
        Contract() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(240).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Minions.of(player, null).isEmpty() ? Optional.of(Component.literal("You have no toys")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (Mob toy : Minions.of(ctx.player(), null)) {
                toy.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                toy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                Vfx.impact(ctx.level(), toy, toy.getBoundingBox().getCenter(), COLOR, 1f, Kind.STAR);
            }
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.2f, 1.2f);
        }
    }

    /** Snap a toy box shut around a foe: it is locked inside for four seconds. */
    public static final class ToyBox extends Ability {
        ToyBox() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, BLUE).cooldown(200).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.target(ctx.player(), 14).ifPresent(target -> Kit.encase(target, Vfx.StatusLook.CANDY, 80));
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.CHEST_CLOSE, 1.2f, 1.2f);
        }
    }

    /** Wipe yourself from every memory around you: monsters forget they were chasing you, players lose sight of you. */
    public static final class Forget extends Ability {
        Forget() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(300).stamina(14).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> m.getTarget() == player)) {
                mob.setTarget(null);
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            }
            for (Player other : ctx.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(10), p -> p != player)) {
                if (AbilityDamage.canHarm(player, other)) {
                    other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                }
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), COLOR, 6f, Kind.STAR, Vfx.RING);
            ctx.sound(GLSounds.HAKI_SENSE.get(), 1f, 1.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A wave of toy-making washes out from you: every beast nearby becomes one of your toys for twenty seconds. */
    public static final class ToyWorld extends Ability {
        ToyWorld() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(900).stamina(36).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                toyify(player, target, 400);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 8f, Kind.STAR, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 2f, 0.8f);
        }
    }

    /** Wind up a whole parade of toy soldiers at once. */
    public static final class ToyParade extends Ability {
        ToyParade() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, BLUE).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Minions.dismiss(ctx.player(), SOLDIER);
            spawnSoldiers(ctx.player(), 6, 500);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), 2f, 1f);
        }
    }
}
