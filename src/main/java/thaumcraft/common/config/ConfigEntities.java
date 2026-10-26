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
}
