package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.devices.TileDioptra;

/**
 * Диоптра (порт BlockDioptra): щелчок переключает режим (ENABLED: Вис / Выброс), редстоун на него не влияет. Компаратор —
 * значение центральной ячейки: floor(r·14) + (r > 0 ? 1 : 0), r = доля от 64.
 */
public class BlockDioptra extends BaseEntityBlock {
    public static final MapCodec<BlockDioptra> CODEC = simpleCodec(BlockDioptra::new);
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public BlockDioptra(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(ENABLED, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENABLED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileDioptra(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ConfigBlocks.DIOPTRA_TILE.get(), TileDioptra::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide) {
            world.setBlock(pos, state.setValue(ENABLED, !state.getValue(ENABLED)), 3);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof TileDioptra tile) {
            float r = tile.grid_amt[84] / 64.0F;
            return Mth.floor(r * 14.0F) + (r > 0.0F ? 1 : 0);
        }
        return 0;
    }
}
