package com.nerodev.grandline.weapon;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.combat.CombatManager;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.style.StyleManager;
import com.nerodev.grandline.transform.Transformation;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The rules of weapons and Seastone:
 * <ul>
 *   <li>A weapon's special property acts on every blow it lands.</li>
 *   <li>Black blades take Haki best: an Armament-coated blow gains more through a weapon with Haki affinity.</li>
 *   <li>Seastone weapons, bullets and nets strike a Logia's real body.</li>
 *   <li>A Devil Fruit user holding Seastone, or standing against Seastone blocks or bars, is sealed and weakened.</li>
 *   <li>A sealed fruit stops working at once: its running powers end and a Zoan changes back.</li>
 * </ul>
 */
public final class WeaponManager {
    /** Anything made of Seastone: a fruit user holding it is sealed. */
    public static final TagKey<Item> SEASTONE_ITEMS = TagKey.create(Registries.ITEM, GrandLine.id("seastone"));
    /** Melee weapons of Seastone: their blows strike a Logia. */
    public static final TagKey<Item> SEASTONE_WEAPONS = TagKey.create(Registries.ITEM, GrandLine.id("seastone_weapons"));
    /** Seastone blocks: a fruit user against them is sealed (cells, walls, floors). */
    public static final TagKey<Block> SEASTONE_BLOCKS = TagKey.create(Registries.BLOCK, GrandLine.id("seastone"));

    /** How long one moment of Seastone contact keeps a fruit sealed; refreshed every second while it lasts. */
    public static final int CONTACT_SEAL_TICKS = 40;

    private WeaponManager() {}

    public static void register() {
        LogiaFruit.TOUCH.add(WeaponManager::seastoneTouches);
        // After CombatManager has marked which blow was swung.
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingIncomingDamageEvent.class, WeaponManager::onIncomingDamage);
        // After Haki has coated the blow.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingIncomingDamageEvent.class, WeaponManager::onHakiAffinity);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, WeaponManager::onDamaged);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, WeaponManager::onPlayerTick);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Weapons
    // ---------------------------------------------------------------------------------------------------------

    /** The weapon behind a melee blow (struck in person, weapon in the main hand), or null. */
    @Nullable
    public static WeaponItem weaponOf(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))
                && attacker.getMainHandItem().getItem() instanceof WeaponItem weapon) {
            return weapon;
        }
        return null;
    }

    /** True for the blow a player actually swung (mobs only ever swing at one target). */
    private static boolean swing(LivingEntity attacker, LivingEntity target) {
        return !(attacker instanceof ServerPlayer player) || CombatManager.isSwing(player, target);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        WeaponItem weapon = weaponOf(event.getSource());
        LivingEntity target = event.getEntity();
        if (weapon != null && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != target) {
            weapon.special().onHit(attacker, target, event, swing(attacker, target));
        }
    }

    private static void onDamaged(LivingDamageEvent.Post event) {
        WeaponItem weapon = weaponOf(event.getSource());
        LivingEntity target = event.getEntity();
        if (weapon != null && event.getNewDamage() > 0 && event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != target) {
            weapon.special().afterHit(attacker, target, event.getNewDamage(), swing(attacker, target));
        }
    }

    /**
     * Armament through a weapon: the Haki share of the blow is scaled by the weapon's affinity. With x1.3 affinity a
     * x1.4 Armament blow becomes x1.52.
     */
    private static void onHakiAffinity(LivingIncomingDamageEvent event) {
        WeaponItem weapon = weaponOf(event.getSource());
        if (weapon == null || weapon.hakiAffinity() == 1f || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !HakiManager.armamentActive(attacker)) {
            return;
        }
        float coat = HakiManager.armamentMultiplier(attacker);
        event.setAmount(event.getAmount() * (1f + (coat - 1f) * weapon.hakiAffinity()) / coat);
    }

    /** Guns reload this many ticks after a shot (a Gunslinger reloads faster). */
    public static int reloadTicks(ServerPlayer shooter, int base) {
        return Math.max(4, Math.round(base * StyleManager.style(shooter).reloadMultiplier()));
    }

    /** The damage of a bullet fired by {@code shooter} from a gun that deals {@code base}. */
    public static float gunDamage(ServerPlayer shooter, float base) {
        return base * StyleManager.style(shooter).gunDamageMultiplier();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Seastone
    // ---------------------------------------------------------------------------------------------------------

    /** Seastone strikes a Logia's real body: bullets, nets and blows from a Seastone weapon. */
    public static boolean seastoneTouches(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof AbilityProjectile shot) {
            ResourceLocation kind = shot.kind();
            return GLWeapons.SEASTONE_BULLET_SHOT.equals(kind) || GLWeapons.NET_THROWN.equals(kind)
                    || shot.extra().getBoolean(GunItem.SEASTONE_TAG);
        }
        return source.getEntity() instanceof LivingEntity attacker && direct == attacker && source.is(AbilityDamage.PHYSICAL)
                && attacker.getMainHandItem().is(SEASTONE_WEAPONS);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !FruitManager.hasFruit(player)) {
            return;
        }
        if (player.tickCount % 20 == 0 && !player.isCreative() && !player.isSpectator() && touchingSeastone(player)) {
            sap(player);
        }
        if (player.tickCount % 5 == 0 && player.hasEffect(GLMobEffects.FRUIT_SEALED)) {
            endFruitPowers(player);
        }
    }

    /** Holding Seastone, or standing in or right against a Seastone block (walls, bars and the floor). */
    public static boolean touchingSeastone(Player player) {
        if (player.getMainHandItem().is(SEASTONE_ITEMS) || player.getOffhandItem().is(SEASTONE_ITEMS)) {
            return true;
        }
        AABB box = player.getBoundingBox().inflate(0.75, 0, 0.75).expandTowards(0, -0.5, 0);
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            if (player.level().getBlockState(pos).is(SEASTONE_BLOCKS)) {
                return true;
            }
        }
        return false;
    }

    /** One second of Seastone contact: the fruit is sealed and the body goes weak. */
    public static void sap(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(GLMobEffects.FRUIT_SEALED, CONTACT_SEAL_TICKS, 0, true, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CONTACT_SEAL_TICKS, 1, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CONTACT_SEAL_TICKS, 0, true, false, true));
        player.displayClientMessage(Component.literal("Seastone drains your strength").withStyle(ChatFormatting.DARK_AQUA), true);
    }

    /** A sealed fruit stops working: running fruit powers end and a fruit transformation (a Zoan form) reverts. */
    public static void endFruitPowers(ServerPlayer player) {
        AbilityState state = AbilityState.of(player);
        if (state.hasActive()) {
            for (ResourceLocation id : List.copyOf(state.activeAbilities())) {
                Ability ability = GLAbilities.REGISTRY.get(id);
                if (ability != null && ability.properties().category() == AbilityCategory.FRUIT) {
                    AbilityManager.end(player, ability, Ability.EndReason.INTERRUPTED);
                }
            }
        }
        Transformation form = TransformationManager.active(player);
        if (form != null && form.sourceAbility() != null && form.sourceAbility().properties().category() == AbilityCategory.FRUIT) {
            TransformationManager.end(player);
        }
    }
}
