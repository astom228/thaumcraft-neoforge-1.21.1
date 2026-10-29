package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.devices.TileVisGenerator;

/**
 * Вис-генератор (порт BlockVisGenerator): при установке выход сам поворачивается к соседнему приёмнику энергии (если есть).
 * Пока в генераторе есть энергия — искры у выхода. Форма — столбик 0.5×0.875, прижатый к стороне выхода.
 */
public class BlockVisGenerator extends BlockTCDeviceFacing {
    public static final MapCodec<BlockVisGenerator> CODEC = simpleCodec(BlockVisGenerator::new);

    public BlockVisGenerator(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileVisGenerator(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ConfigBlocks.VIS_GENERATOR_TILE.get(), TileVisGenerator::tick);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        for (Direction face : Direction.values()) {
            IEnergyStorage cap = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(face), face.getOpposite());
            if (cap != null && cap.canReceive()) {
                return this.defaultBlockState().setValue(FACING, face).setValue(ENABLED, true);
            }
        }
        return super.getStateForPlacement(ctx);
    }

    /** Utils.rotateBlockAABB((0.25, 0, 0.25)–(0.75, 0.875, 0.75), facing): столбик у стороны выхода. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction f = state.getValue(FACING);
        double[] min = {0.25, 0.25, 0.25};
        double[] max = {0.75, 0.75, 0.75};
        int axis = f.getAxis().ordinal();
        if (f.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            min[axis] = 0.125;
            max[axis] = 1.0;
        } else {
            min[axis] = 0.0;
            max[axis] = 0.875;
        }
        return Shapes.box(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        Direction face = state.getValue(FACING);
        if (world.getBlockEntity(pos) instanceof TileVisGenerator tile && tile.getEnergyStored() > 0) {
            double x = face.getStepX() == 0 ? rand.nextGaussian() * 0.1 : face.getStepX() * 0.1;
            double y = face.getStepY() == 0 ? rand.nextGaussian() * 0.1 : face.getStepY() * 0.1;
            double z = face.getStepZ() == 0 ? rand.nextGaussian() * 0.1 : face.getStepZ() * 0.1;
            IFXDispatcher.get().spark(pos.getX() + 0.5 + x, pos.getY() + 0.5 + y, pos.getZ() + 0.5 + z, 0.66F + rand.nextFloat(),
                    0.65F + rand.nextFloat() * 0.1F, 1.0F, 1.0F, 0.8F);
        }
    }
}
