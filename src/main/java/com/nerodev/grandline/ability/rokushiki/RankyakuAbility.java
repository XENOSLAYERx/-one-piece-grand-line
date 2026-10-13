package com.nerodev.grandline.ability.rokushiki;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * Rankyaku (Storm Leg): kick so fast the air itself is cut, and send the blade flying. It slices through everything
 * in its path.
 */
public class RankyakuAbility extends Ability {
    public static final ResourceLocation BLADE = GrandLine.id("rankyaku");

    public RankyakuAbility() {
        super(Properties.of(ActivationType.INSTANT, AbilityCategory.ROKUSHIKI)
                .icon(AbilityIcon.BLADE, 0xD8E4FF)
                .cooldown(60)
                .stamina(16));
    }

    public static void registerBehavior() {
        ProjectileBehavior.Registry.register(BLADE, Projectiles.piercing(AbilityDamage.SLASH, null));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        Kit.shoot(ctx, BLADE, 2.0, 1.4f, 0xE8F0FF, 11f, 25);
        ctx.animate(GLEffects.ANIM_KICK_HIGH);
        ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.4f);
    }
}
