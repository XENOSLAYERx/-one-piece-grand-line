package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

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
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Kibi Kibi no Mi. Pull millet dumplings from your cheeks: any beast that eats one becomes your loyal companion for a
 * while, and fights at your command.
 */
public class MilletFruit extends DevilFruit {
    public static final int COLOR = 0xF0E0A0;
    public static final int BROWN = 0x8A6A2A;
    static final String M = "fruit/grandline:millet";
    static final int PACK = 4;
    static final int ARMY = 8;
    public static final ResourceLocation TAMED = GrandLine.id("millet_tamed");

    public static final DeferredHolder<Ability, KibiDango> KIBI_DANGO = ability("millet_kibi_dango", KibiDango::new);
    public static final DeferredHolder<Ability, Feast> FEAST = ability("millet_feast", Feast::new);
    public static final DeferredHolder<Ability, SicEm> SIC_EM = ability("millet_sic_em", SicEm::new);
    public static final DeferredHolder<Ability, Heel> HEEL = ability("millet_heel", Heel::new);
    public static final DeferredHolder<Ability, Scatter> SCATTER = ability("millet_scatter", Scatter::new);
    public static final DeferredHolder<Ability, BeastArmy> BEAST_ARMY = ability("millet_beast_army", BeastArmy::new);
    public static final DeferredHolder<Ability, Rally> RALLY = ability("millet_rally", Rally::new);

    public static final ResourceLocation DUMPLING = GrandLine.id("millet_dumpling");

    public MilletFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Kibi Kibi no Mi", COLOR)
                .abilities(KIBI_DANGO, FEAST, SIC_EM, HEEL, SCATTER)
                .awakened(BEAST_ARMY, RALLY));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(DUMPLING, new ProjectileBehavior() {
            @Override
            public boolean onHitEntity(AbilityProjectile p, Entity target) {
                ServerPlayer owner = p.ownerPlayer();
                if (owner == null || target == owner || !(target instanceof LivingEntity living)) {
                    return true;
                }
                feed(owner, living, PACK);
                return false;
            }
        });
    }

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, TAMED);
    }

    /**
     * Someone eats a dumpling: friends and companions are healed; a beast becomes a companion for a minute if the pack
     * has room (bosses refuse).
     */
    static void feed(ServerPlayer owner, LivingEntity eater, int cap) {
        ServerLevel level = owner.serverLevel();
        boolean friend = Minions.isAlly(owner, eater) || (eater instanceof Player && !AbilityDamage.canHarm(owner, eater));
        if (friend) {
            eater.heal(4f);
            Vfx.impact(level, eater, eater.getEyePosition().add(0, 0.4, 0), 0xF06A8A, 0.9f, Kind.HEART);
        } else if (eater instanceof Mob mob && Minions.of(owner, TAMED).size() < cap && Minions.bind(owner, TAMED, mob, 1200)) {
            mob.heal(4f);
            Vfx.burst(level, mob, mob.getEyePosition().add(0, 0.4, 0), 0xF06A8A, 1.2f, Kind.HEART, 0);
            level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EAT, mob.getSoundSource(), 1f, 1.2f);
        } else {
            Vfx.impact(level, eater, eater.getEyePosition(), COLOR, 0.8f, Kind.SMOKE);
        }
    }

    static void throwDumpling(AbilityContext ctx, Vec3 dir) {
        AbilityProjectile.spawn(ctx.player(), DUMPLING, ctx.handPos(), dir.scale(1.2), 0.35f, COLOR, 0f, 40).withGravity(0.03);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Toss a millet dumpling: a beast that eats it is your loyal companion for a minute (up to four). Friends heal. */
    public static final class KibiDango extends Ability {
        KibiDango() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, COLOR).cooldown(60).stamina(8).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            throwDumpling(ctx, ctx.player().getLookAngle());
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SNOWBALL_THROW, 1f, 1.2f);
        }
    }

    /** Share a feast of dumplings: you and every companion heal, and keep healing for a while. */
    public static final class Feast extends Ability {
        Feast() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, BROWN).cooldown(400).stamina(14).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.heal(6f);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            for (Mob pet : Minions.of(player, TAMED)) {
                pet.heal(8f);
                pet.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                Vfx.impact(ctx.level(), pet, pet.getEyePosition().add(0, 0.4, 0), 0xF06A8A, 0.9f, Kind.HEART);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 2f, Kind.HEART, 0);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.GENERIC_EAT, 1.2f, 1f);
        }
    }

    /**
     * Point at a foe: every companion charges it, fired up and stronger for ten seconds.
     *
     * @name Sic 'Em
     */
    public static final class SicEm extends Ability {
        SicEm() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, BROWN).cooldown(160).stamina(10).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (Minions.of(player, TAMED).isEmpty()) {
                return Optional.of(Component.literal("You have no companions"));
            }
            return Kit.target(player, 24).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                for (Mob pet : Minions.of(player, TAMED)) {
                    pet.setTarget(target);
                    pet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                    pet.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                }
                Vfx.burst(ctx.level(), player, target.getEyePosition().add(0, 0.6, 0), BROWN, 1.2f, Kind.RING, Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.WOLF_GROWL, 1.2f, 1f);
        }
    }

    /** Whistle your companions to your side: they appear beside you, hardened for ten seconds. */
    public static final class Heel extends Ability {
        Heel() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(240).stamina(10).mastery(M, 20).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Minions.of(player, TAMED).isEmpty() ? Optional.of(Component.literal("You have no companions")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            List<Mob> pets = Minions.of(player, TAMED);
            List<Vec3> spots = Kit.circle(player.position(), 2, Math.max(1, pets.size()), 0);
            for (int i = 0; i < pets.size(); i++) {
                Mob pet = pets.get(i);
                Vec3 from = pet.position();
                Vec3 to = spots.get(i);
                pet.teleportTo(to.x, to.y, to.z);
                pet.setTarget(null);
                pet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                Vfx.blink(ctx.level(), pet, from, to, COLOR, Kind.SMOKE);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.4f, 1.6f);
        }
    }

    /** Scatter a fan of five dumplings: tame several beasts at once. */
    public static final class Scatter extends Ability {
        Scatter() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(300).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (int i = -2; i <= 2; i++) {
                throwDumpling(ctx, ctx.player().getLookAngle().add(ctx.right().scale(i * 0.15)).normalize());
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.SNOWBALL_THROW, 1.4f, 0.9f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Shower dumplings over everything around you: every beast nearby joins you, up to eight, for a minute. */
    public static final class BeastArmy extends Ability {
        BeastArmy() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, BROWN).cooldown(1200).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                if (target instanceof Mob) {
                    feed(player, target, ARMY);
                }
            }
            Vfx.rain(ctx.level(), player, player.position(), COLOR, 10f, Kind.STAR);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.GENERIC_EAT, 2f, 0.8f);
        }
    }

    /** Rally the pack: every companion is healed and fights stronger, faster and tougher for twenty seconds. */
    public static final class Rally extends Ability {
        Rally() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(900).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Mob pet : Minions.of(player, TAMED)) {
                pet.heal(pet.getMaxHealth());
                pet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 2));
                pet.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
                pet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
                Vfx.burst(ctx.level(), pet, pet.getBoundingBox().getCenter(), BROWN, 1.4f, Kind.SPARK, Vfx.RING);
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 6f, Kind.SPARK, Vfx.RING);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.RAID_HORN.value(), 0.6f, 1.4f);
        }
    }
}
