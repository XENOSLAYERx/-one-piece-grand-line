package com.nerodev.grandline.weapon;

import com.nerodev.grandline.GrandLine;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * A melee weapon: a sword, polearm or club with a grade, its own damage, speed and reach, a Haki affinity (how much
 * an Armament-coated blow gains through it; black blades take Haki best) and an optional special property that acts
 * on every blow it lands.
 */
public class WeaponItem extends SwordItem {
    /** What a weapon does on each blow it lands, beyond its damage. */
    public interface Special {
        Special NONE = new Special() {};

        /**
         * Shapes a blow's damage before it lands.
         *
         * @param swing true for the blow the wielder actually swung (not sweep damage to bystanders)
         */
        default void onHit(LivingEntity attacker, LivingEntity target, LivingIncomingDamageEvent event, boolean swing) {}

        /** After a blow has landed and dealt {@code dealt} damage. */
        default void afterHit(LivingEntity attacker, LivingEntity target, float dealt, boolean swing) {}
    }

    private final WeaponKind kind;
    private final WeaponGrade grade;
    private final float hakiAffinity;
    private final Special special;

    /** Builder-style description of a weapon. */
    public record Spec(Tier tier, WeaponKind kind, WeaponGrade grade, float damage, float speed, float reach, float hakiAffinity,
            Special special, Rarity rarity, boolean unbreakable) {
        public static Spec of(Tier tier, WeaponKind kind, WeaponGrade grade, float damage, float speed) {
            return new Spec(tier, kind, grade, damage, speed, 0f, 1f, Special.NONE, Rarity.COMMON, false);
        }

        public Spec reach(float value) {
            return new Spec(tier, kind, grade, damage, speed, value, hakiAffinity, special, rarity, unbreakable);
        }

        public Spec haki(float affinity) {
            return new Spec(tier, kind, grade, damage, speed, reach, affinity, special, rarity, unbreakable);
        }

        public Spec special(Special value) {
            return new Spec(tier, kind, grade, damage, speed, reach, hakiAffinity, value, rarity, unbreakable);
        }

        /** A named blade: its rarity, and it never breaks. */
        public Spec legendary(Rarity value) {
            return new Spec(tier, kind, grade, damage, speed, reach, hakiAffinity, special, value, true);
        }
    }

    public WeaponItem(Spec spec) {
        super(spec.tier(), properties(spec));
        this.kind = spec.kind();
        this.grade = spec.grade();
        this.hakiAffinity = spec.hakiAffinity();
        this.special = spec.special();
    }

    private static Properties properties(Spec spec) {
        ItemAttributeModifiers.Builder attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, spec.damage() - 1f, Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, spec.speed() - 4f, Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
        if (spec.reach() != 0f) {
            attributes.add(Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(GrandLine.id("weapon_reach"), spec.reach(), Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        Properties properties = new Properties().attributes(attributes.build()).rarity(spec.rarity());
        if (spec.unbreakable()) {
            properties = properties.component(DataComponents.UNBREAKABLE, new Unbreakable(true));
        }
        return properties;
    }

    public WeaponKind kind() {
        return kind;
    }

    public WeaponGrade grade() {
        return grade;
    }

    public float hakiAffinity() {
        return hakiAffinity;
    }

    public Special special() {
        return special;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(kind.label() + " - ").withStyle(ChatFormatting.GRAY).append(grade.display()));
        if (special != Special.NONE) {
            tooltip.add(Component.translatable(getDescriptionId() + ".special").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (hakiAffinity > 1f) {
            tooltip.add(Component.literal(String.format("Haki affinity +%.0f%%", (hakiAffinity - 1f) * 100))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
