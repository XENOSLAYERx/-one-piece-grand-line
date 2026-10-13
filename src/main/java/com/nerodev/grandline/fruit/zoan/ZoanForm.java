package com.nerodev.grandline.fruit.zoan;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.transform.Transformation;
import com.nerodev.grandline.transform.TransformationManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Toggles one of a Zoan's bodies. Switching from the other body is direct (the transformation manager ends it first).
 * While the body holds, its traits apply: flying bodies fly, swimmers breathe and race underwater, climbers scale walls.
 */
public final class ZoanForm extends Ability {
    private final ZoanForms forms;
    private final boolean hybrid;

    ZoanForm(ZoanForms forms, boolean hybrid) {
        super(Ability.Properties.of(ActivationType.TOGGLE, com.nerodev.grandline.ability.AbilityCategory.FRUIT)
                .icon(hybrid ? AbilityIcon.CLAW : AbilityIcon.TRANSFORM, forms.color())
                .cooldown(30).stamina(10).staminaPerTick(0.04f).maxActive(20 * 60 * 30).masteryGain(0.4));
        this.forms = forms;
        this.hybrid = hybrid;
    }

    public boolean isHybrid() {
        return hybrid;
    }

    private Transformation form() {
        return hybrid ? forms.hybridForm() : forms.beastForm();
    }

    private boolean flies() {
        return hybrid ? forms.traits().hybridFlight() : forms.traits().beastFlight();
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        TransformationManager.start(player, form());
        if (flies()) {
            Kit.setFlight(player, true);
        }
        ctx.fx(GLEffects.TRANSFORM_PUFF, FxParams.at(player.position()).color(forms.color()).scale(hybrid ? 1.2f : 1.5f));
        ctx.sound(forms.sound.get(), 1.0f, hybrid ? 1.0f : 0.85f);
    }

    @Override
    public boolean onTick(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        if (!TransformationManager.isActive(player, form())) {
            return false;
        }
        ZoanForms.Traits traits = forms.traits();
        if (flies()) {
            player.resetFallDistance();
        }
        if (traits.swim() && player.isInWater()) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false));
        }
        if (traits.climb() && player.horizontalCollision && !player.isShiftKeyDown()) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, Math.max(v.y, 0.2), v.z);
            player.hurtMarked = true;
            player.resetFallDistance();
        }
        return true;
    }

    @Override
    public void onEnd(AbilityContext ctx, EndReason reason) {
        ServerPlayer player = ctx.player();
        if (flies()) {
            Kit.setFlight(player, false);
        }
        if (TransformationManager.isActive(player, form())) {
            TransformationManager.end(player);
            ctx.fx(GLEffects.TRANSFORM_PUFF, FxParams.at(player.position()).color(0xD8D0C0).scale(0.8f));
        }
    }
}
