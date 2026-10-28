package thaumcraft.common.config;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.EntitySpecialItem;

/** Типы сущностей (порт ConfigEntities; пополняется по этапам, основная часть — этап 16). */
public class ConfigEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Thaumcraft.MODID);

    // EntityRegistry.registerModEntity(…, "SpecialItem", …, 64, 20, true)
    public static final Supplier<EntityType<EntitySpecialItem>> SPECIAL_ITEM = ENTITIES.register("special_item",
            () -> EntityType.Builder.<EntitySpecialItem>of(EntitySpecialItem::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(20).build("special_item"));

    // EntityRegistry.registerModEntity(…, "FollowItem", …, 64, 20, false)
    public static final Supplier<EntityType<thaumcraft.common.entities.EntityFollowingItem>> FOLLOWING_ITEM = ENTITIES.register("following_item",
            () -> EntityType.Builder.<thaumcraft.common.entities.EntityFollowingItem>of(thaumcraft.common.entities.EntityFollowingItem::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(20).build("following_item"));

    // EntityRegistry.registerModEntity(…, "FocusProjectile", …, 64, 20, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityFocusProjectile>> FOCUS_PROJECTILE = ENTITIES.register("focus_projectile",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityFocusProjectile>of(thaumcraft.common.entities.projectile.EntityFocusProjectile::new,
                    MobCategory.MISC).sized(0.15F, 0.15F).clientTrackingRange(4).updateInterval(20).build("focus_projectile"));

    // EntityRegistry.registerModEntity(…, "FocusCloud", …, 64, 20, true); размер — по радиусу (getDimensions)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityFocusCloud>> FOCUS_CLOUD = ENTITIES.register("focus_cloud",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityFocusCloud>of(thaumcraft.common.entities.projectile.EntityFocusCloud::new,
                    MobCategory.MISC).sized(0.15F, 0.15F).clientTrackingRange(4).updateInterval(20).build("focus_cloud"));

    // EntityRegistry.registerModEntity(…, "Focusmine", …, 64, 20, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityFocusMine>> FOCUS_MINE = ENTITIES.register("focus_mine",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityFocusMine>of(thaumcraft.common.entities.projectile.EntityFocusMine::new,
                    MobCategory.MISC).sized(0.15F, 0.15F).clientTrackingRange(4).updateInterval(20).build("focus_mine"));

    // EntityRegistry.registerModEntity(…, "Spellbat", …, 64, 3, false, …)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntitySpellBat>> SPELL_BAT = ENTITIES.register("spellbat",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntitySpellBat>of(thaumcraft.common.entities.monster.EntitySpellBat::new,
                    MobCategory.MONSTER).sized(0.5F, 0.9F).clientTrackingRange(4).updateInterval(3).build("spellbat"));

    /** Атрибуты живых сущностей (applyEntityAttributes оригинала). */
    public static void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(SPELL_BAT.get(), thaumcraft.common.entities.monster.EntitySpellBat.createAttributes().build());
    }
}
