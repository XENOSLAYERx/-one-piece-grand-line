package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.zoan.ZoanKit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Kobu Kobu no Mi. Rouse the fighting spirit in others, as the Revolutionary commander Belo Betty does: a wave of the
 * flag makes allies stronger, faster and braver, and even wavering enemies lay down their fear and fight for her.
 */
public class InspireFruit extends DevilFruit {
    public static final int COLOR = 0xE84A3A;
    public static final int GOLD = 0xF0D040;
    static final String M = "fruit/grandline:inspire";

    public static final DeferredHolder<Ability, RallyingWave> RALLYING_WAVE = ability("inspire_rallying_wave", RallyingWave::new);
    public static final DeferredHolder<Ability, FlagStrike> FLAG_STRIKE = ability("inspire_flag_strike", FlagStrike::new);
    public static final DeferredHolder<Ability, Encouragement> ENCOURAGEMENT = ability("inspire_encouragement", Encouragement::new);
    public static final DeferredHolder<Ability, RouseTheWavering> ROUSE = ability("inspire_rouse_the_wavering", RouseTheWavering::new);
    public static final DeferredHolder<Ability, Uprising> UPRISING = ability("inspire_uprising", Uprising::new);
    public static final DeferredHolder<Ability, PeoplesArmy> PEOPLES_ARMY = ability("inspire_peoples_army", PeoplesArmy::new);
    public static final DeferredHolder<Ability, UnbreakableSpirit> UNBREAKABLE = ability("inspire_unbreakable_spirit",
            UnbreakableSpirit::new);

    public static final ResourceLocation ROUSED = GrandLine.id("inspire_roused");
    public static final ResourceLocation ARMY = GrandLine.id("inspire_army");

    public InspireFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Kobu Kobu no Mi", COLOR)
                .abilities(RALLYING_WAVE, FLAG_STRIKE, ENCOURAGEMENT, ROUSE, UPRISING)
                .awakened(PEOPLES_ARMY, UNBREAKABLE));
    }

    static void init() {}

    static void behaviors() {}

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, ROUSED);
        Minions.dismiss(player, ARMY);
    }

    /** Waves the flag: a ring of red and gold, and the cheer that goes with it. */
    static void wave(AbilityContext ctx, float radius) {
        Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, radius, Kind.STAR, Vfx.RING | Vfx.GROUND);
        Vfx.impact(ctx.level(), ctx.player(), ctx.player().getEyePosition().add(0, 1.2, 0), GOLD, 1.6f, Kind.STAR);
    }

    /** Foes within {@code radius} below half health: the ones whose will is wavering. */
    static List<Mob> wavering(ServerPlayer player, double radius) {
        return AbilityDamage.around(player, player.position(), radius).stream()
                .filter(e -> e instanceof Mob && e.getHealth() <= e.getMaxHealth() * 0.5f)
                .map(e -> (Mob) e)
                .toList();
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Wave your flag: you and every ally around you fight stronger and move faster for twenty seconds. */
    public static final class RallyingWave extends Ability {
        RallyingWave() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(300).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : ZoanKit.friends(ctx.player(), 12)) {
                friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int) (400 * ctx.power()), 0));
                friend.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) (400 * ctx.power()), 0));
            }
            wave(ctx, 12f);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.PLAYER_LEVELUP, 1f, 0.8f);
        }
    }

    /** Swing the flagpole through everything in front of you, knocking it back. */
    public static final class FlagStrike extends Ability {
        FlagStrike() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, GOLD).cooldown(35).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5, 70)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 11f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.2, 0.35);
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(2)), ctx.right(), COLOR, 2.6f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.9f);
        }
    }

    /** Shout encouragement: you and your allies nearby shake off fear and fatigue, heal, and harden for ten seconds. */
    public static final class Encouragement extends Ability {
        Encouragement() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, GOLD).cooldown(300).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : ZoanKit.friends(ctx.player(), 12)) {
                friend.heal(6f * ctx.power());
                friend.removeEffect(MobEffects.WEAKNESS);
                friend.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                friend.removeEffect(MobEffects.DIG_SLOWDOWN);
                friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
            }
            wave(ctx, 12f);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.VILLAGER_CELEBRATE, 1.2f, 1f);
        }
    }

    /** Rouse the wavering: up to three badly hurt foes nearby lose their fear and fight for you for twenty seconds. */
    public static final class RouseTheWavering extends Ability {
        RouseTheWavering() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(500).stamina(18).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int roused = 0;
            for (Mob mob : wavering(player, 10)) {
                if (roused >= 3) {
                    break;
                }
                if (Minions.bind(player, ROUSED, mob, (int) (400 * ctx.power()))) {
                    mob.heal(mob.getMaxHealth() * 0.3f);
                    Vfx.impact(ctx.level(), player, mob.getEyePosition().add(0, 0.6, 0), GOLD, 1.2f, Kind.STAR);
                    roused++;
                }
            }
            wave(ctx, 10f);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.RAID_HORN.value(), 0.6f, 1.4f);
        }
    }

    /** Raise an uprising: every ally near you fights far harder, faster and tougher for fifteen seconds. */
    public static final class Uprising extends Ability {
        Uprising() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, GOLD).cooldown(600).stamina(22).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity friend : ZoanKit.friends(player, 15)) {
                friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
                friend.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
                friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
                friend.heal(6f);
            }
            for (LivingEntity foe : AbilityDamage.around(player, player.position(), 8)) {
                foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            }
            wave(ctx, 15f);
            Vfx.burst(ctx.level(), player, player.getEyePosition(), GOLD, 4f, Kind.STAR, Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.RAID_HORN.value(), 1f, 1.1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Call up the people's army: four iron guardians rally to your flag for half a minute, and every wavering foe
     * around you joins them.
     *
     * @name People's Army
     */
    public static final class PeoplesArmy extends Ability {
        PeoplesArmy() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(1200).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Minions.dismiss(player, ARMY);
            for (Vec3 at : Kit.circle(player.position(), 3, 4, 0.4)) {
                Minions.spawn(player, ARMY, EntityType.IRON_GOLEM, Kit.ground(ctx.level(), at.add(0, 2, 0), 6), 600,
                        golem -> golem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0)));
            }
            for (Mob mob : wavering(player, 14)) {
                Minions.bind(player, ROUSED, mob, 600);
            }
            wave(ctx, 14f);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.RAID_HORN.value(), 1.4f, 1f);
        }
    }

    /** For thirty seconds your spirit holds everyone up: allies near you keep healing, hitting harder and taking less. */
    public static final class UnbreakableSpirit extends Ability {
        UnbreakableSpirit() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SUN, GOLD).cooldown(900).stamina(20).staminaPerTick(0.1f).maxActive(600)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            wave(ctx, 10f);
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 1f, 1.3f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 20 == 0) {
                for (LivingEntity friend : ZoanKit.friends(ctx.player(), 10)) {
                    friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 1, false, true));
                    friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, false, true));
                    friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, true));
                }
                Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), GOLD, 10f, Kind.STAR, Vfx.GROUND);
            }
            return true;
        }
    }
}
