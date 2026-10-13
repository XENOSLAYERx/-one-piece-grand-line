package com.nerodev.grandline.effect;

import com.nerodev.grandline.GrandLine;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status effects added by the mod. */
public final class GLMobEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, GrandLine.MODID);

    public static final ResourceKey<DamageType> BLEEDING_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, GrandLine.id("bleeding"));

    /** Deep claw wounds: 1 damage per second per level, ignores armour. */
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding", BleedingEffect::new);

    /**
     * Knocked out by Conqueror's Haki: cannot move, jump, attack or use abilities. Mobs forget their target.
     * Attacks made by a knocked-out entity are cancelled in {@code HakiManager}.
     */
    public static final DeferredHolder<MobEffect, MobEffect> KNOCKED_OUT = EFFECTS.register("knocked_out", KnockedOutEffect::new);

    private GLMobEffects() {}

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }

    static final class KnockedOutEffect extends MobEffect {
        KnockedOutEffect() {
            super(MobEffectCategory.HARMFUL, 0x3A1030);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, GrandLine.id("effect.knocked_out.speed"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.JUMP_STRENGTH, GrandLine.id("effect.knocked_out.jump"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.FLYING_SPEED, GrandLine.id("effect.knocked_out.fly"), -1.0,
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
