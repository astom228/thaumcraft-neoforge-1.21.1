package thaumcraft.api.casters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** BlockEntity, реагирующий на ПКМ кастером (рукавицей). Кастеры — этап 12. */
public interface IInteractWithCaster {
    boolean onCasterRightClick(Level world, ItemStack casterStack, Player player, BlockPos pos, Direction side, InteractionHand hand);
}
