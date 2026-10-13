package com.nerodev.grandline.fruit.logia;

import static com.nerodev.grandline.fruit.logia.LogiaBatch.ability;
import static com.nerodev.grandline.fruit.logia.LogiaBatch.props;

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
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Suna Suna no Mi. A body of sand that drinks the moisture out of anything it grips. Blades of sand split the
 * ground, storms blind and grind, quicksand swallows. Water is its bane: a wet body turns solid and can be hit.
 */
public class SandFruit extends LogiaFruit {
    public static final int COLOR = 0xD8B46A;
    static final String M = "fruit/grandline:sand";

    public static final DeferredHolder<Ability, DesertSpada> DESERT_SPADA = ability("desert_spada", DesertSpada::new);
    public static final DeferredHolder<Ability, Sables> SABLES = ability("sables", Sables::new);
    public static final DeferredHolder<Ability, GroundSecco> GROUND_SECCO = ability("ground_secco", GroundSecco::new);
    public static final DeferredHolder<Ability, DesertGirasole> DESERT_GIRASOLE = ability("desert_girasole", DesertGirasole::new);
    public static final DeferredHolder<Ability, SandShift> SAND_SHIFT = ability("sand_shift", SandShift::new);
    public static final DeferredHolder<Ability, DesertEncierro> DESERT_ENCIERRO = ability("desert_encierro", DesertEncierro::new);
    public static final DeferredHolder<Ability, GreatSandstorm> GREAT_SANDSTORM = ability("great_sandstorm", GreatSandstorm::new);

    public static final ResourceLocation SABLES_STORM = GrandLine.id("sables");
    public static final ResourceLocation QUICKSAND = GrandLine.id("quicksand");
    public static final ResourceLocation SANDSTORM_FIELD = GrandLine.id("great_sandstorm");

