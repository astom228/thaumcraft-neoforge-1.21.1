package thaumcraft.common.blocks.world.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Брёвна (оригинал: прочность 2.0, сопротивление 5 → 3.0, топор, горючесть 5, огонь 5).
 * Свет серебряного дерева (5) задаётся в свойствах блока. Распад листвы при разрушении в 1.21 работает через расстояние до бревна.
 */
public class BlockLogsTC extends RotatedPillarBlock {
    public BlockLogsTC(Properties properties) {
        super(properties);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }
}
