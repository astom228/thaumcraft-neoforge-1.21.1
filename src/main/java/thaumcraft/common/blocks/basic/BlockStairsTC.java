package thaumcraft.common.blocks.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Ступени. Деревянные: горючесть 20, распространение огня 5 (как в оригинале). */
public class BlockStairsTC extends StairBlock {
    private final boolean wood;

    public BlockStairsTC(BlockState baseState, Properties properties, boolean wood) {
        super(baseState, properties);
        this.wood = wood;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return wood ? 20 : super.getFlammability(state, level, pos, direction);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return wood ? 5 : super.getFireSpreadSpeed(state, level, pos, direction);
    }
}
