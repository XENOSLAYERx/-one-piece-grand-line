package com.nerodev.grandline.ability.rokushiki;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Kami-e (Paper Art): go limp as a sheet of paper and let blows carry you aside. While it is held, close-range blows
 * have a good chance to miss entirely, each one paid for in stamina.
 */
public class KamieAbility extends Ability {
    public static final float DODGE_CHANCE = 0.45f;
    public static final float DODGE_STAMINA = 4f;

    public KamieAbility() {
        super(Properties.of(ActivationType.TOGGLE, AbilityCategory.ROKUSHIKI)
                .icon(AbilityIcon.LEAF, 0xF4F0E0)
                .cooldown(200)
                .stamina(10)
                .staminaPerTick(0.05f)
                .maxActive(400));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), 0xF4F0E0, 1.4f, Kind.LEAF, 0);
        ctx.animate(GLEffects.ANIM_SIDESTEP);
        ctx.sound(GLSounds.EVADE.get(), 0.8f, 1.4f);
    }

    @Override
    public boolean onTick(AbilityContext ctx) {
        return true;
    }

    @Override
    public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
        ServerPlayer player = ctx.player();
        if (event.getSource().getEntity() == null || event.getSource().getDirectEntity() != event.getSource().getEntity()) {
            return;
        }
        if (player.getRandom().nextFloat() < DODGE_CHANCE && ctx.state().spendStamina(DODGE_STAMINA, ctx.level().getGameTime())) {
            event.setCanceled(true);
            Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), 0xF4F0E0, 1.2f, Kind.LEAF);
            ctx.animate(GLEffects.ANIM_SIDESTEP);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), GLSounds.EVADE.get(), SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }
}
