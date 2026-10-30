package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;

/**
 * Нарост порчи (порт BlockTaintFeature): светящийся шар на грани блока (FACING — от опоры). Распространяет волокна;
 * на вертикальном заражённом бревне изредка становится гейзером; без опоры исчезает; вдали от семени — флюкс-слизь.
 * При разрушении — Выброс в ауру (ползун порчи — этап 16).
 */
public class BlockTaintFeature extends Block implements ITaintBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public BlockTaintFeature(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    public void die(Level world, BlockPos pos, BlockState state) {
        world.setBlockAndUpdate(pos, BlocksTC.fluxGoo.get().defaultBlockState());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!TaintHelper.isNearTaintSeed(world, pos) && random.nextInt(10) == 0) {
            this.die(world, pos, state);
            return;
        }
        TaintHelper.spreadFibres(world, pos);
        BlockState below = world.getBlockState(pos.below());
        if (below.getBlock() == BlocksTC.taintLog.get() && below.getValue(BlockTaintLog.AXIS) == Direction.Axis.Y && world.random.nextInt(100) == 0) {
            world.setBlockAndUpdate(pos, BlocksTC.taintGeyser.get().defaultBlockState());
        }
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        if (!world.isClientSide && !state.is(newState.getBlock())) {
            // TODO этап 16: с шансом 1/3 — ползун порчи (EntityTaintCrawler) вместо Выброса
            AuraHelper.polluteAura(world, pos, 1.0F, true);
        }
        super.onRemove(state, world, pos, newState, moving);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
        Direction f = state.getValue(FACING);
        BlockPos support = pos.relative(f.getOpposite());
        if (!world.isClientSide && !world.getBlockState(support).isFaceSturdy(world, support, f)) {
            world.removeBlock(pos, false);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case DOWN -> Shapes.box(0.125, 0.625, 0.125, 0.875, 1.0, 0.875);
            case UP -> Shapes.box(0.125, 0.0, 0.125, 0.875, 0.375, 0.875);
            case NORTH -> Shapes.box(0.125, 0.125, 0.625, 0.875, 0.875, 1.0);
            case SOUTH -> Shapes.box(0.125, 0.125, 0.0, 0.875, 0.875, 0.375);
            case WEST -> Shapes.box(0.625, 0.125, 0.125, 1.0, 0.875, 0.875);
            case EAST -> Shapes.box(0.0, 0.125, 0.125, 0.375, 0.875, 0.875);
        };
    }
}
