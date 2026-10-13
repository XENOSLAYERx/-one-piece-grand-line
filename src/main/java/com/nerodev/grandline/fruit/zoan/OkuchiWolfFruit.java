package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.bodies;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.logia.RumbleFruit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Inu Inu no Mi, Model: Okuchi no Makami. Become the great divine wolf that guards Wano, as Yamato does: a guardian
 * of ice and cold that can raise walls of ice, quench flames, and strike with Yamato's own techniques.
 */
public class OkuchiWolfFruit extends ZoanFruit {
    public static final int COLOR = 0xF0F4FF;
    public static final int ICE = 0x6AA8E8;
    public static final int THUNDER = 0xF0E08A;
    static final String M = "fruit/grandline:okuchi_wolf";

    /**
     * @hybrid Divine Wolf Hybrid | Grow a white wolf's ears, tail and icy horns: stronger, faster and untroubled by fire.
     * @beast Okuchi no Makami Form | Become the great white divine wolf: fast, tough and wreathed in cold.
     */
    static final ZoanForms FORMS = bodies("okuchi_wolf", COLOR, () -> SoundEvents.WOLF_HOWL, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.4, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 20, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 0.5, Operation.ADD_VALUE)
                    .dimensions(1.6f, 1.8f, 1.5f));

    public static final DeferredHolder<Ability, NarikaburaArrow> NARIKABURA = ability("okuchi_wolf_narikabura_arrow",
            NarikaburaArrow::new);
    public static final DeferredHolder<Ability, FrostFang> FROST_FANG = ability("okuchi_wolf_frost_fang", FrostFang::new);
    public static final DeferredHolder<Ability, IceWall> ICE_WALL = ability("okuchi_wolf_ice_wall", IceWall::new);
    public static final DeferredHolder<Ability, GlacialHowl> GLACIAL_HOWL = ability("okuchi_wolf_glacial_howl", GlacialHowl::new);
    public static final DeferredHolder<Ability, ThunderBagua> THUNDER_BAGUA = ability("okuchi_wolf_thunder_bagua", ThunderBagua::new);
    public static final DeferredHolder<Ability, DivineBlizzard> DIVINE_BLIZZARD = ability("okuchi_wolf_divine_blizzard",
            DivineBlizzard::new);
    public static final DeferredHolder<Ability, GuardianMantle> GUARDIAN_MANTLE = ability("okuchi_wolf_guardian_mantle",
            GuardianMantle::new);

    public OkuchiWolfFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Inu Inu no Mi, Model: Okuchi no Makami", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), NARIKABURA, FROST_FANG, ICE_WALL, GLACIAL_HOWL, THUNDER_BAGUA)
                .awakened(DIVINE_BLIZZARD, GUARDIAN_MANTLE), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    /** The guardian of ice is untroubled by fire while transformed. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (FORMS.any(user) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * 0.5f);
            user.clearFire();
        }
    }

    /** Chills {@code target}: frost creeps over it and it slows to a crawl. */
    static void chill(LivingEntity target, int ticks) {
        target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + ticks));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
        target.clearFire();
    }

    // ---------------------------------------------------------------------------------------------------------

    /** A single thrust that sends a whistling shockwave straight through everything in a line. */
    public static final class NarikaburaArrow extends Ability {
        NarikaburaArrow() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(70).stamina(12).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double length = 14 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 1.4)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 14f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, player.getLookAngle().scale(1.6).add(0, 0.3, 0));
            }
            Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(length));
            Vfx.beam(ctx.level(), player, ctx.handPos(), end, COLOR, 0.8f, Kind.SHOCKWAVE, Vfx.BEAM_PARTICLES);
            Vfx.burst(ctx.level(), player, ctx.handPos().add(player.getLookAngle()), ICE, 1.6f, Kind.SHOCKWAVE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.2f, 1.3f);
        }
    }

    /** Bite with frozen fangs: a deep wound, and frost spreads through the foe. Needs a form. */
    public static final class FrostFang extends Ability {
        FrostFang() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, ICE).cooldown(35).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                ZoanKit.maul(player, target, 11f * FORMS.boost(player) * ctx.power(), 3, 0);
                chill(target, 100);
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), ICE, 1.2f, Kind.SNOW);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1f, 1.3f);
        }
    }

    /** Raise a wall of ice in front of you that blocks anything trying to pass for ten seconds. */
    public static final class IceWall extends Ability {
        IceWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, ICE).cooldown(240).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 center = Kit.ground(ctx.level(), player.position().add(ctx.horizontalLook().scale(3)), 4);
            Kit.place(ctx.level(), Kit.wall(center, ctx.horizontalLook(), 7, 4), Blocks.PACKED_ICE.defaultBlockState(), 200);
            Vfx.burst(ctx.level(), player, center.add(0, 2, 0), ICE, 3.5f, Kind.SNOW, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.4f, 0.7f);
        }
    }

    /** Howl a freezing wind: everything around you is frozen and battered, and flames on you and your allies go out. */
    public static final class GlacialHowl extends Ability {
        GlacialHowl() {
            super(props(ActivationType.INSTANT, AbilityIcon.SNOWFLAKE, ICE).cooldown(200).stamina(16).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 8 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.FREEZE, 10f * FORMS.boost(player) * ctx.power());
                chill(target, 120);
                AbilityDamage.knockback(target, player.position(), 1.0, 0.3);
            }
            for (LivingEntity friend : ZoanKit.friends(player, radius)) {
                friend.clearFire();
            }
            Vfx.burst(ctx.level(), player, player.position(), ICE, (float) radius, Kind.SNOW, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.WOLF_HOWL, 1.6f, 0.8f);
        }
    }

    /** Close on the foe you face and strike it like a thunderclap. It is knocked senseless. */
    public static final class ThunderBagua extends Ability {
        ThunderBagua() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, THUNDER).cooldown(240).stamina(20).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No one to strike"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 to = target.position().subtract(ctx.horizontalLook().scale(1.4));
                player.teleportTo(to.x, target.getY(), to.z);
                Kit.hit(player, target, AbilityDamage.STRIKE, 22f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(2.0).add(0, 0.5, 0));
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 40, 0, false, false, true));
                RumbleFruit.bolt(ctx.level(), target.position());
                Vfx.trail(ctx.level(), player, from.add(0, 1, 0), to.add(0, 1, 0), THUNDER, 1.0f, Kind.SPARK);
                Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), THUNDER, 2.6f, Kind.SPARK, Vfx.RING | Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.6f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Call down a divine blizzard: everything around you is frozen solid in ice. Needs a form. */
    public static final class DivineBlizzard extends Ability {
        DivineBlizzard() {
            super(props(ActivationType.INSTANT, AbilityIcon.SNOWFLAKE, COLOR).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 11 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.FREEZE, 18f * ctx.power());
                Kit.encase(target, Vfx.StatusLook.ICE, 60);
                chill(target, 160);
            }
            Vfx.rain(ctx.level(), player, player.position().add(0, 6, 0), COLOR, (float) radius, Kind.SNOW);
            Vfx.burst(ctx.level(), player, player.position(), ICE, (float) radius, Kind.SNOW, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 2f, 0.5f);
        }
    }

    /** Wrap yourself in the guardian's cold for thirty seconds: foes near you freeze, and blows lose a third of their force. */
    public static final class GuardianMantle extends Ability {
        GuardianMantle() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, ICE).cooldown(900).stamina(20).staminaPerTick(0.1f).maxActive(600)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), ICE, 3f, Kind.SNOW, Vfx.RING | Vfx.FLASH);
            ctx.sound(SoundEvents.WOLF_HOWL, 1.4f, 1.0f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!FORMS.any(player)) {
                return false;
            }
            if (ctx.activeTicks() % 10 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 6)) {
                    chill(target, 30);
                }
                Vfx.burst(ctx.level(), player, player.position(), ICE, 6f, Kind.SNOW, Vfx.GROUND);
            }
            player.clearFire();
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.67f);
        }
    }
}
