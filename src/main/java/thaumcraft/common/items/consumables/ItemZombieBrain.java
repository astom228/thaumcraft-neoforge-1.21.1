package thaumcraft.common.items.consumables;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Мозг зомби: еда (4, 0.2), голод с шансом 0.8. После поедания — Искажение (оригинал: onFoodEaten). */
public class ItemZombieBrain extends Item {
    public ItemZombieBrain(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // TODO этап 3: if (!level.isClientSide) { 10% — +1 постоянного Искажения, иначе +1..3 временного } (addWarpToPlayer)
        return super.finishUsingItem(stack, level, entity);
    }
}
