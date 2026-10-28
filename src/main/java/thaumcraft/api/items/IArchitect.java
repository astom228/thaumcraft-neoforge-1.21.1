package thaumcraft.api.items;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Предмет, подсвечивающий область блоков (порт IArchitect): фокус «План», стихийная лопата и т.п. */
public interface IArchitect {
    HitResult getArchitectMOP(ItemStack stack, Level world, LivingEntity player);

    boolean useBlockHighlight(ItemStack stack);

    ArrayList<BlockPos> getArchitectBlocks(ItemStack stack, Level world, BlockPos pos, Direction side, Player player);

    boolean showAxis(ItemStack stack, Level world, Player player, Direction side, EnumAxis axis);

    enum EnumAxis {
        X,
        Y,
        Z
    }
}
