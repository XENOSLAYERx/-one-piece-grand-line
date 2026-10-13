package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.props;

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
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mato Mato no Mi. Touch something to mark it as a target; from then on, anything the user throws curves through the
 * air to it for as long as the mark lasts.
 */
public class MarkFruit extends DevilFruit {
    public static final int COLOR = 0xE84A4A;
    public static final int STEEL = 0xE0E4E8;
    static final String M = "fruit/grandline:mark";
    static final String MARKED_BY = "grandline_marked_by";
    static final String MARKED_UNTIL = "grandline_marked_until";
    static final String TARGET = "target";
    static final int MARK_TICKS = 1200;

    public static final DeferredHolder<Ability, MarkTouch> MARK_TOUCH = ability("mark_touch", MarkTouch::new);
    public static final DeferredHolder<Ability, MarkedKnife> MARKED_KNIFE = ability("mark_knife", MarkedKnife::new);
    public static final DeferredHolder<Ability, KnifeVolley> KNIFE_VOLLEY = ability("mark_volley", KnifeVolley::new);
    public static final DeferredHolder<Ability, AnchorDrop> ANCHOR_DROP = ability("mark_anchor_drop", AnchorDrop::new);
    public static final DeferredHolder<Ability, HomingBarrage> HOMING_BARRAGE = ability("mark_barrage", HomingBarrage::new);
    public static final DeferredHolder<Ability, MarkAll> MARK_ALL = ability("mark_all", MarkAll::new);
    public static final DeferredHolder<Ability, ShipDrop> SHIP_DROP = ability("mark_ship_drop", ShipDrop::new);

    public static final ResourceLocation KNIFE = GrandLine.id("mark_knife");
    public static final ResourceLocation ANCHOR = GrandLine.id("mark_anchor");

