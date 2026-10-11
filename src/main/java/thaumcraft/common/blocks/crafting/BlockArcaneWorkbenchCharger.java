package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Зарядник верстака (порт BlockArcaneWorkbenchCharger): ставится только на магический верстак (на фокусный манипулятор —
 * с этапа 12), без опоры выпадает предметом; правый клик открывает верстак под ним.
 */
public class BlockArcaneWorkbenchCharger extends Block {

    public BlockArcaneWorkbenchCharger(Properties properties) {
        super(properties);
    }

    private static boolean isValidBase(BlockState below) {
        // TODO этап 12: || below.is(BlocksTC.wandWorkbench) (фокусный манипулятор, меню 7)
        return below.is(BlocksTC.arcaneWorkbench.get());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidBase(level.getBlockState(pos.below()));
    }

    /** Как в оригинале: при установке блок под зарядником отправляет данные клиентам. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos().below()) instanceof TileThaumcraft te && !context.getLevel().isClientSide) {
            te.syncTile(true);
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!isValidBase(level.getBlockState(pos.below()))) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockState(pos.below()).is(BlocksTC.arcaneWorkbench.get())) {
            BlockArcaneWorkbench.openGui(level, pos.below(), player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
