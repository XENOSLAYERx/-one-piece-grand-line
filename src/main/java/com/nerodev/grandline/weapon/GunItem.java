package com.nerodev.grandline.weapon;

import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A firearm: use it to fire one bullet from the ammunition you carry (Seastone bullets first in the off hand, then
 * whatever is in the inventory). Each gun has its own damage, muzzle speed, accuracy and reload time.
 */
public class GunItem extends Item {
    /** Set in a bullet's extra data when it is Seastone (for shot kinds that can be either). */
    public static final String SEASTONE_TAG = "seastone";

    private final float damage;
    private final int reload;
    private final float speed;
    private final float spread;
    private final boolean longGun;

    /**
     * @param speed   muzzle speed in blocks per tick
     * @param spread  how far a shot can stray (0 = dead accurate)
     * @param longGun fired from the shoulder with both hands (a musket) rather than one (a pistol)
     */
    public GunItem(float damage, int reload, float speed, float spread, boolean longGun, Properties properties) {
        super(properties.stacksTo(1));
        this.damage = damage;
        this.reload = reload;
        this.speed = speed;
        this.spread = spread;
        this.longGun = longGun;
    }

    public float damage() {
        return damage;
    }

    public int reload() {
        return reload;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer shooter)) {
            return InteractionResultHolder.success(gun);
        }
        Optional<Boolean> bullet = AmmoItem.consume(shooter);
        if (bullet.isEmpty()) {
            outOfAmmo(shooter);
            return InteractionResultHolder.fail(gun);
        }
        boolean seastone = bullet.get();
        fire(shooter, seastone ? GLWeapons.SEASTONE_BULLET_SHOT : GLWeapons.BULLET_SHOT, WeaponManager.gunDamage(shooter, damage), speed,
                spread, seastone);
        Fx.animate(shooter, longGun ? GLEffects.ANIM_RIFLE_SHOT : GLEffects.ANIM_PISTOL_SHOT);
        shooter.getCooldowns().addCooldown(this, WeaponManager.reloadTicks(shooter, reload));
        return InteractionResultHolder.consume(gun);
    }

    public static void outOfAmmo(ServerPlayer shooter) {
        shooter.displayClientMessage(Component.literal("Out of ammunition").withStyle(ChatFormatting.RED), true);
        shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f,
                1.4f);
    }

    public float speed() {
        return speed;
    }

    public boolean longGun() {
        return longGun;
    }

    /**
     * Fires one bullet of {@code kind} along the shooter's look, strayed by up to {@code spread}.
     *
     * @return the bullet
     */
    public static AbilityProjectile fire(ServerPlayer shooter, ResourceLocation kind, float damage, float speed, float spread,
            boolean seastone) {
        Vec3 look = shooter.getLookAngle();
        Vec3 dir = look.add(shooter.getRandom().triangle(0, spread), shooter.getRandom().triangle(0, spread),
                shooter.getRandom().triangle(0, spread)).normalize();
        Vec3 muzzle = shooter.getEyePosition().add(look.scale(0.6)).add(0, -0.1, 0);
        AbilityProjectile shot = AbilityProjectile.spawn(shooter, kind, muzzle, dir.scale(speed), 0.15f,
                seastone ? GLWeapons.SEASTONE_COLOR : 0x8A8A8A, damage, 30);
        if (seastone) {
            shot.extra().putBoolean(SEASTONE_TAG, true);
        }
        Vfx.impact(shooter.serverLevel(), shooter, muzzle.add(look.scale(0.3)), 0xF0C060, 0.6f, Kind.SPARK);
        Vfx.impact(shooter.serverLevel(), shooter, muzzle.add(look.scale(0.4)), 0xD0D0D0, 0.8f, Kind.SMOKE);
        shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                0.35f, 1.9f);
        shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS,
                1f, 0.6f);
        return shot;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(WeaponKind.GUN.label() + String.format(" - %.0f damage, reloads in %.1fs", damage, reload / 20f))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Use to fire; needs bullets").withStyle(ChatFormatting.DARK_GRAY));
    }
}
