package com.nerodev.grandline.weapon;

import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Seastone handcuffs: snap them onto a creature or player within reach. For half a minute its Devil Fruit power is
 * sealed and it is weak and slow, as anyone in Seastone is.
 */
public class SeastoneCuffsItem extends Item {
    public static final int DURATION = 600;

    public SeastoneCuffsItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        cuff(target, DURATION);
        Vfx.status(level, target, Vfx.StatusLook.SEASTONE, DURATION);
        Vfx.impact(level, player, target.getBoundingBox().getCenter(), 0x6E8C8A, 1.2f, Kind.SPARK);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.2f, 0.8f);
        if (target instanceof Player cuffed) {
            cuffed.displayClientMessage(Component.literal("Seastone cuffs! Your strength drains away").withStyle(ChatFormatting.DARK_AQUA), true);
        }
        stack.consume(1, player);
        return InteractionResult.CONSUME;
    }

    /** Seals {@code target}'s fruit and saps its strength for {@code ticks}. */
    public static void cuff(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(GLMobEffects.FRUIT_SEALED, ticks, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on a creature or player to cuff it: seals Devil Fruit powers for 30s")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
