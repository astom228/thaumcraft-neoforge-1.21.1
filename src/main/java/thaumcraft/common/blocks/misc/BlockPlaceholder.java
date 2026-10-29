package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.devices.BlockInfernalFurnace;

/**
 * Заполнитель многоблока (порт BlockPlaceholder): невидимый блок на месте кирпича / обсидиана адской печи; при сломе роняет
 * исходный блок (таблица добычи) и разбирает печь. Заполнители пресса големов — этап 18.
 */
public class BlockPlaceholder extends Block {
    public BlockPlaceholder(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (state.is(newState.getBlock()) || level.isClientSide || BlockInfernalFurnace.isIgnoring()) {
            return;
        }
        if (state.is(BlocksTC.placeholderNetherbrick.get()) || state.is(BlocksTC.placeholderObsidian.get())) {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    for (int c = -1; c <= 1; c++) {
                        BlockState s = level.getBlockState(pos.offset(a, b, c));
                        if (s.is(BlocksTC.infernalFurnace.get())) {
                            BlockInfernalFurnace.destroyFurnace(level, pos.offset(a, b, c), s, pos);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
