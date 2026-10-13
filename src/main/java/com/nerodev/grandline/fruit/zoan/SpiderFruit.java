package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.bodies;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Kumo Kumo no Mi, Model: Rosamygale Grauvogeli. Become an ancient spider: eight legs that climb any wall, silk that
 * binds whatever it touches, venomous fangs, and (as Black Maria showed) webs that burn with whatever is caught in them.
 */
public class SpiderFruit extends ZoanFruit {
    public static final int COLOR = 0x2A2030;
    public static final int SILK = 0xF0F0F0;
    public static final int VENOM = 0x8A3AA8;
    static final String M = "fruit/grandline:spider";

    /**
     * @hybrid Spider-Woman | Grow a spider's body and legs below the waist: bigger and stronger, and you can climb walls.
     * @beast Spider Form | Become a giant ancient spider: fast, tough and able to run up any wall.
     */
    static final ZoanForms FORMS = bodies("spider", COLOR, () -> SoundEvents.SPIDER_AMBIENT, ZoanForms.Traits.CLIMBER,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.SCALE, 0.4, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.25, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 16, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 0.5, Operation.ADD_VALUE)
                    .dimensions(2.4f, 1.6f, 1.3f));

    public static final DeferredHolder<Ability, WebShot> WEB_SHOT = ability("spider_web_shot", WebShot::new);
    public static final DeferredHolder<Ability, VenomBite> VENOM_BITE = ability("spider_venom_bite", VenomBite::new);
    public static final DeferredHolder<Ability, WebWall> WEB_WALL = ability("spider_web_wall", WebWall::new);
    public static final DeferredHolder<Ability, BurningSilk> BURNING_SILK = ability("spider_burning_silk", BurningSilk::new);
    public static final DeferredHolder<Ability, WebTrap> WEB_TRAP = ability("spider_web_trap", WebTrap::new);
    public static final DeferredHolder<Ability, SpiderNest> SPIDER_NEST = ability("spider_nest", SpiderNest::new);
    public static final DeferredHolder<Ability, SilkInferno> SILK_INFERNO = ability("spider_silk_inferno", SilkInferno::new);

    public static final ResourceLocation WEB = GrandLine.id("spider_web");
    public static final ResourceLocation TRAP = GrandLine.id("spider_web_trap");
    public static final ResourceLocation NEST = GrandLine.id("spider_nest");

    public SpiderFruit() {
        super(Properties.of(FruitType.ANCIENT_ZOAN, GLRarity.EPIC, "Kumo Kumo no Mi, Model: Rosamygale Grauvogeli", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), WEB_SHOT, VENOM_BITE, WEB_WALL, BURNING_SILK, WEB_TRAP)
                .awakened(SPIDER_NEST, SILK_INFERNO), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(WEB, Projectiles.bolt(AbilityDamage.STRIKE, (p, target) -> {
            Kit.root(target, Vfx.StatusLook.STRINGS, 60);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        }));
        ProjectileBehavior.Registry.register(TRAP, Projectiles.zone(10, (p, target) -> {
            Kit.root(target, Vfx.StatusLook.STRINGS, 20);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
        }, null));
        ProjectileBehavior.Registry.register(NEST, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.POISON, p.damage());
                Kit.root(target, Vfx.StatusLook.STRINGS, 30);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Shoot a sticky strand of silk: whatever it hits is bound in place for three seconds. Needs a form. */
    public static final class WebShot extends Ability {
        WebShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, SILK).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, WEB, 1.8, 0.4f, SILK, 4f * FORMS.boost(ctx.player()), 30);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.TRIPWIRE_ATTACH, 1f, 0.7f);
        }
    }

    /** Sink venomous fangs into whatever is in front of you: a deep, poisoned bite. Needs a form. */
    public static final class VenomBite extends Ability {
        VenomBite() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, VENOM).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4).ifPresent(target -> {
                ZoanKit.maul(player, target, 9f * FORMS.boost(player) * ctx.power(), 3, 0);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2));
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), VENOM, 1f, Kind.DROP);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.SPIDER_AMBIENT, 1f, 0.6f);
        }
    }

    /** Spin a wall of webbing in front of you that stops anything trying to pass for ten seconds. */
    public static final class WebWall extends Ability {
        WebWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, SILK).cooldown(200).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 center = Kit.ground(ctx.level(), player.position().add(ctx.horizontalLook().scale(3)), 4);
            Kit.place(ctx.level(), Kit.wall(center, ctx.horizontalLook(), 5, 3), Blocks.COBWEB.defaultBlockState(), 200);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), SILK, 2.5f, Kind.STREAK, 0);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.TRIPWIRE_ATTACH, 1.2f, 0.5f);
        }
    }

    /** Fling burning silk in a line: everything it touches is bound, cut and set ablaze. */
    public static final class BurningSilk extends Ability {
        BurningSilk() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, 0xE86A2A).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double length = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 1.4)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 10f * FORMS.boost(player) * ctx.power());
                target.igniteForSeconds(5);
                Kit.root(target, Vfx.StatusLook.STRINGS, 40);
            }
            Vec3 from = ctx.handPos();
            Vec3 to = player.getEyePosition().add(player.getLookAngle().scale(length));
            Vfx.beam(ctx.level(), player, from, to, SILK, 0.4f, Kind.STREAK, Vfx.BEAM_PARTICLES);
            Vfx.trail(ctx.level(), player, from, to, 0xE86A2A, 0.8f, Kind.FLAME);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.FIRE_GUN.get(), 1f, 0.9f);
        }
    }

    /** Lay a field of web where you point: for eight seconds everything that walks in is caught and held. */
    public static final class WebTrap extends Ability {
        WebTrap() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(300).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 8);
            AbilityProjectile.zone(player, TRAP, at, 6 * ctx.power(), SILK, 0f, 160);
            Vfx.burst(ctx.level(), player, at, SILK, (float) (6 * ctx.power()), Kind.STREAK, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.TRIPWIRE_ATTACH, 1.4f, 0.4f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Turn the ground around you into a spider's nest for ten seconds: everything caught in it is bound and poisoned.
     *
     * @name Spider Nest
     */
    public static final class SpiderNest extends Ability {
        SpiderNest() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, VENOM).cooldown(700).stamina(26).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            AbilityProjectile.zone(player, NEST, player.position(), radius, SILK, 4f * ctx.power(), 200);
            for (Vec3 at : Kit.circle(player.position(), radius * 0.7, 8, 0)) {
                Vec3 ground = Kit.ground(ctx.level(), at, 4);
                Kit.place(ctx.level(), List.of(BlockPos.containing(ground)), Blocks.COBWEB.defaultBlockState(), 200);
            }
            Vfx.burst(ctx.level(), player, player.position(), SILK, (float) radius, Kind.STREAK, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.SPIDER_AMBIENT, 1.6f, 0.4f);
        }
    }

    /** Wrap everything around you in silk, then set the silk alight: they burn bound and helpless. */
    public static final class SilkInferno extends Ability {
        SilkInferno() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, 0xE86A2A).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 8 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.encase(target, Vfx.StatusLook.STRINGS, 40);
                Kit.hit(player, target, AbilityDamage.FIRE, 14f * ctx.power());
                target.igniteForSeconds(8);
                Vfx.eruption(ctx.level(), player, target.position(), 0xE86A2A, 2.5f, Kind.FLAME);
            }
            Vfx.burst(ctx.level(), player, player.position(), 0xE86A2A, (float) radius, Kind.FLAME, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 1.4f, 0.8f);
        }
    }
}
