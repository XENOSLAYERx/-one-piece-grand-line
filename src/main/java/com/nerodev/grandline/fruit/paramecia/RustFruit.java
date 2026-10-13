package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Sabi Sabi no Mi. Metal rusts and crumbles at the user's touch: armour thins, blades break, iron giants fall apart.
 */
public class RustFruit extends DevilFruit {
    public static final int COLOR = 0xA8582A;
    public static final int FLAKE = 0xD88A4A;
    static final String M = "fruit/grandline:rust";

    static final Buffs.Mod RUSTED = Buffs.mod(Attributes.ARMOR, GrandLine.id("rust_rusted_armor"), -0.35,
            Operation.ADD_MULTIPLIED_TOTAL);
    static final Buffs.Mod CORRODED = Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("rust_corroded_toughness"), -1,
            Operation.ADD_MULTIPLIED_TOTAL);

    public static final DeferredHolder<Ability, RustTouch> RUST_TOUCH = ability("rust_touch", RustTouch::new);
    public static final DeferredHolder<Ability, BladeBreak> BLADE_BREAK = ability("rust_blade_break", BladeBreak::new);
    public static final DeferredHolder<Ability, RustWave> RUST_WAVE = ability("rust_wave", RustWave::new);
    public static final DeferredHolder<Ability, RustVeil> RUST_VEIL = ability("rust_veil", RustVeil::new);
    public static final DeferredHolder<Ability, Crumble> CRUMBLE = ability("rust_crumble", Crumble::new);
    public static final DeferredHolder<Ability, RustField> RUST_FIELD = ability("rust_field", RustField::new);
    public static final DeferredHolder<Ability, IronCollapse> IRON_COLLAPSE = ability("rust_iron_collapse", IronCollapse::new);

    public static final ResourceLocation FIELD = GrandLine.id("rust_field");

    public RustFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Sabi Sabi no Mi", COLOR)
                .abilities(RUST_TOUCH, BLADE_BREAK, RUST_WAVE, RUST_VEIL, CRUMBLE)
                .awakened(RUST_FIELD, IRON_COLLAPSE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(20, (p, target) -> {
            rust(target, 60);
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && target instanceof IronGolem) {
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage() * 3);
            }
        }, null));
    }

    /** Rusts a creature's armour for {@code ticks}: a third of its protection and all its toughness crumble. */
    static void rust(LivingEntity target, int ticks) {
        Buffs.timed(target, RUSTED, ticks);
        Buffs.timed(target, CORRODED, ticks);
        if (target.level() instanceof net.minecraft.server.level.ServerLevel level) {
            Vfx.impact(level, target, target.getBoundingBox().getCenter(), FLAKE, 0.8f, Kind.SHARD);
        }
    }

    /** Damage against metal: iron golems and heavily armoured foes take much more. */
    static float againstMetal(LivingEntity target, float base) {
        if (target instanceof IronGolem) {
            return base * 3;
        }
        return base + target.getArmorValue() * 0.6f;
    }

    /** Every bare-handed blow you land rusts the target's armour a little. */
    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK) && event.getSource().getDirectEntity() == user) {
            rust(target, 100);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Grip a foe: its armour rusts through and it takes damage according to how much metal it wears. */
    public static final class RustTouch extends Ability {
        RustTouch() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(50).stamina(10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, againstMetal(target, 5f * ctx.power()));
                rust(target, 300);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.GRINDSTONE_USE, 1f, 0.6f);
        }
    }

    /** Touch a foe's weapon: if it is a monster's, it rusts to nothing; a player's hand goes numb instead. */
    public static final class BladeBreak extends Ability {
        BladeBreak() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, FLAKE).cooldown(160).stamina(14).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 5).ifPresent(target -> {
                if (target instanceof Mob mob && !mob.getMainHandItem().isEmpty()) {
                    mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    Vfx.burst(ctx.level(), mob, mob.getBoundingBox().getCenter(), FLAKE, 1.2f, Kind.SHARD, 0);
                } else {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2));
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 2));
                }
                Kit.hit(player, target, AbilityDamage.CRUSH, 3f * ctx.power());
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ITEM_BREAK, 1.2f, 0.7f);
        }
    }

    /** Send a wave of rust flakes over everyone in front of you, corroding their armour. */
    public static final class RustWave extends Ability {
        RustWave() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(120).stamina(14).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 7 * ctx.power(), 45)) {
                rust(target, 240);
                Kit.hit(player, target, AbilityDamage.CRUSH, againstMetal(target, 3f * ctx.power()));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(7)), FLAKE, 1.4f,
                    Kind.SHARD, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.GRINDSTONE_USE, 1.4f, 0.5f);
        }
    }

    /** Hold up a veil of rust: arrows and other metal flying at you crumble before they land. */
    public static final class RustVeil extends Ability {
        RustVeil() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, FLAKE).cooldown(200).stamina(10).staminaPerTick(0.25f)
                    .maxActive(100).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FOCUS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (AbstractArrow arrow : ctx.level().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(4),
                    e -> e.getOwner() != player)) {
                Vfx.impact(ctx.level(), player, arrow.position(), FLAKE, 0.6f, Kind.SHARD);
                arrow.discard();
            }
            if (ctx.activeTicks() % 8 == 0) {
                Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), FLAKE, 2.5f, Kind.SHARD, Vfx.RING);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Lay both hands on a foe and rust everything metal on it at once: the more armour it wears, the more it hurts. */
    public static final class Crumble extends Ability {
        Crumble() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(180).stamina(18).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, againstMetal(target, 6f * ctx.power()) * 1.6f);
                rust(target, 400);
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), FLAKE, 1.8f, Kind.SHARD, Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ANVIL_DESTROY, 1f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Let rust spread across the ground around you: every bit of metal inside it corrodes, and iron golems fall apart. */
    public static final class RustField extends Ability {
        RustField() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            AbilityProjectile.zone(player, FIELD, player.position(), radius, FLAKE, 3f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), FLAKE, (float) radius, Kind.SHARD,
                    Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.GRINDSTONE_USE, 2f, 0.4f);
        }
    }

    /** Every armoured foe around you rusts from the inside out, taking damage for every piece of metal it wears. */
    public static final class IronCollapse extends Ability {
        IronCollapse() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, FLAKE).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 12 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, againstMetal(target, 6f * ctx.power()) * 1.5f);
                rust(target, 300);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            Vec3 center = player.position().add(0, 1, 0);
            Vfx.burst(ctx.level(), player, center, COLOR, 12f, Kind.SHARD, Vfx.RING | Vfx.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ANVIL_DESTROY, 2f, 0.5f);
        }
    }
}
