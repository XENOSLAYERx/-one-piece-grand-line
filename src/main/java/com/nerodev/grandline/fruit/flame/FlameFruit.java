package com.nerodev.grandline.fruit.flame;

import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.LogiaFruit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Mera Mera no Mi. A Logia: the body is fire. Immune to fire and lava, physical attacks pass through, and the
 * user's own blows set targets alight. Awakening lets the fire act on its own: fireflies, burning ground.
 */
public class FlameFruit extends LogiaFruit {
    public FlameFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.LEGENDARY, "Mera Mera no Mi", FlameAbilities.COLOR)
                .abilities(FlameAbilities.FIRE_FIST, FlameAbilities.FIRE_GUN, FlameAbilities.FLAME_COMMANDMENT,
                        FlameAbilities.FIRE_PILLAR, FlameAbilities.FLAME_EMPEROR)
                .awakened(FlameAbilities.FIREFLIES, FlameAbilities.DETONATE, FlameAbilities.FLAME_FIELD));
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE);
    }

    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        // Bare-handed and weapon blows from a flame body leave the target burning.
        if (event.getSource().getDirectEntity() == user) {
            target.igniteForSeconds(FruitManager.isAwakened(user) ? 5 : 2);
        }
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        // A flame body cannot stay on fire visibly forever; vanilla burning does no damage anyway (immune).
        if (user.getRemainingFireTicks() > 0 && user.tickCount % 20 == 0) {
            user.clearFire();
        }
    }

    @Override
    public boolean ticks() {
        return true;
    }
}
