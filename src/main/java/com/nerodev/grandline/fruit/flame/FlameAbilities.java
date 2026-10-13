package com.nerodev.grandline.fruit.flame;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.entity.ProjectileBehavior;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Abilities and projectile kinds of the Flame-Flame Fruit (Mera Mera no Mi). */
public final class FlameAbilities {
    public static final String MASTERY = "fruit/grandline:flame";
    public static final int COLOR = 0xFF6A1A;

    private static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<Ability, FlameMoves.FireFist> FIRE_FIST = ABILITIES.register("fire_fist", FlameMoves.FireFist::new);
    public static final DeferredHolder<Ability, FlameMoves.FireGun> FIRE_GUN = ABILITIES.register("fire_gun", FlameMoves.FireGun::new);
    public static final DeferredHolder<Ability, FlameMoves.FlameCommandment> FLAME_COMMANDMENT =
            ABILITIES.register("flame_commandment", FlameMoves.FlameCommandment::new);
    public static final DeferredHolder<Ability, FlameMoves.FirePillar> FIRE_PILLAR = ABILITIES.register("fire_pillar", FlameMoves.FirePillar::new);
    public static final DeferredHolder<Ability, FlameMoves.FlameEmperor> FLAME_EMPEROR =
            ABILITIES.register("flame_emperor", FlameMoves.FlameEmperor::new);
    public static final DeferredHolder<Ability, FlameMoves.Fireflies> FIREFLIES = ABILITIES.register("fireflies", FlameMoves.Fireflies::new);
    public static final DeferredHolder<Ability, FlameMoves.Detonate> DETONATE = ABILITIES.register("firefly_detonate", FlameMoves.Detonate::new);
    public static final DeferredHolder<Ability, FlameMoves.FlameField> FLAME_FIELD = ABILITIES.register("flame_field", FlameMoves.FlameField::new);

    // Projectile kinds
    public static final ResourceLocation FIRE_FIST_SHOT = GrandLine.id("fire_fist");
    public static final ResourceLocation FIRE_BULLET = GrandLine.id("fire_bullet");
    public static final ResourceLocation FLAME_WALL = GrandLine.id("flame_wall");
    public static final ResourceLocation PILLAR = GrandLine.id("fire_pillar");
    public static final ResourceLocation ENTEI = GrandLine.id("entei");
    public static final ResourceLocation FIREFLY = GrandLine.id("firefly");

    private FlameAbilities() {}

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        ProjectileBehavior.Registry.register(FIRE_FIST_SHOT, new FlameMoves.FireFistShot());
        ProjectileBehavior.Registry.register(FIRE_BULLET, new FlameMoves.FireBulletShot());
        ProjectileBehavior.Registry.register(FLAME_WALL, new FlameMoves.WallBehavior());
        ProjectileBehavior.Registry.register(PILLAR, new FlameMoves.PillarBehavior());
        ProjectileBehavior.Registry.register(ENTEI, new FlameMoves.EnteiShot());
        ProjectileBehavior.Registry.register(FIREFLY, new FlameMoves.FireflyBehavior());
    }
}
