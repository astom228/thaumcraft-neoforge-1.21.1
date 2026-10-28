package thaumcraft.api.casters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Обработчик щелчка перчаткой по зарегистрированному состоянию блока (порт ICasterTriggerManager). */
public interface ICasterTriggerManager {
    boolean performTrigger(Level world, ItemStack casterStack, Player player, BlockPos pos, Direction side, int event);
}
