package thaumcraft.common.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ConfigEntities;

/** Дротик голема (порт EntityGolemDart): маленькая стрела 0.2×0.2; подобрать нельзя (стрелок — не игрок). */
public class EntityGolemDart extends AbstractArrow {
    public EntityGolemDart(EntityType<? extends EntityGolemDart> type, Level world) {
        super(type, world);
    }

    public EntityGolemDart(Level world, LivingEntity shooter) {
        super(ConfigEntities.GOLEM_DART.get(), shooter, world, new ItemStack(Items.ARROW), null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }
}
