package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.devices.TileStabilizer;

/** Стабилизатор (порт BlockStabilizer): форма — Utils.rotateBlockAABB((0.125, 0, 0.125)–(0.875, 1, 0.875), facing). */
public class BlockStabilizer extends BlockTCDeviceFacing {
    public static final MapCodec<BlockStabilizer> CODEC = simpleCodec(BlockStabilizer::new);

    public BlockStabilizer(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileStabilizer(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ConfigBlocks.STABILIZER_TILE.get(), TileStabilizer::tick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction.Axis axis = state.getValue(FACING).getAxis();
        double[] min = {0.125, 0.125, 0.125};
        double[] max = {0.875, 0.875, 0.875};
        min[axis.ordinal()] = 0.0;
        max[axis.ordinal()] = 1.0;
        return Shapes.box(min[0], min[1], min[2], max[0], max[1], max[2]);
    }
}
