package com.nerodev.grandline.ability.rokushiki;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Shigan (Finger Pistol): drive a fingertip into the foe in front of you as fast as a bullet. A small, deep wound that
 * keeps bleeding.
 */
public class ShiganAbility extends Ability {
    public ShiganAbility() {
        super(Properties.of(ActivationType.INSTANT, AbilityCategory.ROKUSHIKI)
                .icon(AbilityIcon.SPEAR, 0xD8E4FF)
                .cooldown(40)
                .stamina(12));
    }

    @Override
    public Optional<Component> checkUse(ServerPlayer player) {
        return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        Kit.target(player, 4).ifPresent(target -> {
            Kit.hit(player, target, AbilityDamage.SLASH, 12f * ctx.power());
            target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 80, 1));
            Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), 0xD8E4FF, 0.8f, Kind.SPARK);
        });
        ctx.animate(GLEffects.ANIM_PUNCH);
        ctx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.6f);
    }
}
