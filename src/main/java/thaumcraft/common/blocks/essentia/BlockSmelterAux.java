package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import com.mojang.serialization.MapCodec;

/**
 * Вспомогательный насос плавильни (порт BlockSmelterAux): ставится только на боковую (не лицевую) грань плавильни и смотрит
 * на неё; алембики над насосом тоже получают эссенцию плавильни.
 */
public class BlockSmelterAux extends HorizontalDirectionalBlock {
    public static final MapCodec<BlockSmelterAux> CODEC = simpleCodec(BlockSmelterAux::new);

    public BlockSmelterAux(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** canPlaceBlockOnSide: боковая грань плавильни, не её лицо; FACING — к плавильне. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        if (!canPlaceOn(ctx.getLevel(), ctx.getClickedPos(), side)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, side.getOpposite());
    }

    static boolean canPlaceOn(Level world, BlockPos pos, Direction side) {
        if (!side.getAxis().isHorizontal()) {
            return false;
        }
        BlockState smelter = world.getBlockState(pos.relative(side.getOpposite()));
        return smelter.getBlock() instanceof BlockSmelter && smelter.getValue(BlockSmelter.FACING) != side;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
