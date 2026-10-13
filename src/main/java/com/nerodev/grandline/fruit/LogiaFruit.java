package com.nerodev.grandline.fruit;

import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Elemental body. Physical attacks pass through the user, at a small stamina cost per dodge.
 * <p>
 * Counterplay, so a Logia is never invincible:
 * <ul>
 *   <li>Any registered {@link #TOUCH} rule lets the attack land (Armament Haki, Seastone weapons).</li>
 *   <li>In water there is no intangibility (FruitManager skips the hook entirely).</li>
 *   <li>Out of stamina means the body cannot disperse: the hit lands.</li>
 *   <li>Explosions, magic, falling and other non-physical damage always apply.</li>
 * </ul>
 */
public abstract class LogiaFruit extends DevilFruit {
    /** Stamina spent each time an attack passes through the body. */
    public static final float DODGE_COST = 4f;

    /** Rules that let an attack touch a Logia body. Haki and Seastone add themselves here. */
    public static final List<Predicate<DamageSource>> TOUCH = new CopyOnWriteArrayList<>();

    protected LogiaFruit(Properties properties) {
        super(properties);
    }

    /** True for damage this element ignores completely (e.g. fire for Flame). */
    protected abstract boolean immuneTo(DamageSource source);

    /** Damage multiplier against this element (fire melts ice and snow, burns a forest...). 1 = normal. */
    protected float weakness(ServerPlayer user, DamageSource source) {
        return 1f;
    }

    /**
     * Whether blows can pass through the body right now. False while the fruit is sealed; subclasses add their own
     * conditions (wet sand turns solid).
     */
    protected boolean intangible(ServerPlayer user) {
        return !user.hasEffect(GLMobEffects.FRUIT_SEALED);
    }

    public static boolean canTouch(DamageSource source) {
        for (Predicate<DamageSource> rule : TOUCH) {
            if (rule.test(source)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (immuneTo(source)) {
            event.setCanceled(true);
            return;
        }
        float weakness = weakness(user, source);
        if (weakness != 1f) {
            event.setAmount(event.getAmount() * weakness);
        }
        if (!source.is(AbilityDamage.PHYSICAL) || canTouch(source) || !intangible(user)) {
            return;
        }
        AbilityState state = AbilityState.of(user);
        if (!state.spendStamina(DODGE_COST, AbilityManager.now(user))) {
            return;
        }
        event.setCanceled(true);
        Vec3 at = source.getSourcePosition() != null
                ? user.getBoundingBox().getCenter().lerp(source.getSourcePosition(), 0.3)
                : user.getBoundingBox().getCenter();
        Fx.play(user.serverLevel(), GLEffects.LOGIA_DODGE, user, FxParams.at(at).color(color()));
        AbilityManager.sync(user);
    }
}
