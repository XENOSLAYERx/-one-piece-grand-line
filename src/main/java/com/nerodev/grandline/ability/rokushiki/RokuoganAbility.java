package com.nerodev.grandline.ability.rokushiki;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Rokuogan (Six King Gun): the secret seventh technique, only open to someone who has mastered the other six. Both
 * fists against the foe in front of you release a shockwave that goes straight through armour.
 */
public class RokuoganAbility extends Ability {
    public RokuoganAbility() {
        super(Properties.of(ActivationType.INSTANT, AbilityCategory.ROKUSHIKI)
                .icon(AbilityIcon.BURST, 0xA8C8FF)
                .cooldown(400)
                .stamina(30));
    }

    /** The six techniques Rokuogan is built on. */
    static List<Ability> six() {
        return List.of(GLAbilities.SORU.get(), GLAbilities.GEPPO.get(), GLAbilities.TEKKAI.get(), GLAbilities.SHIGAN.get(),
                GLAbilities.RANKYAKU.get(), GLAbilities.KAMIE.get());
    }

    @Override
    public Optional<Component> checkUse(ServerPlayer player) {
        for (Ability technique : six()) {
            if (!AbilityAccess.canUse(player, technique)) {
                return Optional.of(Component.literal("Rokuogan needs all six techniques; you lack ").append(technique.displayName()));
            }
        }
        return Kit.target(player, 3.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        Kit.target(player, 3.5).ifPresent(target -> {
            Kit.hit(player, target, AbilityDamage.STRIKE, 22f * ctx.power() + target.getArmorValue() * 0.6f);
            AbilityDamage.knockback(target, player.position(), 2.6, 0.5);
            target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 20, 0, false, false, true));
            Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), 0xA8C8FF, 2.6f, Kind.SHOCKWAVE, Vfx.RING | Vfx.FLASH | Vfx.HEAVY);
        });
        ctx.animate(GLEffects.ANIM_PALMS);
        ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.6f, 0.6f);
    }
}
