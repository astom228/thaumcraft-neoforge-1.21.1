package thaumcraft.common.blocks.world.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Листва (горючесть 60, огонь 30). Дроп (саженец 1/100 и 1/75, капля ртути с серебряного дерева) — в таблицах добычи.
 * TODO этап 5: листва серебряного дерева на случайном тике добавляет 0.01 Вис в ауру, если Вис ниже базы.
 */
public class BlockLeavesTC extends LeavesBlock {
    public BlockLeavesTC(Properties properties) {
        super(properties);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }
}
