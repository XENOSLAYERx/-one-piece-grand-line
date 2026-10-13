package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.props;

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
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wara Wara no Mi. A body of straw, and straw effigies that take your wounds for you: bind a foe to a doll and half of
 * every hit you suffer lands on them instead. The tarot decides the rest.
 */
public class StrawFruit extends DevilFruit {
    public static final int COLOR = 0xE8D07A;
    public static final int BROWN = 0x8A6A2A;
    static final String M = "fruit/grandline:straw";
    public static final ResourceLocation SCARECROW = GrandLine.id("straw_scarecrow");
    static final Map<UUID, Effigy> EFFIGIES = new HashMap<>();
    /** Set while a redirected wound is being dealt, so two effigies bound to each other can't bounce it forever. */
    static boolean redirecting;

    public static final DeferredHolder<Ability, StrawNeedles> STRAW_NEEDLES = ability("straw_needles", StrawNeedles::new);
    public static final DeferredHolder<Ability, StrawEffigy> STRAW_EFFIGY = ability("straw_effigy", StrawEffigy::new);
    public static final DeferredHolder<Ability, StrawMan> STRAW_MAN = ability("straw_man", StrawMan::new);
    public static final DeferredHolder<Ability, StrawSword> STRAW_SWORD = ability("straw_sword", StrawSword::new);
    public static final DeferredHolder<Ability, Tarot> TAROT = ability("straw_tarot", Tarot::new);
    public static final DeferredHolder<Ability, ScarecrowArmy> SCARECROW_ARMY = ability("straw_scarecrow_army", ScarecrowArmy::new);
    public static final DeferredHolder<Ability, StrawDemon> STRAW_DEMON = ability("straw_demon", StrawDemon::new);

    public static final ResourceLocation NEEDLE = GrandLine.id("straw_needle");

    record Effigy(UUID target, long until) {}

