package com.nerodev.grandline.fruit.leopard;

import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Neko Neko no Mi, Model: Leopard. A carnivorous Zoan: the user can become a hybrid or a full leopard.
 * Zoan bodies are tough: in either form the user heals slowly, and claws open wounds on any target they hit.
 */
public class LeopardFruit extends DevilFruit {
    public LeopardFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.RARE, "Neko Neko no Mi, Model: Leopard", LeopardAbilities.COLOR)
                .abilities(LeopardAbilities.HYBRID_FORM, LeopardAbilities.BEAST_FORM, LeopardAbilities.POUNCE,
                        LeopardAbilities.CLAWS, LeopardAbilities.FRENZY)
                .awakened(LeopardAbilities.PRIMAL_FURY, LeopardAbilities.ROAR));
    }

    static boolean transformed(ServerPlayer player) {
        return LeopardMoves.hybrid(player) || LeopardMoves.beast(player);
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        // Zoan durability: slow natural healing while transformed (faster when awakened).
        if (user.tickCount % (awakened ? 40 : 100) == 0 && transformed(user) && user.getHealth() < user.getMaxHealth()) {
            user.heal(1f);
        }
    }

    @Override
    public boolean ticks() {
        return true;
    }

    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        // Bare-handed blows in a form are claw strikes.
        if (event.getSource().getDirectEntity() == user && user.getMainHandItem().isEmpty() && transformed(user)) {
            event.setAmount(event.getAmount() + 2f);
        }
    }
}
