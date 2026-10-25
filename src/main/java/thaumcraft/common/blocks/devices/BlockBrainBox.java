package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.crafting.BlockThaumatorium;

/**
 * Мнемоническая матрица (порт BlockBrainBox): ставится только на боковую грань тауматориума (не на лицевую) и смотрит на него;
 * без тауматориума выпадает. Каждая даёт тауматориуму 2 рецепта.
 */
public class BlockBrainBox extends DirectionalBlock {
    public static final MapCodec<BlockBrainBox> CODEC = simpleCodec(BlockBrainBox::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 3.0, 3.0, 13.0, 13.0, 13.0);

    public BlockBrainBox(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState st = this.defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
        return st.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? st : null;
    }

    /** canPlaceBlockOnSide: за матрицей — тауматориум, и это не его лицевая сторона. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction toThaum = state.getValue(FACING);
        BlockState t = level.getBlockState(pos.relative(toThaum));
        return t.is(BlocksTC.thaumatorium.get()) && t.getValue(BlockThaumatorium.FACING) != toThaum.getOpposite();
    }

    /** neighborChanged: без тауматориума — выпасть (как dropBlockAsItem + setBlockToAir). */
    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir == state.getValue(FACING) && !level.getBlockState(pos.relative(dir)).is(BlocksTC.thaumatorium.get()) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
