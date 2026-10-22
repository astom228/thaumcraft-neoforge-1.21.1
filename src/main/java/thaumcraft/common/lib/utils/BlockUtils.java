package thaumcraft.common.lib.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Порт BlockUtils (методы добавляются по мере переноса систем). */
public class BlockUtils {

    /** Есть ли у блока хотя бы одна сторона, не закрытая непрозрачным блоком. */
    public static boolean isBlockExposed(Level world, BlockPos pos) {
        for (Direction face : Direction.values()) {
            BlockPos p = pos.relative(face);
            if (!world.getBlockState(p).isSolidRender(world, p)) {
                return true;
            }
        }
        return false;
    }
}
