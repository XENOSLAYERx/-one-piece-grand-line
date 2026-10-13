package com.nerodev.grandline.fruit.zoan;

import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.transform.TransformationManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Common ground of every Zoan: two bodies (hybrid and beast), toughness that heals the user slowly while
 * transformed, and bare-handed blows that land as claws, hooves or jaws in either body.
 */
public abstract class ZoanFruit extends DevilFruit {
    private final ZoanForms forms;

    protected ZoanFruit(Properties properties, ZoanForms forms) {
        super(properties);
        this.forms = forms;
    }

    public ZoanForms forms() {
        return forms;
    }

    /** Extra damage of a bare-handed blow in a form (beast bodies hit harder). */
    protected float unarmedBonus(ServerPlayer user) {
        return forms.beast(user) ? 3f : 2f;
    }

    @Override
    public boolean ticks() {
        return true;
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.tickCount % (awakened ? 40 : 100) == 0 && forms.any(user) && user.getHealth() < user.getMaxHealth()) {
            user.heal(1f);
        }
    }

    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() == user && user.getMainHandItem().isEmpty() && forms.any(user)) {
            event.setAmount(event.getAmount() + unarmedBonus(user));
        }
    }

    @Override
    public void onLost(ServerPlayer player) {
        if (forms.any(player)) {
            TransformationManager.end(player);
        }
    }
}
