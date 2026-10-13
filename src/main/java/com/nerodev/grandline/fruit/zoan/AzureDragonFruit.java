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
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fruit.logia.RumbleFruit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 * Uo Uo no Mi, Model: Seiryu. Become an azure dragon, as Kaido does: a sky serpent that rides on clouds of flame,
 * breathes blasts of fire, whips up tornadoes and wind blades, and in its hybrid body strikes like the thunder.
 */
public class AzureDragonFruit extends ZoanFruit {
    public static final int COLOR = 0x3A6AC8;
    public static final int FIRE = 0xF08A2A;
    public static final int WIND = 0xD8E8F0;
    public static final int THUNDER = 0xF0E08A;
    static final String M = "fruit/grandline:azure_dragon";

    /**
     * @hybrid Dragon-Man | Grow a dragon's horns and tail and a giant's frame: enormously strong and very hard to hurt.
     * @beast Azure Dragon Form | Become a vast azure dragon: it flies, breathes fire and shrugs off almost anything.
     */
    static final ZoanForms FORMS = bodies("azure_dragon", COLOR, () -> SoundEvents.ENDER_DRAGON_GROWL, ZoanForms.Traits.FLYER,
            b -> b.modifier(Attributes.SCALE, 0.6, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 16, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.8, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 30, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.3, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 60, Operation.ADD_VALUE)
                    .dimensions(3.0f, 3.0f, 2.2f));

    public static final DeferredHolder<Ability, BoloBreath> BOLO_BREATH = ability("azure_dragon_bolo_breath", BoloBreath::new);
    public static final DeferredHolder<Ability, Kaifu> KAIFU = ability("azure_dragon_kaifu", Kaifu::new);
    public static final DeferredHolder<Ability, FlameClouds> FLAME_CLOUDS = ability("azure_dragon_flame_clouds", FlameClouds::new);
    public static final DeferredHolder<Ability, Tatsumaki> TATSUMAKI = ability("azure_dragon_tatsumaki", Tatsumaki::new);
    public static final DeferredHolder<Ability, ThunderBagua> THUNDER_BAGUA = ability("azure_dragon_thunder_bagua", ThunderBagua::new);
    public static final DeferredHolder<Ability, GreatBoloBreath> GREAT_BOLO_BREATH = ability("azure_dragon_great_bolo_breath",
            GreatBoloBreath::new);
    public static final DeferredHolder<Ability, Ragnaraku> RAGNARAKU = ability("azure_dragon_ragnaraku", Ragnaraku::new);

    public static final ResourceLocation BOLO = GrandLine.id("azure_dragon_bolo");
    public static final ResourceLocation BLADE = GrandLine.id("azure_dragon_kaifu");
    public static final ResourceLocation TWISTER = GrandLine.id("azure_dragon_tatsumaki");

