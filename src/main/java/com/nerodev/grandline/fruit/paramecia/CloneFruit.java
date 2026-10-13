package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Disguise;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mane Mane no Mi. Touch a face with your right hand to memorise it; then take that face and body whenever you like.
 * Creatures are fooled by one of their own. Bon Clay's Okama Kenpo ballet kicks do the rest.
 */
public class CloneFruit extends DevilFruit {
    public static final int COLOR = 0xF08AC0;
    public static final int SWAN = 0x3A3A8A;
    static final String M = "fruit/grandline:clone";
    static final String MEMORY = "grandline_clone_memory";

    public static final DeferredHolder<Ability, Memoire> MEMOIRE = ability("clone_memoire", Memoire::new);
    public static final DeferredHolder<Ability, Mimic> MIMIC = ability("clone_mimic", Mimic::new);
    public static final DeferredHolder<Ability, SwanKick> SWAN_KICK = ability("clone_swan_kick", SwanKick::new);
    public static final DeferredHolder<Ability, Arabesque> ARABESQUE = ability("clone_arabesque", Arabesque::new);
    public static final DeferredHolder<Ability, HauteCouture> HAUTE_COUTURE = ability("clone_haute_couture", HauteCouture::new);
    public static final DeferredHolder<Ability, CloneArmy> ARMY = ability("clone_army", CloneArmy::new);
    public static final DeferredHolder<Ability, GrandBallet> GRAND_BALLET = ability("clone_grand_ballet", GrandBallet::new);

    public static final ResourceLocation ARMY_KIND = GrandLine.id("clone_army");

    public CloneFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Mane Mane no Mi", COLOR)
                .abilities(MEMOIRE, MIMIC, SWAN_KICK, ARABESQUE, HAUTE_COUTURE)
                .awakened(ARMY, GRAND_BALLET));
    }

    static void init() {}

    static void behaviors() {}

    /** The memorised face: "player:&lt;uuid&gt;", an entity type id, or "" if none. */
    static String memory(ServerPlayer player) {
        return player.getPersistentData().getString(MEMORY);
    }

    @Nullable
    static EntityType<?> memorisedType(ServerPlayer player) {
        String memory = memory(player);
        if (memory.isEmpty() || memory.startsWith("player:")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(memory);
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    @Override
    public void onLost(ServerPlayer player) {
        Disguise.clear(player);
        player.getPersistentData().remove(MEMORY);
        Minions.dismiss(player, ARMY_KIND);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Touch a creature or player in front of you to memorise its face and body (a light slap, too). */
    public static final class Memoire extends Ability {
        Memoire() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(40).stamina(6).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Touch someone first"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                CompoundTag data = player.getPersistentData();
                if (target instanceof Player other) {
                    data.putString(MEMORY, "player:" + other.getUUID());
                } else {
                    data.putString(MEMORY, BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
                }
                Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
                player.displayClientMessage(Component.literal("Memorised: ").append(target.getDisplayName()), true);
                Vfx.burst(ctx.level(), target, target.getEyePosition(), COLOR, 0.8f, Kind.STAR, 0);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.PLAYER_ATTACK_WEAK, 1f, 1.4f);
        }
    }

    /** Take the face and body you memorised. Creatures of that kind won't attack you unless you hurt them. */
    public static final class Mimic extends Ability {
        Mimic() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(60).stamina(10).staminaPerTick(0.05f)
                    .maxActive(6000).masteryGain(0.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return memory(player).isEmpty() ? Optional.of(Component.literal("Memorise a face first")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            String memory = memory(player);
            if (memory.startsWith("player:")) {
                try {
                    Disguise.asPlayer(player, UUID.fromString(memory.substring("player:".length())));
                } catch (IllegalArgumentException e) {
                    return;
                }
            } else {
                EntityType<?> type = memorisedType(player);
                if (type == null) {
                    return;
                }
                Disguise.as(player, type);
            }
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1.5f, Kind.SMOKE, Vfx.FLASH);
            ctx.sound(GLSounds.INFLATE.get(), 0.8f, 1.6f);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Disguise.clear(ctx.player());
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.2f, Kind.SMOKE, 0);
        }
    }

    /**
     * Two quick ballet kicks to whoever is in front of you.
     *
     * @name Okama Kenpo: Swan Kick
     */
    public static final class SwanKick extends Ability {
        SwanKick() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, SWAN).cooldown(40).stamina(8).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.8 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * ctx.power());
                Kit.hit(player, target, AbilityDamage.STRIKE, 4f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 0.9, 0.3);
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.5)), player.getLookAngle(), COLOR, 1.6f, 2);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.3f);
        }
    }

    /**
     * Dart forward in a ballet leap, kicking straight through a line of foes.
     *
     * @name Swan Arabesque
     */
    public static final class Arabesque extends Ability {
        Arabesque() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(70).stamina(12).mastery(M, 20)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 8 * ctx.power(), 1.0)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.2, 0.4);
            }
            Vec3 start = player.position();
            Kit.dash(player, 1.5, 0.2);
            Vfx.trail(ctx.level(), player, start.add(0, 1, 0), start.add(player.getLookAngle().scale(8)).add(0, 1, 0), COLOR, 0.6f,
                    Kind.FEATHER);
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 0.8f, 1.6f);
        }
    }

    /**
     * Hold for a whirling flurry of pointed-toe kicks.
     *
     * @name Okama Way: Haute Couture
     */
    public static final class HauteCouture extends Ability {
        HauteCouture() {
            super(props(ActivationType.HOLD, AbilityIcon.SWIRL, COLOR).cooldown(140).stamina(12).staminaPerTick(0.35f)
                    .maxActive(40).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FRENZY);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 3 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.cone(player, 4 * ctx.power(), 50)) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 2.5f * ctx.power());
                }
                Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.6)), COLOR, 0.8f, Kind.FEATHER);
                ctx.sound(SoundEvents.PLAYER_ATTACK_WEAK, 0.7f, 1.6f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Copy the creature you memorised three times over: three doubles fight for you for twenty seconds. */
    public static final class CloneArmy extends Ability {
        CloneArmy() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(900).stamina(36).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return memorisedType(player) != null ? Optional.empty() : Optional.of(Component.literal("Memorise a creature first"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            EntityType<?> type = memorisedType(player);
            if (type == null) {
                return;
            }
            Minions.dismiss(player, ARMY_KIND);
            for (Vec3 at : Kit.circle(player.position(), 2.5, 3, player.getYRot() * Math.PI / 180)) {
                Minions.spawnAny(player, ARMY_KIND, type, at, 400, null);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.INFLATE.get(), 1.2f, 1.2f);
        }
    }

    /**
     * Spin on one toe with your leg outstretched, kicking everything around you away.
     *
     * @name Grand Swan Ballet
     */
    public static final class GrandBallet extends Ability {
        GrandBallet() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, SWAN).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 5 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position().add(0, 1, 0), radius)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 14f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.8, 0.5);
            }
            Vfx.spiral(ctx.level(), player, player.position().add(0, 1, 0), COLOR, (float) radius, Kind.FEATHER);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), SWAN, (float) radius, Kind.FEATHER, Vfx.RING | Vfx.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.9f);
        }
    }
}
