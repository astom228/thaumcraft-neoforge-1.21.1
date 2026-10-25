package thaumcraft.api.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Блок (или его BlockEntity), на который можно наклеить ярлык. true — ярлык использован. */
public interface ILabelable {
    boolean applyLabel(Player player, BlockPos pos, Direction side, ItemStack labelstack);
}
