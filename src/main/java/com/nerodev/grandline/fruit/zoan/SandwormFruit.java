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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mimizu Mimizu no Mi, Model: Sandworm. Become an enormous sandworm, as Saint Shepherd Ju Peter does: it swims through
 * the ground, bursts up beneath its prey, and devours the land and everything standing on it, with the elders' body
 * that will not stay wounded.
 */
public class SandwormFruit extends GoroseiFruit {
    public static final int COLOR = 0xC8A87A;
    public static final int SAND = 0xD8C090;
    static final String M = "fruit/grandline:sandworm";

    /**
     * @hybrid Sandworm Hybrid | Your legs become a sandworm's thick tail: far tougher and stronger, and nothing knocks you over.
     * @beast Sandworm Form | Become an enormous sandworm: colossal, armoured, and able to devour the ground under its prey.
     */
    static final ZoanForms FORMS = bodies("sandworm", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 12, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.8, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 34, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1.5, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 3, Operation.ADD_VALUE)
                    .dimensions(2.8f, 2.8f, 2.0f));

    public static final DeferredHolder<Ability, Devour> DEVOUR = ability("sandworm_devour", Devour::new);
    public static final DeferredHolder<Ability, Burrow> BURROW = ability("sandworm_burrow", Burrow::new);
    public static final DeferredHolder<Ability, Pentagram> PENTAGRAM = ability("sandworm_pentagram", Pentagram::new);
    public static final DeferredHolder<Ability, Eruption> ERUPTION = ability("sandworm_eruption", Eruption::new);
    public static final DeferredHolder<Ability, LandDevourer> LAND_DEVOURER = ability("sandworm_land_devourer", LandDevourer::new);
    public static final DeferredHolder<Ability, WorldEater> WORLD_EATER = ability("sandworm_world_eater", WorldEater::new);
    public static final DeferredHolder<Ability, UndergroundRampage> RAMPAGE = ability("sandworm_underground_rampage",
            UndergroundRampage::new);

    public static final ResourceLocation MAW = GrandLine.id("sandworm_maw");

    public SandwormFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Mimizu Mimizu no Mi, Model: Sandworm", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), DEVOUR, BURROW, PENTAGRAM, ERUPTION, LAND_DEVOURER)
                .awakened(WORLD_EATER, RAMPAGE), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(MAW, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
                Kit.pull(target, p.position(), 0.5);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
                if (owner.getHealth() < owner.getMaxHealth()) {
                    owner.heal(0.5f);
                }
            }
        }, null));
    }

    /** Bursts out of the ground at {@code at}: everything there is crushed and thrown into the air. */
    static void erupt(AbilityContext ctx, Vec3 at, double radius, float damage) {
        ServerPlayer player = ctx.player();
        for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
            Kit.hit(player, target, AbilityDamage.CRUSH, damage);
            Kit.launch(target, new Vec3(0, 1.3, 0));
        }
        Vfx.eruption(ctx.level(), player, at, SAND, (float) radius + 1, Kind.SHARD);
        Vfx.burst(ctx.level(), player, at, COLOR, (float) radius, Kind.SMOKE, Vfx.RING | Vfx.GROUND);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Swallow the foe in front of you whole for a moment: crushing damage, and you mend a little from the meal. */
    public static final class Devour extends Ability {
        Devour() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, COLOR).cooldown(80).stamina(12).masteryGain(0.6));
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
                Kit.encase(target, Vfx.StatusLook.MUD, 30);
                player.heal(4f);
                Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), COLOR, 1.8f, Kind.SMOKE, 0);
            });
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.GENERIC_EAT, 1.6f, 0.5f);
        }
    }

    /** Dive into the ground and burst out where you point, up to twenty-four blocks away, tossing up whatever is there. */
    public static final class Burrow extends Ability {
        Burrow() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, SAND).cooldown(120).stamina(12).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 10);
            Vfx.burst(ctx.level(), player, from, COLOR, 2f, Kind.SHARD, Vfx.GROUND);
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            erupt(ctx, at, 3, 8f * FORMS.boost(player) * ctx.power());
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.ROOTED_DIRT_BREAK, 1.6f, 0.5f);
        }
    }

    /** Step into a pentagram and out of another wherever you look, up to thirty-two blocks away. */
    public static final class Pentagram extends PentagramStep {
        Pentagram() {
            super(SIGIL, M);
        }
    }

    /** Burst up from below the spot you point at: everything standing there is crushed and hurled skyward. */
    public static final class Eruption extends Ability {
        Eruption() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, SAND).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            erupt(ctx, Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 10), 4 * ctx.power(), 16f * FORMS.boost(player) * ctx.power());
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 0.7f);
        }
    }

    /** Devour the ground where you point: for five seconds it becomes a maw that drags in and crushes everything. */
    public static final class LandDevourer extends Ability {
        LandDevourer() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, COLOR).cooldown(300).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 10);
            AbilityProjectile.zone(player, MAW, at, 6 * ctx.power(), COLOR, 5f * ctx.power(), 100);
            Vfx.burst(ctx.level(), player, at, COLOR, (float) (6 * ctx.power()), Kind.SHARD, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.SAND_DRAIN.get(), 1.6f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Open a maw in the ground all around you for six seconds: everything is dragged in, crushed and eaten. */
    public static final class WorldEater extends Ability {
        WorldEater() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, SAND).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, MAW, player.position(), 11 * ctx.power(), SAND, 7f * ctx.power(), 120);
            Vfx.burst(ctx.level(), player, player.position(), SAND, (float) (11 * ctx.power()), Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.SAND_STORM.get(), 2f, 0.4f);
        }
    }

    /** Tunnel along a line twenty-four blocks long: the ground erupts under everything along it. */
    public static final class UndergroundRampage extends Ability {
        UndergroundRampage() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 dir = ctx.horizontalLook();
            double length = 24 * ctx.power();
            for (int i = 1; i <= 8; i++) {
                erupt(ctx, Kit.ground(ctx.level(), from.add(dir.scale(i * length / 8)).add(0, 2, 0), 6), 3, 18f * ctx.power());
            }
            Vec3 end = Kit.ground(ctx.level(), from.add(dir.scale(length)).add(0, 2, 0), 6);
            player.teleportTo(end.x, end.y, end.z);
            player.resetFallDistance();
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.5f);
        }
    }
}
