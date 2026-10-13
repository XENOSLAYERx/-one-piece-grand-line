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
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Chiyu Chiyu no Mi. Healing tears and spores of light that close any wound. Friends are mended; the undead, made of
 * death, burn in the same light.
 */
public class HealFruit extends DevilFruit {
    public static final int COLOR = 0xF8D0E0;
    public static final int GREEN = 0x8AE08A;
    static final String M = "fruit/grandline:heal";

    public static final DeferredHolder<Ability, HealingTears> HEALING_TEARS = ability("heal_tears", HealingTears::new);
    public static final DeferredHolder<Ability, HealingSpores> HEALING_SPORES = ability("heal_spores", HealingSpores::new);
    public static final DeferredHolder<Ability, TearShot> TEAR_SHOT = ability("heal_tear_shot", TearShot::new);
    public static final DeferredHolder<Ability, Cleanse> CLEANSE = ability("heal_cleanse", Cleanse::new);
    public static final DeferredHolder<Ability, DandelionField> DANDELION_FIELD = ability("heal_dandelion_field", DandelionField::new);
    public static final DeferredHolder<Ability, GreatBloom> GREAT_BLOOM = ability("heal_great_bloom", GreatBloom::new);
    public static final DeferredHolder<Ability, LifeShield> LIFE_SHIELD = ability("heal_life_shield", LifeShield::new);

    public static final ResourceLocation TEAR = GrandLine.id("heal_tear");
    public static final ResourceLocation FIELD = GrandLine.id("heal_field");

    public HealFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Chiyu Chiyu no Mi", COLOR)
                .abilities(HEALING_TEARS, HEALING_SPORES, TEAR_SHOT, CLEANSE, DANDELION_FIELD)
                .awakened(GREAT_BLOOM, LIFE_SHIELD));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(TEAR, Projectiles.bolt(AbilityDamage.LIGHT, (p, target) -> {
            if (target.getType().is(EntityTypeTags.UNDEAD)) {
                ServerPlayer owner = p.ownerPlayer();
                if (owner != null) {
                    Kit.hit(owner, target, AbilityDamage.LIGHT, p.damage() * 1.5f);
                }
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
            }
        }));
        // The field mends every friend inside it and sears the undead.
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && target.getType().is(EntityTypeTags.UNDEAD)) {
                Kit.hit(owner, target, AbilityDamage.LIGHT, p.damage());
            }
        }, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 20 == 0) {
                for (Player friend : friends(owner, p.position(), p.size() / 2.0)) {
                    friend.heal(1.5f);
                }
            }
        }));
    }

    /** The user and every player within {@code radius} that the user's powers may not harm. */
    static List<Player> friends(ServerPlayer owner, Vec3 at, double radius) {
        return owner.serverLevel().getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius),
                p -> p == owner || !AbilityDamage.canHarm(owner, p));
    }

    static void bloom(AbilityContext ctx, LivingEntity who) {
        Vfx.burst(ctx.level(), who, who.getBoundingBox().getCenter(), GREEN, 1.4f, Kind.HEART, 0);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Shed healing tears: your wounds close and keep closing for a few seconds. */
    public static final class HealingTears extends Ability {
        HealingTears() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(240).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.heal(8f * ctx.power());
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            bloom(ctx, player);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.6f);
        }
    }

    /** Release spores of healing light over everyone around you: friends are mended. */
    public static final class HealingSpores extends Ability {
        HealingSpores() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, GREEN).cooldown(300).stamina(16).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Player friend : friends(player, player.position(), 8 * ctx.power())) {
                friend.heal(6f * ctx.power());
                friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
                bloom(ctx, friend);
            }
            Vfx.rain(ctx.level(), player, player.position(), GREEN, 8f, Kind.GLOW);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.6f);
        }
    }

    /** Flick a tear of light: it burns the undead fiercely and stings anything else. */
    public static final class TearShot extends Ability {
        TearShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, COLOR).cooldown(40).stamina(8).mastery(M, 10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, TEAR, 1.8, 0.35f, COLOR, 5f, 25);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 2f);
        }
    }

    /** Wash every harmful effect off yourself and every friend beside you. */
    public static final class Cleanse extends Ability {
        Cleanse() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, GREEN).cooldown(300).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Player friend : friends(player, player.position(), 6)) {
                for (Holder<MobEffect> effect : new ArrayList<>(friend.getActiveEffectsMap().keySet())) {
                    if (!effect.value().isBeneficial()) {
                        friend.removeEffect(effect);
                    }
                }
                friend.clearFire();
                bloom(ctx, friend);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 1.2f);
        }
    }

    /** Sow a field of glowing dandelions where you point: friends inside heal over time, the undead are seared. */
    public static final class DandelionField extends Ability {
        DandelionField() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, COLOR).cooldown(400).stamina(18).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 16), 6);
            AbilityProjectile.zone(player, FIELD, at, 6 * ctx.power(), GREEN, 3f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, at, GREEN, 6f, Kind.LEAF, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A great bloom of healing light: every friend nearby is restored and shielded with extra hearts. */
    public static final class GreatBloom extends Ability {
        GreatBloom() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, GREEN).cooldown(900).stamina(36).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Player friend : friends(player, player.position(), 16 * ctx.power())) {
                friend.heal(20f);
                friend.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
                bloom(ctx, friend);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), GREEN, 16f, Kind.HEART, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.TOTEM_USE, 1f, 1.4f);
        }
    }

    /** Wrap every friend near you in living light: for five seconds they shrug off most harm and heal quickly. */
    public static final class LifeShield extends Ability {
        LifeShield() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Player friend : friends(player, player.position(), 10 * ctx.power())) {
                friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
                friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2));
                bloom(ctx, friend);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 10f, Kind.GLOW, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1.2f, 1.6f);
        }
    }
}