    public MarkFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Mato Mato no Mi", COLOR)
                .abilities(MARK_TOUCH, MARKED_KNIFE, KNIFE_VOLLEY, ANCHOR_DROP, HOMING_BARRAGE)
                .awakened(MARK_ALL, SHIP_DROP));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(KNIFE, seeking(0.35, Projectiles.bolt(AbilityDamage.SLASH, null)));
        ProjectileBehavior.Registry.register(ANCHOR, seeking(0.5, Projectiles.blast(2.5, AbilityDamage.CRUSH, null, p -> {
            if (p.level() instanceof ServerLevel level) {
                Vfx.burst(level, p, p.position(), STEEL, 2.5f, Kind.SHARD, Vfx.SHOCKWAVE | Vfx.GROUND);
            }
        })));
    }

    /**
     * Wraps a behaviour so the projectile flies to the creature whose id is stored in its extra data, ignoring
     * everything else in the way; without a mark it flies straight.
     */
    static ProjectileBehavior seeking(double turn, ProjectileBehavior inner) {
        return new ProjectileBehavior() {
            @Override
            public void tick(AbilityProjectile p) {
                if (p.extra().hasUUID(TARGET) && p.level() instanceof ServerLevel level) {
                    Entity target = level.getEntity(p.extra().getUUID(TARGET));
                    if (target != null && target.isAlive()) {
                        Vec3 velocity = p.getDeltaMovement();
                        double speed = Math.max(0.6, velocity.length());
                        Vec3 to = target.getBoundingBox().getCenter().subtract(p.position()).normalize();
                        p.setDeltaMovement(velocity.normalize().lerp(to, turn).normalize().scale(speed));
                    }
                }
                inner.tick(p);
            }

            @Override
            public boolean onHitEntity(AbilityProjectile p, Entity target) {
                return inner.onHitEntity(p, target);
            }

            @Override
            public void onHitBlock(AbilityProjectile p, BlockHitResult hit) {
                inner.onHitBlock(p, hit);
            }

            @Override
            public void onExpire(AbilityProjectile p) {
                inner.onExpire(p);
            }

            @Override
            public boolean hitsBlocks() {
                return inner.hitsBlocks();
            }
        };
    }

    static void mark(ServerPlayer player, LivingEntity target) {
        target.getPersistentData().putUUID(MARKED_BY, player.getUUID());
        target.getPersistentData().putLong(MARKED_UNTIL, player.level().getGameTime() + MARK_TICKS);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_TICKS, 0, false, false));
        Vfx.burst(player.serverLevel(), target, target.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.RING, Vfx.RING);
    }

    static boolean markedBy(ServerPlayer player, LivingEntity target) {
        return target.getPersistentData().hasUUID(MARKED_BY) && player.getUUID().equals(target.getPersistentData().getUUID(MARKED_BY))
                && target.getPersistentData().getLong(MARKED_UNTIL) >= player.level().getGameTime();
    }

    /** The user's marked creatures within {@code range}. */
    static List<LivingEntity> marks(ServerPlayer player, double range) {
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e.isAlive() && markedBy(player, e));
    }

    /** The nearest marked creature, if any. */
    @Nullable
    static LivingEntity nearestMark(ServerPlayer player) {
        LivingEntity best = null;
        for (LivingEntity e : marks(player, 64)) {
            if (best == null || e.distanceToSqr(player) < best.distanceToSqr(player)) {
                best = e;
            }
        }
        return best;
    }

    static AbilityProjectile throwAt(AbilityContext ctx, ResourceLocation kind, @Nullable LivingEntity target, Vec3 dir, float size,
            float damage, int life) {
        AbilityProjectile p = AbilityProjectile.spawn(ctx.player(), kind, ctx.handPos(), dir.scale(1.4), size, STEEL, damage, life);
        if (target != null) {
            p.extra().putUUID(TARGET, target.getUUID());
        }
        return p;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Touch a creature to mark it for a minute: it glows, and everything you throw will find it. */
    public static final class MarkTouch extends Ability {
        MarkTouch() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(40).stamina(6).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Touch a target first"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                mark(player, target);
                Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ITEM_FRAME_ADD_ITEM, 1f, 1.2f);
        }
    }

    /** Throw a knife. If you have a mark, the knife curves through the air to it. */
    public static final class MarkedKnife extends Ability {
        MarkedKnife() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, STEEL).cooldown(30).stamina(6).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            throwAt(ctx, KNIFE, nearestMark(ctx.player()), ctx.player().getLookAngle(), 0.3f, 8f * ctx.power(), 60);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 0.8f, 1.6f);
        }
    }

    /** Fan out five knives; every one of them turns toward your mark. */
    public static final class KnifeVolley extends Ability {
        KnifeVolley() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, STEEL).cooldown(80).stamina(12).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            LivingEntity target = nearestMark(player);
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.3)).add(0, 0.15, 0).normalize();
                throwAt(ctx, KNIFE, target, dir, 0.3f, 5f * ctx.power(), 70);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 1f, 1.3f);
        }
    }

    /** Heave an anchor high into the air; it comes down on your mark wherever it hides. */
    public static final class AnchorDrop extends Ability {
        AnchorDrop() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, STEEL).cooldown(200).stamina(18).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return nearestMark(player) != null ? Optional.empty() : Optional.of(Component.literal("Mark a target first"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            LivingEntity target = nearestMark(ctx.player());
            if (target == null) {
                return;
            }
            AbilityProjectile anchor = throwAt(ctx, ANCHOR, target, new Vec3(0, 1, 0), 1.2f, 14f * ctx.power(), 100);
            anchor.withPower(ctx.power());
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ANVIL_PLACE, 0.8f, 0.6f);
        }
    }

    /** Hold to keep throwing knives at your mark as fast as your hands can move. */
    public static final class HomingBarrage extends Ability {
        HomingBarrage() {
            super(props(ActivationType.HOLD, AbilityIcon.BLADE, COLOR).cooldown(160).stamina(12).staminaPerTick(0.35f)
                    .maxActive(50).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_GATLING);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                ServerPlayer player = ctx.player();
                Vec3 dir = player.getLookAngle().add(ctx.right().scale((player.getRandom().nextDouble() - 0.5) * 0.6)).normalize();
                throwAt(ctx, KNIFE, nearestMark(player), dir, 0.3f, 4f * ctx.power(), 60);
                ctx.sound(SoundEvents.TRIDENT_THROW.value(), 0.5f, 1.8f);
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

    /** Mark every foe around you at once. */
    public static final class MarkAll extends Ability {
        MarkAll() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(500).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 16 * ctx.power())) {
                mark(player, target);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 16f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ITEM_FRAME_ADD_ITEM, 1.6f, 0.8f);
        }
    }

    /** Hurl something enormous into the sky; a piece of it falls on every creature you have marked. */
    public static final class ShipDrop extends Ability {
        ShipDrop() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(700).stamina(34).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return marks(player, 48).isEmpty() ? Optional.of(Component.literal("Mark a target first")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : marks(player, 48)) {
                AbilityProjectile drop = AbilityProjectile.spawn(player, ANCHOR, target.position().add(0, 14, 0), new Vec3(0, -1.2, 0),
                        2.5f, 0x6A4A2A, 18f * ctx.power(), 60);
                drop.extra().putUUID(TARGET, target.getUUID());
                drop.withPower(ctx.power() * 1.5f);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ANVIL_LAND, 1.6f, 0.5f);
        }
    }
}
