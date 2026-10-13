package com.nerodev.grandline.effect;

import com.nerodev.grandline.GrandLine;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status effects added by the mod. */
public final class GLMobEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, GrandLine.MODID);

    public static final ResourceKey<DamageType> BLEEDING_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, GrandLine.id("bleeding"));

    /** Deep claw wounds: 1 damage per second per level, ignores armour. */
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding", BleedingEffect::new);

    private GLMobEffects() {}

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
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
