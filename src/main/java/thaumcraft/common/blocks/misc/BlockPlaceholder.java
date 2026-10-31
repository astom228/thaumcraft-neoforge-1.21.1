package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.crafting.BlockGolemBuilder;
import thaumcraft.common.blocks.devices.BlockInfernalFurnace;

/**
 * Заполнитель многоблока (порт BlockPlaceholder): невидимый блок на месте частей адской печи (кирпич, обсидиан) и пресса
 * големов (решётка, наковальня, котёл, стол). При сломе роняет исходный блок (таблица добычи) и разбирает свой многоблок;
 * ПКМ по части пресса открывает окно пресса.
 */
public class BlockPlaceholder extends Block {
    public BlockPlaceholder(Properties props) {
        super(props);
    }

    private static boolean isFurnacePart(BlockState state) {
        return state.is(BlocksTC.placeholderNetherbrick.get()) || state.is(BlocksTC.placeholderObsidian.get());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isFurnacePart(state)) {
            BlockPos press = findPress(world, pos);
            if (press != null) {
                BlockGolemBuilder.openGui(world, press, player);
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    private static BlockPos findPress(Level world, BlockPos pos) {
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                for (int c = -1; c <= 1; c++) {
                    if (world.getBlockState(pos.offset(a, b, c)).is(BlocksTC.golemBuilder.get())) {
                        return pos.offset(a, b, c);
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (state.is(newState.getBlock()) || level.isClientSide) {
            return;
        }
        if (isFurnacePart(state)) {
            if (BlockInfernalFurnace.isIgnoring()) {
                return;
            }
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
        } else if (!BlockGolemBuilder.isIgnoring()) {
            BlockPos press = findPress(level, pos);
            if (press != null) {
                BlockGolemBuilder.destroy(level, press, pos);
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
