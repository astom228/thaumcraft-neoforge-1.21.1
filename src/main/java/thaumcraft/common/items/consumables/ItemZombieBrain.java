package thaumcraft.common.items.consumables;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;

/** Мозг зомби: еда (4, 0.2), голод с шансом 0.8. После поедания — Искажение. */
public class ItemZombieBrain extends Item {
    public ItemZombieBrain(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            // Оригинал (onFoodEaten): 10% — +1 обычного Искажения, иначе +1..3 временного
            if (level.random.nextFloat() < 0.1F) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.NORMAL);
            } else {
                ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1 + level.random.nextInt(3), IPlayerWarp.EnumWarpType.TEMPORARY);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
