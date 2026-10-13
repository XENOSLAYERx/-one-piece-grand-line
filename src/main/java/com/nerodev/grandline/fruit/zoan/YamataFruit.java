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
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Hebi Hebi no Mi, Model: Yamata no Orochi. Become the eight-headed serpent of legend, as Kurozumi Orochi does: eight
 * heads that bite all at once, coils that crush, and a body that lives on as long as a single head still does.
 */
public class YamataFruit extends ZoanFruit {
    public static final int COLOR = 0x5A3A6A;
    public static final int GOLD = 0xE8C040;
    static final String M = "fruit/grandline:yamata";
    static final String HEADS = "grandline_yamata_heads";
    static final int SPARE_HEADS = 7;

    /**
     * @hybrid Yamata-Man | Four serpent heads grow from your back: stronger, tougher, and every head bites.
     * @beast Yamata no Orochi Form | Become the eight-headed serpent: colossal, armoured, and alive while one head lives.
     */
    static final ZoanForms FORMS = bodies("yamata", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 30, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(3.2f, 3.0f, 2.6f));

    public static final DeferredHolder<Ability, EightFangs> EIGHT_FANGS = ability("yamata_eight_fangs", EightFangs::new);
    public static final DeferredHolder<Ability, CoilCrush> COIL_CRUSH = ability("yamata_coil_crush", CoilCrush::new);
    public static final DeferredHolder<Ability, WrithingNecks> WRITHING_NECKS = ability("yamata_writhing_necks", WrithingNecks::new);
    public static final DeferredHolder<Ability, HeadLunge> HEAD_LUNGE = ability("yamata_head_lunge", HeadLunge::new);
    public static final DeferredHolder<Ability, SerpentRampage> SERPENT_RAMPAGE = ability("yamata_serpent_rampage",
            SerpentRampage::new);
    public static final DeferredHolder<Ability, EightHeadedStorm> EIGHT_HEADED_STORM = ability("yamata_eight_headed_storm",
            EightHeadedStorm::new);
    public static final DeferredHolder<Ability, Regrowth> REGROWTH = ability("yamata_regrowth", Regrowth::new);

