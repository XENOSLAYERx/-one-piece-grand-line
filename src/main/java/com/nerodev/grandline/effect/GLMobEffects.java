package com.nerodev.grandline.effect;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityCategory;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status effects added by the mod. */
public final class GLMobEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, GrandLine.MODID);

    public static final ResourceKey<DamageType> BLEEDING_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, GrandLine.id("bleeding"));

    /** Deep claw wounds: 1 damage per second per level, ignores armour. */
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding", BleedingEffect::new);

    /** Knocked out by Conqueror's Haki: cannot move, jump, attack or use abilities. Mobs forget their target. */
    public static final DeferredHolder<MobEffect, MobEffect> KNOCKED_OUT = EFFECTS.register("knocked_out",
            () -> new IncapacitatedEffect(0x3A1030, "knocked_out"));

    /**
     * Sealed inside something: ice, stone, wax, candy, gold, mochi. Same restrictions as knocked out; the look is
     * chosen by whoever applied it ({@code Kit.encase}).
     */
    public static final DeferredHolder<MobEffect, MobEffect> ENCASED = EFFECTS.register("encased",
            () -> new IncapacitatedEffect(0x9FD8F0, "encased"));

    /**
     * Devil Fruit powers sealed (Dark-Dark Fruit's touch, Seastone): fruit abilities cannot be used and Logia
     * bodies stop being intangible while it lasts.
     */
    public static final DeferredHolder<MobEffect, MobEffect> FRUIT_SEALED = EFFECTS.register("fruit_sealed",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x3A4A5A) {});

    /** Feet bound (iron bonds, strings, roots, glue): cannot move or jump, but can still fight. */
    public static final DeferredHolder<MobEffect, MobEffect> ROOTED = EFFECTS.register("rooted", RootedEffect::new);

    private GLMobEffects() {}

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }

    /** Server rules shared by every incapacitating effect: no attacks and no abilities while it lasts. */
    public static void registerRules() {
        AbilityAccess.addRule((player, ability) -> incapacitated(player)
                ? Optional.of(Component.literal(player.hasEffect(KNOCKED_OUT) ? "You are unconscious" : "You can't move"))
                : Optional.empty());
        AbilityAccess.addRule((player, ability) -> ability.properties().category() == AbilityCategory.FRUIT
                && player.hasEffect(FRUIT_SEALED)
                ? Optional.of(Component.literal("Your Devil Fruit power is sealed"))
                : Optional.empty());
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingIncomingDamageEvent.class, event -> {
            if (event.getSource().getEntity() instanceof LivingEntity attacker && incapacitated(attacker)) {
                event.setCanceled(true);
            }
        });
    }

    /** Knocked out or encased: no moving, attacking or using powers. */
    public static boolean incapacitated(LivingEntity entity) {
        return entity.hasEffect(KNOCKED_OUT) || entity.hasEffect(ENCASED);
    }

    static final class IncapacitatedEffect extends MobEffect {
        IncapacitatedEffect(int color, String name) {
            super(MobEffectCategory.HARMFUL, color);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, GrandLine.id("effect." + name + ".speed"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.JUMP_STRENGTH, GrandLine.id("effect." + name + ".jump"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.FLYING_SPEED, GrandLine.id("effect." + name + ".fly"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
                if (mob instanceof Creeper creeper) {
                    creeper.setSwellDir(-1);
                }
            }
            entity.setSprinting(false);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }
    }

    static final class RootedEffect extends MobEffect {
        RootedEffect() {
            super(MobEffectCategory.HARMFUL, 0x8A7A5A);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, GrandLine.id("effect.rooted.speed"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.JUMP_STRENGTH, GrandLine.id("effect.rooted.jump"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }
    }

    static final class BleedingEffect extends MobEffect {
        BleedingEffect() {
            super(MobEffectCategory.HARMFUL, 0x9E1B1B);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            entity.hurt(entity.damageSources().source(BLEEDING_DAMAGE), 1.0f + amplifier);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }
    }
}
