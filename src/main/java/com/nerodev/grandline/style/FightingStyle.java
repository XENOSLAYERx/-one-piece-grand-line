package com.nerodev.grandline.style;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.combat.CombatManager;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * A way of fighting without a Devil Fruit: Brawler, Black Leg, Fish-Man Karate... A style grants its own moves (which
 * need style mastery, earned by using them), changes what bare-handed blows do, and ends combos with its own
 * finisher. One style is equipped at a time.
 */
public class FightingStyle {
    private final int color;
    private final CombatManager.Finisher finisher;
    private final List<Supplier<? extends Ability>> moves;

    @SafeVarargs
    public FightingStyle(int color, CombatManager.Finisher finisher, Supplier<? extends Ability>... moves) {
        this.color = color;
        this.finisher = finisher;
        this.moves = List.of(moves);
    }

    public final ResourceLocation id() {
        ResourceLocation id = GLStyles.REGISTRY.getKey(this);
        if (id == null) {
            throw new IllegalStateException("Fighting style not registered: " + getClass().getName());
        }
        return id;
    }

    /** Mastery key for this style's moves, e.g. "style/grandline:black_leg". */
    public final String masteryKey() {
        return masteryKey(id());
    }

    public static String masteryKey(ResourceLocation style) {
        return "style/" + style;
    }

    public Component displayName() {
        return Component.translatable("style." + id().getNamespace() + "." + id().getPath());
    }

    public Component description() {
        return Component.translatable("style." + id().getNamespace() + "." + id().getPath() + ".desc");
    }

    public int color() {
        return color;
    }

    public CombatManager.Finisher finisher() {
        return finisher;
    }

    public List<Ability> moves() {
        return moves.stream().<Ability>map(Supplier::get).toList();
    }

    /**
     * A bare-handed melee blow by a user of this style is landing (combos have already been graded). Styles change
     * the damage or add effects here.
     */
    public void onUnarmedHit(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {}
}
