package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.forms;
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
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Disguise;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Inu Inu no Mi, Model: Tanuki. Become a tanuki, the shapeshifter of folklore: take the shape of a harmless animal so
 * monsters ignore you, drum your belly, fling leaves sharp as blades, and, awakened, swell to the size of a hill.
 */
public class TanukiFruit extends ZoanFruit {
    public static final int COLOR = 0x8A6A4A;
    public static final int LEAF = 0x5AA84A;
    static final String M = "fruit/grandline:tanuki";
    public static final ResourceLocation DECOY = GrandLine.id("tanuki_decoy");

    /**
     * @hybrid Tanuki-Man | Grow round ears and a bushy tail: quicker and a little tougher.
     * @beast Tanuki Form | Become a tanuki: small and quick on your feet.
     */
    static final ZoanForms FORMS = forms("tanuki", COLOR, () -> SoundEvents.FOX_AMBIENT, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ARMOR, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.3, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(0.7f, 0.8f, 0.6f));

    public static final DeferredHolder<Ability, BellyDrum> BELLY_DRUM = ability("tanuki_belly_drum", BellyDrum::new);
    public static final DeferredHolder<Ability, Shapeshift> SHAPESHIFT = ability("tanuki_shapeshift", Shapeshift::new);
    public static final DeferredHolder<Ability, TailSlam> TAIL_SLAM = ability("tanuki_tail_slam", TailSlam::new);
    public static final DeferredHolder<Ability, BellyBounce> BELLY_BOUNCE = ability("tanuki_belly_bounce", BellyBounce::new);
    public static final DeferredHolder<Ability, LeafShuriken> LEAF_SHURIKEN = ability("tanuki_leaf_shuriken", LeafShuriken::new);
    public static final DeferredHolder<Ability, MountainShape> MOUNTAIN_SHAPE = ability("tanuki_mountain_shape", MountainShape::new);
    public static final DeferredHolder<Ability, PhantomParade> PHANTOM_PARADE = ability("tanuki_phantom_parade", PhantomParade::new);

    public static final ResourceLocation LEAF_KIND = GrandLine.id("tanuki_leaf");

    public TanukiFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Inu Inu no Mi, Model: Tanuki", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), BELLY_DRUM, SHAPESHIFT, TAIL_SLAM, BELLY_BOUNCE, LEAF_SHURIKEN)
                .awakened(MOUNTAIN_SHAPE, PHANTOM_PARADE), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(LEAF_KIND, Projectiles.piercing(AbilityDamage.SLASH, null));
    }

    @Override
    public void onLost(ServerPlayer player) {
        super.onLost(player);
        Disguise.clear(player);
        Minions.dismiss(player, DECOY);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Drum on your belly: the boom knocks back and dizzies everything around you. */
    public static final class BellyDrum extends Ability {
        BellyDrum() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(80).stamina(9).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * FORMS.boost(player) * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.2, 0.35);
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            }
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 5f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2f, 0.6f);
        }
    }

    /**
     * Shapeshift into a harmless pig: monsters lose interest in you while the shape holds, until you strike one.
     */
    public static final class Shapeshift extends Ability {
        Shapeshift() {
            super(props(ActivationType.TOGGLE, AbilityIcon.LEAF, LEAF).cooldown(300).stamina(10).staminaPerTick(0.05f).maxActive(1200)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Disguise.as(ctx.player(), EntityType.PIG);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), LEAF, 1.6f, Kind.LEAF, Vfx.FLASH);
            ctx.sound(SoundEvents.FOX_AMBIENT, 1f, 1.4f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 10 == 0) {
                for (Mob hunter : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16),
                        m -> m.getTarget() == player && m.getLastHurtByMob() != player)) {
                    hunter.setTarget(null);
                }
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Disguise.clear(ctx.player());
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), LEAF, 1.4f, Kind.LEAF, 0);
        }
    }

    /** Swing your thick tail around into whatever is beside you, bouncing it away. Needs a form. */
    public static final class TailSlam extends Ability {
        TailSlam() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, COLOR).cooldown(40).stamina(8).mastery(M, 10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 3.5)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.0).add(0, 0.6, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.6, 0), COLOR, 3.5f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.BOUNCE.get(), 1f, 0.8f);
        }
    }

    /** Bounce on your round belly: you spring high, and foes beside you are tossed into the air. */
    public static final class BellyBounce extends Ability {
        BellyBounce() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(100).stamina(9).mastery(M, 20).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 3.5)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 5f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.9, 0));
            }
            Kit.launch(player, ctx.horizontalLook().scale(0.4).add(0, 1.3, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0));
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 2f, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(GLSounds.BOUNCE.get(), 1.2f, 1f);
        }
    }

    /** Flick a fan of leaves as sharp as blades: they cut through everything in their path. */
    public static final class LeafShuriken extends Ability {
        LeafShuriken() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, LEAF).cooldown(60).stamina(10).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.1)).normalize();
                AbilityProjectile.spawn(player, LEAF_KIND, ctx.handPos(), dir.scale(1.8), 0.4f, LEAF, 5f * ctx.power(), 22);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.AZALEA_LEAVES_BREAK, 1.4f, 1.4f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Swell to the size of a hill: enormous, armoured and immovable, though slow. */
    public static final class MountainShape extends Buffs.Toggle {
        MountainShape() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(900).stamina(28).staminaPerTick(0.3f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("tanuki_mountain_scale"), 3, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("tanuki_mountain_armor"), 12, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("tanuki_mountain_damage"), 8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("tanuki_mountain_kb"), 1, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("tanuki_mountain_speed"), -0.35,
                                    Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), LEAF, 5f, Kind.LEAF, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(GLSounds.INFLATE.get(), 1.6f, 0.4f);
        }
    }

    /** Conjure a parade of phantom foxes from leaves: four of them harry your foes for fifteen seconds. */
    public static final class PhantomParade extends Ability {
        PhantomParade() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, LEAF).cooldown(900).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Minions.dismiss(player, DECOY);
            for (Vec3 at : Kit.circle(player.position(), 2.5, 4, 0)) {
                Minions.spawn(player, DECOY, EntityType.FOX, at, 300, fox -> {
                    fox.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 2));
                    fox.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
                });
                Vfx.burst(ctx.level(), player, at.add(0, 0.5, 0), LEAF, 1f, Kind.LEAF, 0);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.FOX_SCREECH, 1.2f, 1f);
        }
    }
}
