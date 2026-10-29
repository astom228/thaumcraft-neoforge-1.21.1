package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.devices.TileLampArcane;
import thaumcraft.common.tiles.devices.TileLampFertility;
import thaumcraft.common.tiles.devices.TileLampGrowth;

/**
 * Лампы (порт BlockLamp): магическая (расставляет в темноте невидимые огоньки), роста (ускоряет растения, тратит Herba)
 * и плодородия (сводит животных, тратит Cupiditas). Крепится к блоку со стороны FACING и падает без него; светит, когда включена.
 */
public class BlockLamp extends BlockTCDeviceFacing {
    public enum Kind {
        ARCANE, GROWTH, FERTILITY
    }

    public static final MapCodec<BlockLamp> CODEC = simpleCodec(p -> new BlockLamp(Kind.ARCANE, p));
    private static final VoxelShape SHAPE = Block.box(4.0, 2.0, 4.0, 12.0, 14.0, 12.0);
    public final Kind kind;

    public BlockLamp(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** Крепление — к блоку, по которому щёлкнули (facing = сторона щелчка, обращённая). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite()).setValue(ENABLED, false);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide) {
            return;
        }
        if (level.isEmptyBlock(pos.relative(state.getValue(FACING)))) {
            level.destroyBlock(pos, true);
            return;
        }
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TileLampArcane arcane && state.getValue(ENABLED) && level.hasNeighborSignal(pos)) {
            arcane.removeLights();
        }
        boolean checkUpdate = !(te instanceof TileLampGrowth g && g.charges <= 0) && !(te instanceof TileLampFertility f && f.charges <= 0);
        if (checkUpdate) {
            super.neighborChanged(state, level, pos, block, from, moving);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TileLampArcane arcane) {
            arcane.removeLights();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (this.kind) {
            case ARCANE -> new TileLampArcane(pos, state);
            case GROWTH -> new TileLampGrowth(pos, state);
            case FERTILITY -> new TileLampFertility(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return switch (this.kind) {
            case ARCANE -> createTickerHelper(type, ConfigBlocks.LAMP_ARCANE_TILE.get(), TileLampArcane::tick);
            case GROWTH -> createTickerHelper(type, ConfigBlocks.LAMP_GROWTH_TILE.get(), TileLampGrowth::tick);
            case FERTILITY -> createTickerHelper(type, ConfigBlocks.LAMP_FERTILITY_TILE.get(), TileLampFertility::tick);
        };
    }
}
