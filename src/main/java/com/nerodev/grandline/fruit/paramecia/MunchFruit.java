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
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Baku Baku no Mi. Eat anything, and become what you ate: swallow metal for an iron body, gunpowder for a cannon,
 * arrows straight out of the air for ammunition.
 */
public class MunchFruit extends DevilFruit {
    public static final int COLOR = 0x6A8AC8;
    public static final int TOOTH = 0xF0D040;
    static final String M = "fruit/grandline:munch";
    static final String AMMO = "grandline_munch_ammo";
    static final int MAX_AMMO = 20;

    static final Buffs.Mod IRON_ARMOR = Buffs.mod(Attributes.ARMOR, GrandLine.id("munch_iron_armor"), 8, Operation.ADD_VALUE);
    static final Buffs.Mod IRON_TOUGHNESS = Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("munch_iron_toughness"), 3,
            Operation.ADD_VALUE);

    public static final DeferredHolder<Ability, Bite> BITE = ability("munch_bite", Bite::new);
    public static final DeferredHolder<Ability, Devour> DEVOUR = ability("munch_devour", Devour::new);
    public static final DeferredHolder<Ability, Cannon> CANNON = ability("munch_cannon", Cannon::new);
    public static final DeferredHolder<Ability, Spit> SPIT = ability("munch_spit", Spit::new);
    public static final DeferredHolder<Ability, Swallow> SWALLOW = ability("munch_swallow", Swallow::new);
    public static final DeferredHolder<Ability, Tank> TANK = ability("munch_tank", Tank::new);
    public static final DeferredHolder<Ability, Factory> FACTORY = ability("munch_factory", Factory::new);

    public static final ResourceLocation SHELL = GrandLine.id("munch_shell");

    public MunchFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Baku Baku no Mi", COLOR)
                .abilities(BITE, DEVOUR, CANNON, SPIT, SWALLOW)
                .awakened(TANK, FACTORY));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SHELL, Projectiles.blast(3, AbilityDamage.BLAST, null, p -> {
            if (p.level() instanceof ServerLevel level) {
                Vfx.burst(level, p, p.position(), 0xFF8A2A, 3f, Kind.FLAME, Vfx.SHOCKWAVE | Vfx.FLASH);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1f);
            }
        }));
    }

    static int ammo(ServerPlayer player) {
        return player.getPersistentData().getInt(AMMO);
    }

    static void setAmmo(ServerPlayer player, int ammo) {
        player.getPersistentData().putInt(AMMO, Math.max(0, Math.min(MAX_AMMO, ammo)));
        player.displayClientMessage(Component.literal("Cannon shells in belly: " + ammo(player)), true);
    }

    static void fireShell(AbilityContext ctx, Vec3 dir) {
        AbilityProjectile.spawn(ctx.player(), SHELL, ctx.handPos(), dir.scale(1.6), 0.5f, 0x4A4A52, 10f * ctx.power(), 40)
                .withGravity(0.02).withPower(ctx.power());
    }

    @Override
    public void onLost(ServerPlayer player) {
        player.getPersistentData().remove(AMMO);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Clamp your huge jaws on a foe in front of you; the bite feeds you a little. */
    public static final class Bite extends Ability {
        Bite() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, TOOTH).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                if (Kit.hit(player, target, AbilityDamage.STRIKE, 9f * ctx.power())) {
                    player.heal(3f);
                }
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), TOOTH, 1.2f, Kind.SHARD, 0);
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.GENERIC_EAT, 1.2f, 0.6f);
        }
    }

    /**
     * Eat whatever you hold in your other hand and take on its nature: metal gives an iron body for half a minute,
     * gunpowder or TNT loads your cannon, food heals you.
     */
    public static final class Devour extends Ability {
        Devour() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, COLOR).cooldown(40).stamina(6).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return player.getOffhandItem().isEmpty() ? Optional.of(Component.literal("Hold something to eat in your off hand"))
                    : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ItemStack stack = player.getOffhandItem();
            if (stack.isEmpty()) {
                return;
            }
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (stack.is(Tags.Items.INGOTS) || stack.is(Tags.Items.STORAGE_BLOCKS)) {
                int ticks = stack.is(Tags.Items.STORAGE_BLOCKS) ? 1800 : 600;
                Buffs.timed(player, IRON_ARMOR, ticks);
                Buffs.timed(player, IRON_TOUGHNESS, ticks);
                Vfx.status(ctx.level(), player, Vfx.StatusLook.IRON_BANDS, 20);
                ctx.sound(SoundEvents.ANVIL_USE, 0.8f, 1.2f);
            } else if (stack.is(Tags.Items.GUNPOWDERS) || stack.is(Items.TNT) || stack.is(Items.FIRE_CHARGE)) {
                setAmmo(player, ammo(player) + (stack.is(Items.TNT) ? 5 : 2));
                ctx.sound(SoundEvents.CROSSBOW_LOADING_END.value(), 1f, 0.6f);
            } else if (food != null) {
                player.heal(food.nutrition());
                player.getFoodData().eat(food.nutrition(), food.saturation());
            } else {
                player.heal(1f);
            }
            stack.shrink(1);
            Vfx.burst(ctx.level(), player, player.getEyePosition(), TOOTH, 0.8f, Kind.SHARD, 0);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.GENERIC_EAT, 1.2f, 0.8f);
        }
    }

    /**
     * Turn your arm into a cannon and fire a shell you have eaten.
     *
     * @name Wapol Cannon
     */
    public static final class Cannon extends Ability {
        Cannon() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, 0xFF8A2A).cooldown(30).stamina(8).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return ammo(player) > 0 ? Optional.empty() : Optional.of(Component.literal("Eat gunpowder or TNT first"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ammo(player) <= 0) {
                return;
            }
            setAmmo(player, ammo(player) - 1);
            fireShell(ctx, player.getLookAngle());
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.4f);
        }
    }

    /**
     * Gulp down a foe in front of you and spit it far away.
     *
     * @name Baku Baku Shock
     */
    public static final class Spit extends Ability {
        Spit() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(120).stamina(14).mastery(M, 20)
                    .masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * ctx.power());
                Kit.launch(target, player.getLookAngle().scale(2.2).add(0, 0.6, 0));
                Vfx.trail(ctx.level(), player, player.getEyePosition(), target.position().add(player.getLookAngle().scale(6)),
                        0xC8E0A0, 0.6f, Kind.DROP);
            });
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.LLAMA_SPIT, 1.2f, 0.6f);
        }
    }

    /** Hold your mouth wide open: arrows and other projectiles flying at you are eaten and become cannon shells. */
    public static final class Swallow extends Ability {
        Swallow() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, TOOTH).cooldown(160).stamina(10).staminaPerTick(0.25f)
                    .maxActive(60).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_ROAR);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(1.5));
            for (Projectile p : ctx.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(4.5),
                    e -> e.getOwner() != player && e.position().distanceToSqr(mouth) < 16)) {
                p.discard();
                setAmmo(player, ammo(player) + 1);
                Vfx.burst(ctx.level(), player, mouth, TOOTH, 0.8f, Kind.SHARD, 0);
                ctx.sound(SoundEvents.GENERIC_EAT, 1f, 1.2f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Eat your way into a body like a tank: huge, armoured, and running down anything in front of it.
     *
     * @name Wapol Tank
     */
    public static final class Tank extends Buffs.Toggle {
        Tank() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(700).stamina(30).staminaPerTick(0.3f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("munch_tank_scale"), 0.6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("munch_tank_armor"), 12, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("munch_tank_knockback"), 1, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("munch_tank_speed"), -0.2,
                                    Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), COLOR, 2.5f, Kind.SHARD, Vfx.HEAVY);
            ctx.sound(SoundEvents.ANVIL_LAND, 1f, 0.5f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.cone(player, 3 * ctx.power(), 50)) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 5f * ctx.power());
                    AbilityDamage.knockback(target, player.position(), 1.2, 0.3);
                }
            }
            return true;
        }
    }

    /**
     * Fire every shell in your belly at once in a roaring spread (at least three).
     *
     * @name Baku Baku Factory
     */
    public static final class Factory extends Ability {
        Factory() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, 0xFF8A2A).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int shells = Math.max(3, ammo(player));
            setAmmo(player, 0);
            for (int i = 0; i < shells; i++) {
                double spread = (i - (shells - 1) / 2.0) * 0.07;
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(spread)).add(0, (player.getRandom().nextDouble() - 0.5) * 0.08, 0);
                fireShell(ctx, dir.normalize());
            }
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.8f);
        }
    }
}