    public YamataFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Hebi Hebi no Mi, Model: Yamata no Orochi", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), EIGHT_FANGS, COIL_CRUSH, WRITHING_NECKS, HEAD_LUNGE,
                        SERPENT_RAMPAGE)
                .awakened(EIGHT_HEADED_STORM, REGROWTH), FORMS);
    }

    static void init() {}

    static void behaviors() {
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, YamataFruit::onDeath);
    }

    /** Spare heads left (seven of the eight; the last one is the user's life). */
    static int heads(ServerPlayer player) {
        return player.getPersistentData().contains(HEADS) ? player.getPersistentData().getInt(HEADS) : SPARE_HEADS;
    }

    static void setHeads(ServerPlayer player, int heads) {
        player.getPersistentData().putInt(HEADS, Math.max(0, Math.min(SPARE_HEADS, heads)));
    }

    /** A severed head grows back every two minutes (one minute once awakened). */
    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        super.tick(user, awakened);
        if (user.tickCount % (awakened ? 1200 : 2400) == 0 && heads(user) < SPARE_HEADS) {
            setHeads(user, heads(user) + 1);
            user.displayClientMessage(Component.literal("A severed head grows back (" + (heads(user) + 1) + " of 8)"), true);
        }
    }

    /** As long as one head is left, a killing blow only severs another one. */
    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(FruitManager.fruitOf(player) instanceof YamataFruit)
                || !FORMS.any(player) || heads(player) <= 0) {
            return;
        }
        event.setCanceled(true);
        setHeads(player, heads(player) - 1);
        player.setHealth(player.getMaxHealth() * 0.3f);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3));
        Vfx.burst(player.serverLevel(), player, player.getBoundingBox().getCenter(), COLOR, 3f, Kind.DROP, Vfx.FLASH);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_HURT, SoundSource.PLAYERS, 1.4f, 0.6f);
        player.displayClientMessage(Component.literal("A head is severed! " + (heads(player) + 1) + " of 8 remain"), true);
    }

    /** The heads strike from several directions at once: streaks from above and around the user onto {@code target}. */
    static void strikes(AbilityContext ctx, LivingEntity target, int count) {
        ServerPlayer player = ctx.player();
        Vec3 base = player.getEyePosition().add(0, 1, 0);
        for (int i = 0; i < count; i++) {
            Vec3 from = base.add(ctx.right().scale((i - (count - 1) / 2.0) * 0.8)).add(0, (i % 2) * 0.8, 0);
            Vfx.trail(ctx.level(), player, from, target.getEyePosition(), COLOR, 0.6f, Kind.STREAK);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Every head bites at once: all foes in front of you are torn and left bleeding. Needs a form. */
    public static final class EightFangs extends Ability {
        EightFangs() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, GOLD).cooldown(40).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int heads = FORMS.beast(player) ? 8 : 4;
            for (LivingEntity target : AbilityDamage.cone(player, FORMS.beast(player) ? 7 : 5, 70)) {
                ZoanKit.maul(player, target, (6f + heads) * FORMS.boost(player) * ctx.power(), 4, 0);
                strikes(ctx, target, 3);
            }
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.CLAW.get(), 1.2f, 0.6f);
        }
    }

    /** Wrap your coils around the foe you face and squeeze: crushing damage, and it is held fast. Needs a form. */
    public static final class CoilCrush extends Ability {
        CoilCrush() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, COLOR).cooldown(140).stamina(12).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 6).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 14f * FORMS.boost(player) * ctx.power());
                Kit.root(target, Vfx.StatusLook.COILS, 50);
                Vfx.spiral(ctx.level(), player, target.position(), COLOR, 1.4f, Kind.STREAK);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(SoundEvents.SLIME_SQUISH, 1.2f, 0.5f);
        }
    }

    /** Your necks thrash in every direction: everything around you is battered and thrown back. */
    public static final class WrithingNecks extends Ability {
        WrithingNecks() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(100).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = (FORMS.beast(player) ? 7 : 5) * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * FORMS.boost(player) * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.6, 0.4);
            }
            Vfx.spiral(ctx.level(), player, player.position(), COLOR, (float) radius, Kind.STREAK);
            Vfx.burst(ctx.level(), player, player.position(), GOLD, (float) radius, Kind.SHOCKWAVE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_FRENZY);
            ctx.sound(SoundEvents.RAVAGER_ATTACK, 1.4f, 0.8f);
        }
    }

    /** Shoot every head forward along a line: whatever they bite is dragged back to you. Needs a form. */
    public static final class HeadLunge extends Ability {
        HeadLunge() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, GOLD).cooldown(120).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double length = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 2.0)) {
                ZoanKit.maul(player, target, 14f * FORMS.boost(player) * ctx.power(), 3, 0);
                Kit.pull(target, player.position().add(ctx.horizontalLook().scale(2)), 1.2);
            }
            Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(length));
            for (int i = -1; i <= 1; i++) {
                Vec3 off = ctx.right().scale(i * 0.9);
                Vfx.trail(ctx.level(), player, player.getEyePosition().add(off), end.add(off), COLOR, 0.8f, Kind.STREAK);
            }
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.CLAW.get(), 1.4f, 0.5f);
        }
    }

    /** Charge with every head snapping: everything in your path is gored aside and left bleeding. Needs a form. */
    public static final class SerpentRampage extends Ability {
        SerpentRampage() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(200).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 14 * ctx.power();
            for (LivingEntity target : ZoanKit.charge(player, length, 2.6, 16f * FORMS.boost(player) * ctx.power(), 1.4)) {
                target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 80, 1));
            }
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), COLOR, 2.4f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Eight heads strike again and again in every direction: everything around you is bitten to pieces.
     *
     * @name Eight-Headed Storm
     */
    public static final class EightHeadedStorm extends Ability {
        EightHeadedStorm() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, GOLD).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                for (int i = 0; i < 4; i++) {
                    ZoanKit.maul(player, target, 5f * ctx.power(), 5, 1);
                }
                strikes(ctx, target, 4);
            }
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 10f, Kind.SHOCKWAVE, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_FRENZY);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
        }
    }

    /** Regrow every severed head at once and close half your wounds. Needs a form. */
    public static final class Regrowth extends Ability {
        Regrowth() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, GOLD).cooldown(2400).stamina(24).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            setHeads(player, SPARE_HEADS);
            player.heal(player.getMaxHealth() * 0.5f);
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), GOLD, 3f, Kind.GLOW, Vfx.RING | Vfx.FLASH);
            player.displayClientMessage(Component.literal("All eight heads are whole again"), true);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.6f, 1.2f);
        }
    }
}
