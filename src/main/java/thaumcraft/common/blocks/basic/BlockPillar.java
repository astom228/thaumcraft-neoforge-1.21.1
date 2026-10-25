package thaumcraft.common.blocks.basic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.blocks.BlocksTC;

/**
 * Колонна наполнения (порт BlockPillar): магическая, древняя, жуткая. Рамка выделения — нижняя половина, столкновение —
 * два блока в высоту; при снятии роняет 2 камня своего вида (сама не выпадает). Ставится лицом от игрока.
 */
public class BlockPillar extends HorizontalDirectionalBlock {
    public static final MapCodec<BlockPillar> CODEC = simpleCodec(BlockPillar::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final VoxelShape COLLISION = Block.box(0.0, 0.0, 0.0, 16.0, 32.0, 16.0);

    public BlockPillar(Properties properties) {
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

    /** onBlockPlacedBy: сторона по повороту игрока, развёрнутая к нему. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !world.isClientSide) {
            Block stone = state.is(BlocksTC.pillarAncient.get()) ? BlocksTC.stoneAncient.get()
                    : state.is(BlocksTC.pillarEldritch.get()) ? BlocksTC.stoneEldritchTile.get() : BlocksTC.stoneArcane.get();
            popResource(world, pos, new ItemStack(stone, 2));
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }
}