    public AzureDragonFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Uo Uo no Mi, Model: Seiryu", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), BOLO_BREATH, KAIFU, FLAME_CLOUDS, TATSUMAKI, THUNDER_BAGUA)
                .awakened(GREAT_BOLO_BREATH, RAGNARAKU), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BOLO, Projectiles.blast(3.5, AbilityDamage.FIRE, (p, target) -> target.igniteForSeconds(5),
                p -> Vfx.burst((ServerLevel) p.level(), p, p.position(), FIRE, 3.5f, Kind.FLAME, Vfx.RING | Vfx.HEAVY)));
        ProjectileBehavior.Registry.register(BLADE, Projectiles.piercing(AbilityDamage.SLASH, (p, target) ->
                Kit.launch(target, p.getDeltaMovement().normalize().scale(0.8).add(0, 0.3, 0))));
        ProjectileBehavior.Registry.register(TWISTER, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.SLASH, p.damage());
                Kit.pull(target, p.position().add(0, 3, 0), 0.5);
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Breathe a ball of fire that bursts on whatever it hits and sets everything around it ablaze. */
    public static final class BoloBreath extends Ability {
        BoloBreath() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, FIRE).cooldown(60).stamina(12).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            AbilityProjectile shot = AbilityProjectile.spawn(ctx.player(), BOLO, ctx.player().getEyePosition(),
                    ctx.player().getLookAngle().scale(1.6), 1.2f, FIRE, 14f * FORMS.boost(ctx.player()) * ctx.power(), 40);
            shot.withPower(ctx.power());
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.FIRE_GUN.get(), 1.4f, 0.6f);
        }
    }

    /** Lash the air into three blades of wind that cut through everything in their path. */
    public static final class Kaifu extends Ability {
        Kaifu() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, WIND).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = look.yRot(i * 0.25f);
                AbilityProjectile.spawn(player, BLADE, ctx.handPos(), dir.scale(1.8), 1.4f, WIND, 9f * FORMS.boost(player) * ctx.power(), 25)
                        .withPower(ctx.power());
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.BREEZE_WHIRL, 1.4f, 0.7f);
        }
    }

    /**
     * Call up clouds of flame beneath you and ride them through the sky for half a minute.
     *
     * @name Kaen Kumo
     */
    public static final class FlameClouds extends Ability {
        FlameClouds() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, FIRE).cooldown(400).stamina(14).staminaPerTick(0.1f).maxActive(600)
                    .mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), FIRE, 2.4f, Kind.SMOKE, Vfx.RING);
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 1f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.resetFallDistance();
            if (ctx.activeTicks() % 3 == 0 && !player.onGround()) {
                Vfx.impact(ctx.level(), player, player.position(), 0xC8C0B8, 1.6f, Kind.SMOKE);
                Vfx.impact(ctx.level(), player, player.position(), FIRE, 1.0f, Kind.FLAME);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            if (!FORMS.beast(ctx.player())) {
                Kit.setFlight(ctx.player(), false);
            }
        }
    }

    /** Raise a tornado where you point that slices and lifts everything inside for six seconds. */
    public static final class Tatsumaki extends Ability {
        Tatsumaki() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, WIND).cooldown(300).stamina(18).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, TWISTER, Kit.ground(ctx.level(), Kit.aimPoint(player, 28), 10), 7 * ctx.power(), WIND,
                    5f * ctx.power(), 120);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BREEZE_WHIRL, 2f, 0.4f);
        }
    }

    /** Close on the foe you face and strike with the weight of thunder. It is knocked senseless. */
    public static final class ThunderBagua extends Ability {
        ThunderBagua() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, THUNDER).cooldown(240).stamina(20).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No one to strike"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 to = target.position().subtract(ctx.horizontalLook().scale(1.5));
                player.teleportTo(to.x, target.getY(), to.z);
                Kit.hit(player, target, AbilityDamage.STRIKE, 24f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(2.2).add(0, 0.5, 0));
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 40, 0, false, false, true));
                RumbleFruit.bolt(ctx.level(), target.position());
                Vfx.trail(ctx.level(), player, from.add(0, 1, 0), to.add(0, 1, 0), THUNDER, 1.2f, Kind.SPARK);
                Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), THUNDER, 3f, Kind.SPARK, Vfx.RING | Vfx.FLASH | Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.6f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A Bolo Breath at full heat: a torrent of fire thirty blocks long that burns everything along it. */
    public static final class GreatBoloBreath extends Ability {
        GreatBoloBreath() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, FIRE).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double length = 30 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 2.5)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 24f * ctx.power());
                target.igniteForSeconds(8);
            }
            Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(length));
            Vfx.beam(ctx.level(), player, player.getEyePosition(), end, FIRE, 2.6f, Kind.FLAME, Vfx.BEAM_GLOW);
            Vfx.beam(ctx.level(), player, player.getEyePosition(), end, 0xF0D04A, 1.4f, Kind.FLAME, Vfx.BEAM_PARTICLES);
            Vfx.burst(ctx.level(), player, end, FIRE, 5f, Kind.FLAME, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 2f, 0.5f);
        }
    }

    /** Bring a blow down on the ground so hard that thunder answers it, flattening everything around you. */
    public static final class Ragnaraku extends Ability {
        Ragnaraku() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, THUNDER).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 9 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 22f * ctx.power());
                Kit.hit(player, target, AbilityDamage.SHOCK, 6f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 2.0, 0.6);
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 30, 0, false, false, true));
            }
            for (Vec3 at : Kit.circle(player.position(), radius * 0.7, 6, ctx.activeTicks())) {
                RumbleFruit.bolt(ctx.level(), Kit.ground(ctx.level(), at.add(0, 3, 0), 8));
            }
            Vfx.burst(ctx.level(), player, player.position(), THUNDER, (float) radius, Kind.SPARK, Vfx.RING | Vfx.GROUND | Vfx.HEAVY | Vfx.FLASH);
            Vfx.burst(ctx.level(), player, player.position(), 0x8A7A5A, (float) radius * 0.6f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.RUMBLE_THUNDER.get(), 2f, 0.7f);
        }
    }
}
