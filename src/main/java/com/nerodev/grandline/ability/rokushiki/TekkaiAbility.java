package com.nerodev.grandline.ability.rokushiki;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.sound.GLSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Tekkai (Iron Body): hold to harden the body like steel. Physical damage is cut to a quarter and knockback is
 * ignored, but you can barely move and every blocked hit costs extra stamina. Damage that bypasses armour
 * (magic, poison, starvation...) is not blocked.
 */
public class TekkaiAbility extends Ability {
    public static final float DAMAGE_TAKEN = 0.25f;
    public static final float STAMINA_PER_BLOCK = 4f;
    private static final ResourceLocation SLOW_ID = GrandLine.id("tekkai_slow");
    private static final ResourceLocation ANCHOR_ID = GrandLine.id("tekkai_anchor");

    public TekkaiAbility() {
        super(Properties.of(ActivationType.HOLD, AbilityCategory.ROKUSHIKI)
                .icon(AbilityIcon.SHIELD, 0xB8C2CC)
                .cooldown(40)
                .stamina(5)
                .staminaPerTick(0.4f)
                .maxActive(20 * 10));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        addModifier(player.getAttribute(Attributes.MOVEMENT_SPEED),
                new AttributeModifier(SLOW_ID, -0.85, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        addModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE),
                new AttributeModifier(ANCHOR_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
        ctx.animate(GLEffects.ANIM_TEKKAI);
        ctx.sound(GLSounds.TEKKAI_ON.get(), 0.9f, 1.0f);
        ctx.fx(GLEffects.TEKKAI_GUARD, FxParams.at(player.position()).color(0xC8D0D8));
    }

    @Override
    public boolean onTick(AbilityContext ctx) {
        if (ctx.activeTicks() > 0 && ctx.activeTicks() % 15 == 0) {
            ctx.fx(GLEffects.TEKKAI_GUARD, FxParams.at(ctx.player().position()).color(0xC8D0D8).variant(1));
        }
        return true;
    }

    @Override
    public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }
        ServerPlayer player = ctx.player();
        event.setAmount(event.getAmount() * DAMAGE_TAKEN);
        ctx.state().spendStamina(STAMINA_PER_BLOCK, AbilityManager.now(player));
        Fx.play(player.serverLevel(), GLEffects.TEKKAI_BLOCK, player,
                FxParams.at(player.position().add(0, player.getBbHeight() * 0.6, 0)).color(0xE0E6EC));
        ctx.sound(GLSounds.TEKKAI_BLOCK.get(), 1.0f, 0.8f + player.getRandom().nextFloat() * 0.3f);
    }

    @Override
    public void onEnd(AbilityContext ctx, EndReason reason) {
        ServerPlayer player = ctx.player();
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SLOW_ID);
        removeModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), ANCHOR_ID);
        Fx.stopAnimation(player);
    }

    private static void addModifier(AttributeInstance attribute, AttributeModifier modifier) {
        if (attribute != null && !attribute.hasModifier(modifier.id())) {
            attribute.addTransientModifier(modifier);
        }
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }

    /** True while Tekkai's movement penalty is applied. Used by GameTests. */
    public static boolean isHardened(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        return speed != null && speed.hasModifier(SLOW_ID);
    }
}
