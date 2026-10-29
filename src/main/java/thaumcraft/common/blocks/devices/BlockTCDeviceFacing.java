package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Устройство со сторонами и включением (порт BlockTCDevice с IBlockFacing + IBlockEnabled): FACING — все 6 сторон,
 * ENABLED — по умолчанию «нет сигнала редстоуна» (updateState оригинала: enabled = !isBlockPowered). При установке — к игроку
 * (с Shift — от игрока). Подклассы переопределяют поведение, как в оригинале.
 */
public abstract class BlockTCDeviceFacing extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    protected BlockTCDeviceFacing(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(ENABLED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // EnumFacing.getDirectionFromEntityLiving: сторона к игроку
        Direction d = ctx.getNearestLookingDirection().getOpposite();
        Player p = ctx.getPlayer();
        return this.defaultBlockState().setValue(FACING, p != null && p.isShiftKeyDown() ? d.getOpposite() : d).setValue(ENABLED, true);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        this.updateState(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        this.updateState(level, pos, state);
        super.neighborChanged(state, level, pos, block, from, moving);
    }

    protected void updateState(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        boolean flag = !level.hasNeighborSignal(pos);
        if (flag != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, flag), 3);
        }
    }
}
