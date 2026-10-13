package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

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
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Pamu Pamu no Mi. Make your body, or anything inorganic you touch, swell up like a balloon until it bursts like a
 * bomb, sharp fragments and all.
 */
public class PopFruit extends DevilFruit {
    public static final int COLOR = 0xF08A3A;
    public static final int DARK = 0x3A2A2A;
    static final String M = "fruit/grandline:pop";

    public static final DeferredHolder<Ability, PunkPistol> PUNK_PISTOL = ability("pop_punk_pistol", PunkPistol::new);
    public static final DeferredHolder<Ability, Shrapnel> SHRAPNEL = ability("pop_shrapnel", Shrapnel::new);
    public static final DeferredHolder<Ability, Rupture> RUPTURE = ability("pop_rupture", Rupture::new);
    public static final DeferredHolder<Ability, SwellGuard> SWELL_GUARD = ability("pop_swell_guard", SwellGuard::new);
    public static final DeferredHolder<Ability, BurstField> BURST_FIELD = ability("pop_burst_field", BurstField::new);
    public static final DeferredHolder<Ability, MassRupture> MASS_RUPTURE = ability("pop_mass_rupture", MassRupture::new);
    public static final DeferredHolder<Ability, ShardStorm> SHARD_STORM = ability("pop_shard_storm", ShardStorm::new);

    public static final ResourceLocation BURSTER = GrandLine.id("pop_burster");
    public static final ResourceLocation FIELD = GrandLine.id("pop_field");
    public static final ResourceLocation STORM = GrandLine.id("pop_storm");

    public PopFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Pamu Pamu no Mi", COLOR)
                .abilities(PUNK_PISTOL, SHRAPNEL, RUPTURE, SWELL_GUARD, BURST_FIELD)
                .awakened(MASS_RUPTURE, SHARD_STORM));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BURSTER, Projectiles.blast(3, AbilityDamage.BLAST, null, p -> pop(p.level(), p, p.position(), 3f)));
        // Swollen lumps on the ground that burst a moment later.
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(Integer.MAX_VALUE, (p, target) -> {}, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount == 25) {
                for (LivingEntity target : AbilityDamage.around(owner, p.position(), p.size() / 2.0)) {
                    Kit.hit(owner, target, AbilityDamage.BLAST, p.damage());
                }
                for (Vec3 at : Kit.circle(p.position(), p.size() / 3.0, 5, 0)) {
                    pop(owner.level(), p, at, 1.8f);
                }
                p.discard();
            }
        }));
        ProjectileBehavior.Registry.register(STORM, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.SLASH, p.damage());
            }
        }, null));
    }

    /** The look of something swelling and bursting at {@code at}. */
    static void pop(net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity source, Vec3 at, float size) {
        if (level instanceof ServerLevel server) {
            Vfx.burst(server, source, at, COLOR, size, Kind.FLAME, Vfx.SHOCKWAVE | Vfx.FLASH);
            Vfx.burst(server, source, at, DARK, size, Kind.SHARD, 0);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS,
                    0.8f + size * 0.2f, 1.3f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Swell your fist as it lands so it bursts against whatever it hits.
     *
     * @name Punk Pistol
     */
    public static final class PunkPistol extends Ability {
        PunkPistol() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(50).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.8 * ctx.power(), 40)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 9f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.2, 0.3);
            }
            pop(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.6)), 1.6f);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.INFLATE.get(), 0.8f, 1.6f);
        }
    }

    /** Swell a scrap of metal in your hand and hurl it: it bursts into shrapnel where it lands. */
    public static final class Shrapnel extends Ability {
        Shrapnel() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, DARK).cooldown(60).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, BURSTER, 1.3, 0.6f, DARK, 8f, 30).withGravity(0.03);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.INFLATE.get(), 1f, 1.2f);
        }
    }

    /** Lay a hand on a foe's armour and make it swell and burst: a blast right against its body. */
    public static final class Rupture extends Ability {
        Rupture() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(140).stamina(16).mastery(M, 10).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Vec3 at = target.getBoundingBox().getCenter();
                Kit.hit(player, target, AbilityDamage.BLAST, 12f * ctx.power());
                for (LivingEntity near : AbilityDamage.around(player, at, 3)) {
                    if (near != target) {
                        Kit.hit(player, near, AbilityDamage.BLAST, 6f * ctx.power());
                    }
                }
                pop(ctx.level(), target, at, 2.2f);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.INFLATE.get(), 1.2f, 0.9f);
        }
    }

    /** Puff your body up into a tough, bulging shell. */
    public static final class SwellGuard extends Buffs.Toggle {
        SwellGuard() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(10).staminaPerTick(0.12f)
                            .maxActive(500).mastery(M, 20).masteryGain(0.7),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("pop_swell_armor"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("pop_swell_knockback"), 0.6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.SCALE, GrandLine.id("pop_swell_scale"), 0.2, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            ctx.sound(GLSounds.INFLATE.get(), 1f, 0.8f);
        }
    }

    /** Swell the ground itself where you point: a moment later the lumps burst under everyone standing there. */
    public static final class BurstField extends Ability {
        BurstField() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, DARK).cooldown(160).stamina(16).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            AbilityProjectile.zone(player, FIELD, at, 4 * ctx.power(), COLOR, 12f * ctx.power(), 30);
            Vfx.burst(ctx.level(), player, at, COLOR, 4f, Kind.BUBBLE, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.INFLATE.get(), 1.4f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Every bit of metal on every foe around you swells and bursts at once. */
    public static final class MassRupture extends Ability {
        MassRupture() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.BLAST, (10f + target.getArmorValue() * 0.4f) * ctx.power());
                pop(ctx.level(), target, target.getBoundingBox().getCenter(), 1.6f);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.8f);
        }
    }

    /** Burst a mass of metal high overhead: a storm of shrapnel rains on the spot you point at. */
    public static final class ShardStorm extends Ability {
        ShardStorm() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, DARK).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 26), 8);
            AbilityProjectile.zone(player, STORM, at, 6 * ctx.power(), DARK, 4f * ctx.power(), 100);
            pop(ctx.level(), player, at.add(0, 10, 0), 4f);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 1f);
        }
    }
}
