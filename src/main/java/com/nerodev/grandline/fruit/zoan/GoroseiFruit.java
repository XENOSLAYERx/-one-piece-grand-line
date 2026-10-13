package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The fruits of the Five Elders. Their transformed bodies close wounds almost as fast as they open, and each elder
 * can step through a pentagram to appear wherever they look.
 */
public abstract class GoroseiFruit extends ZoanFruit {
    /** The pentagram's dark red. */
    public static final int SIGIL = 0x8A1A2A;

    protected GoroseiFruit(Properties properties, ZoanForms forms) {
        super(properties, forms);
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.tickCount % (awakened ? 10 : 20) == 0 && forms().any(user) && user.getHealth() < user.getMaxHealth()) {
            user.heal(1f);
        }
    }

    /** A pentagram sigil drawn flat on the ground at {@code at}. */
    static void sigil(AbilityContext ctx, Vec3 at, float radius) {
        Vfx.burst(ctx.level(), ctx.player(), at, SIGIL, radius, Kind.RING, Vfx.RING | Vfx.GROUND);
        for (Vec3 point : Kit.circle(at, radius * 0.8, 5, 0)) {
            Vfx.eruption(ctx.level(), ctx.player(), point, SIGIL, 1.2f, Kind.SPARK);
        }
    }

    /** Steps into a pentagram and out of another where the user looks, up to thirty-two blocks away. */
    abstract static class PentagramStep extends Ability {
        PentagramStep(int color, String masteryKey) {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, color).cooldown(160).stamina(12).mastery(masteryKey, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            sigil(ctx, from, 1.8f);
            if (Kit.blink(player, 32 * ctx.power(), SIGIL, Kind.SMOKE) > 0) {
                sigil(ctx, player.position(), 1.8f);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 1f, 0.8f);
        }
    }
}