    public StrawFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Wara Wara no Mi", COLOR)
                .abilities(STRAW_NEEDLES, STRAW_EFFIGY, STRAW_MAN, STRAW_SWORD, TAROT)
                .awakened(SCARECROW_ARMY, STRAW_DEMON));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(NEEDLE, Projectiles.piercing(AbilityDamage.SLASH, null));
    }

    @Override
    public void onLost(ServerPlayer player) {
        EFFIGIES.remove(player.getUUID());
        Minions.dismiss(player, SCARECROW);
    }

    /** While an effigy is bound, half of every wound the user takes is passed on to the foe tied to it. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        Effigy effigy = EFFIGIES.get(user.getUUID());
        if (effigy == null || redirecting) {
            return;
        }
        if (effigy.until() < user.level().getGameTime()) {
            EFFIGIES.remove(user.getUUID());
            return;
        }
        Entity bound = user.serverLevel().getEntity(effigy.target());
        if (!(bound instanceof LivingEntity victim) || !victim.isAlive() || victim.distanceToSqr(user) > 48 * 48) {
            return;
        }
        float half = event.getAmount() * 0.5f;
        event.setAmount(event.getAmount() - half);
        redirecting = true;
        try {
            Kit.hit(user, victim, AbilityDamage.DRAIN, half);
        } finally {
            redirecting = false;
        }
        Vfx.impact(user.serverLevel(), victim, victim.getBoundingBox().getCenter(), BROWN, 1f, Kind.SHARD);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Shoot a spray of hardened straw needles that run through everything in their path.
     *
     * @name Straw Needles
     */
    public static final class StrawNeedles extends Ability {
        StrawNeedles() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.07)).normalize();
                AbilityProjectile.spawn(player, NEEDLE, ctx.handPos(), dir.scale(2.0), 0.3f, COLOR, 6f * ctx.power(), 20);
            }
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.GRASS_BREAK, 1.2f, 1.4f);
        }
    }

    /**
     * Bind a foe to a straw effigy for thirty seconds: half of every wound you take lands on them instead.
     *
     * @name Straw Effigy
     */
    public static final class StrawEffigy extends Ability {
        StrawEffigy() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, BROWN).cooldown(600).stamina(14).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                EFFIGIES.put(player.getUUID(), new Effigy(target.getUUID(), ctx.level().getGameTime() + 600));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), BROWN, 0.1f, Kind.SPARK,
                        Vfx.BEAM_GLOW);
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.EVOKER_PREPARE_ATTACK, 1f, 1.2f);
        }
    }

    /**
     * Swell into a straw man: bigger, tougher and harder-hitting.
     *
     * @name Straw Man
     */
    public static final class StrawMan extends Buffs.Toggle {
        StrawMan() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(600).mastery(M, 10).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("straw_man_scale"), 0.4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("straw_man_armor"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("straw_man_damage"), 3, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 2f, Kind.LEAF, 0);
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(SoundEvents.GRASS_PLACE, 1.6f, 0.6f);
        }
    }

    /**
     * Draw a sword of woven straw from your arm and cut in a wide arc.
     *
     * @name Straw Sword
     */
    public static final class StrawSword extends Ability {
        StrawSword() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, BROWN).cooldown(50).stamina(9).mastery(M, 20).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5 * ctx.power(), 55)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 11f * ctx.power());
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.8)), player.getLookAngle(), COLOR, 2.2f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1f);
        }
    }

    /**
     * Draw a tarot card and take what it says: the Sun (strength and speed), Temperance (healing), the Tower (an
     * explosion that hits every foe around you) or Death (every foe nearby withers).
     */
    public static final class Tarot extends Ability {
        Tarot() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(300).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int card = ctx.level().random.nextInt(4);
            String name;
            switch (card) {
                case 0 -> {
                    name = "The Sun";
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
                    Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), 0xFFD040, 2f, Kind.GLOW, Vfx.FLASH);
                }
                case 1 -> {
                    name = "Temperance";
                    player.heal(8f);
                    Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), 0x8AE08A, 2f, Kind.HEART, 0);
                }
                case 2 -> {
                    name = "The Tower";
                    for (LivingEntity target : AbilityDamage.around(player, player.position(), 5 * ctx.power())) {
                        Kit.hit(player, target, AbilityDamage.BLAST, 10f * ctx.power());
                        Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.1).add(0, 0.5, 0));
                    }
                    Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), 0xFF8A3A, 5f, Kind.SMOKE, Vfx.HEAVY | Vfx.FLASH);
                }
                default -> {
                    name = "Death";
                    for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                    }
                    Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), 0x3A3A3A, 8f, Kind.SMOKE, Vfx.RING);
                }
            }
            player.displayClientMessage(Component.literal(name), true);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.BOOK_PAGE_TURN, 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Raise four straw scarecrows with bows that fight for you for twenty seconds. */
    public static final class ScarecrowArmy extends Ability {
        ScarecrowArmy() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, BROWN).cooldown(900).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Minions.dismiss(player, SCARECROW);
            for (Vec3 at : Kit.circle(player.position(), 2.4, 4, 0)) {
                Minions.spawn(player, SCARECROW, EntityType.STRAY, at, 400, mob -> {
                    mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
                    mob.setDropChance(EquipmentSlot.MAINHAND, 0f);
                    mob.setDropChance(EquipmentSlot.HEAD, 0f);
                });
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.GRASS_PLACE, 2f, 0.5f);
        }
    }

    /**
     * Become a demon of straw: huge, armoured and savage.
     *
     * @name Straw Demon
     */
    public static final class StrawDemon extends Buffs.Toggle {
        StrawDemon() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SKULL, COLOR).cooldown(900).stamina(30).staminaPerTick(0.3f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("straw_demon_scale"), 1.2, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("straw_demon_armor"), 10, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("straw_demon_damage"), 7, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("straw_demon_kb"), 0.8, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), COLOR, 4f, Kind.LEAF, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1f, 1.4f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 12 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), BROWN, 1.6f, Kind.LEAF);
            }
            return true;
        }
    }
}