    public SandFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.LEGENDARY, "Suna Suna no Mi", COLOR)
                .abilities(DESERT_SPADA, SABLES, GROUND_SECCO, DESERT_GIRASOLE, SAND_SHIFT)
                .awakened(DESERT_ENCIERRO, GREAT_SANDSTORM));
    }

    static void init() {}

    static void behaviors() {
        // A slow-moving sandstorm that drags in, blinds and grinds whatever it passes.
        ProjectileBehavior.Registry.register(SABLES_STORM, Projectiles.zone(5, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.DRAIN, p.damage());
                Kit.pull(target, p.position(), 0.35);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
            }
        }, null));
        // Quicksand: anything inside sinks and cannot walk out.
        ProjectileBehavior.Registry.register(QUICKSAND, Projectiles.zone(4, (p, target) -> {
            Kit.root(target, Vfx.StatusLook.SAND, 12);
            target.setDeltaMovement(target.getDeltaMovement().multiply(0.3, 1, 0.3).add(0, -0.15, 0));
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 20 == 0) {
                Kit.hit(owner, target, AbilityDamage.DRAIN, p.damage());
            }
        }, null));
        ProjectileBehavior.Registry.register(SANDSTORM_FIELD, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.DRAIN, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1));
            }
        }, null));
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return false;
    }

    @Override
    protected boolean intangible(ServerPlayer user) {
        // Wet sand clumps together: in water or rain the body is solid.
        return super.intangible(user) && !user.isInWaterOrRain();
    }

    // ---------------------------------------------------------------------------------------------------------

    /** A blade of sand races along the ground, splitting it and cutting everyone in its path. */
    public static final class DesertSpada extends Ability {
        DesertSpada() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(70).stamina(16).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Set<Integer> hit = new HashSet<>();
            Vec3 start = player.position();
            int length = (int) (16 * ctx.power());
            for (int d = 1; d <= length; d++) {
                Vec3 point = Kit.ground(ctx.level(), start.add(dir.scale(d)).add(0, 1.5, 0), 4);
                for (LivingEntity target : AbilityDamage.around(player, point.add(0, 0.8, 0), 1.4)) {
                    if (hit.add(target.getId())) {
                        Kit.hit(player, target, AbilityDamage.SLASH, 9f * ctx.power());
                        Kit.launch(target, target.getDeltaMovement().add(0, 0.55, 0));
                    }
                }
                if (d % 2 == 0) {
                    Vfx.eruption(ctx.level(), player, point, COLOR, 1.5f + d * 0.08f, Kind.SHARD);
                }
            }
            Vfx.trail(ctx.level(), player, start, start.add(dir.scale(length)), COLOR, 1.2f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.SAND_SLASH.get(), 1.2f, 0.9f);
        }
    }

    /** Send a sandstorm rolling forward that pulls in, blinds and grinds down everything it passes. */
    public static final class Sables extends Ability {
        Sables() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, COLOR).cooldown(140).stamina(22).mastery(M, 10)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 start = player.position().add(ctx.horizontalLook().scale(2));
            AbilityProjectile.spawn(player, SABLES_STORM, start, ctx.horizontalLook().scale(0.3), 6f * ctx.power(), COLOR,
                    3f * ctx.power(), 90);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.SAND_STORM.get(), 1.4f, 1f);
        }
    }

    /** Grip a foe and drink the water from their body: heavy damage, weakness and thirst. You recover a little. */
    public static final class GroundSecco extends Ability {
        GroundSecco() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(90).stamina(14).mastery(M, 20)
                    .masteryGain(0.8));
        }

        @Override
        public Optional<net.minecraft.network.chat.Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty()
                    : Optional.of(net.minecraft.network.chat.Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.DRAIN, 10f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 2));
                player.heal(4f);
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), COLOR, 1.5f, Kind.SHARD, 0);
                Vfx.trail(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.handPos(), 0x9AD8F0, 0.6f, Kind.DROP);
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.SAND_DRAIN.get(), 1f, 1f);
        }
    }

    /** Turn the ground ahead into quicksand that holds and slowly swallows anyone caught in it. */
    public static final class DesertGirasole extends Ability {
        DesertGirasole() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, COLOR).cooldown(200).stamina(24).mastery(M, 35)
                    .masteryGain(1.2));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 22), 6);
            AbilityProjectile.zone(player, QUICKSAND, at, 5 * ctx.power(), COLOR, 2f * ctx.power(), 110);
            Vfx.burst(ctx.level(), player, at, COLOR, 5f, Kind.SHARD, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SAND_STORM.get(), 1f, 0.6f);
        }
    }

    /** Scatter into sand and reform further ahead. */
    public static final class SandShift extends Ability {
        SandShift() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(50).stamina(10).mastery(M, 50)
                    .masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.blink(ctx.player(), 14 * ctx.power(), COLOR, Kind.SHARD);
            ctx.sound(GLSounds.SAND_STORM.get(), 0.8f, 1.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A pillar of sand closes around your target, holding it fast while it dries out. */
    public static final class DesertEncierro extends Ability {
        DesertEncierro() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(240).stamina(26).masteryGain(1.5));
        }

        @Override
        public Optional<net.minecraft.network.chat.Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 24).isPresent() ? Optional.empty()
                    : Optional.of(net.minecraft.network.chat.Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.SAND, 70);
                Kit.hit(player, target, AbilityDamage.DRAIN, 8f * ctx.power());
                Vfx.eruption(ctx.level(), player, target.position(), COLOR, 4f, Kind.SHARD);
            });
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SAND_STORM.get(), 1.4f, 0.8f);
        }
    }

    /** Become the eye of a great sandstorm that blinds, starves and grinds everyone around you. */
    public static final class GreatSandstorm extends Ability {
        GreatSandstorm() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TORNADO, COLOR).cooldown(500).stamina(30).staminaPerTick(0.25f)
                    .maxActive(200).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, SANDSTORM_FIELD, player.position(), 12 * ctx.power(), COLOR, 2f * ctx.power(), 210)
                    .following(Vec3.ZERO);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SAND_STORM.get(), 2f, 0.7f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 20 == 0) {
                ctx.sound(GLSounds.SAND_STORM.get(), 1.2f, 0.8f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.discard(ctx.player(), SANDSTORM_FIELD);
        }
    }
}
