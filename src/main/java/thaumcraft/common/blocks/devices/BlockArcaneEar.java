package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.devices.TileArcaneEar;

/**
 * Магическое ухо и переключающее ухо (порт BlockArcaneEar / BlockArcaneEarToggle): крепится к твёрдой стороне блока, ПКМ —
 * следующая нота (с проигрышем), даёт сигнал 15 во все стороны, пока ENABLED.
 */
public class BlockArcaneEar extends BlockTCDeviceFacing {
    public static final MapCodec<BlockArcaneEar> CODEC = simpleCodec(p -> new BlockArcaneEar(false, p));
    public final boolean toggle;

    public BlockArcaneEar(boolean toggle, Properties props) {
        super(props);
        this.toggle = toggle;
        this.registerDefaultState(this.defaultBlockState().setValue(ENABLED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getClickedFace()).setValue(ENABLED, false);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction f = state.getValue(FACING);
        BlockPos on = pos.relative(f.getOpposite());
        return level.getBlockState(on).isFaceSturdy(level, on, f);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (level.getBlockEntity(pos) instanceof TileArcaneEar tile) {
            tile.updateTone();
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.getBlockEntity(pos) instanceof TileArcaneEar tile) {
            tile.updateTone();
        }
        if (!level.isClientSide && !this.canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof TileArcaneEar tile) {
            tile.changePitch();
            tile.triggerNote(world, pos, true);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(ENABLED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(ENABLED) ? 15 : 0;
    }

    /** Соседям и блоку, к которому прикреплено ухо, — об изменении сигнала. */
    public static void notifyAround(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        level.updateNeighborsAt(pos, st.getBlock());
        if (st.hasProperty(FACING)) {
            level.updateNeighborsAt(pos.relative(st.getValue(FACING).getOpposite()), st.getBlock());
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

    /** eventReceived оригинала: звук инструмента с высотой ноты и частица ноты. */
    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        NoteBlockInstrument[] all = NoteBlockInstrument.values();
        NoteBlockInstrument ins = id >= 0 && id < all.length ? all[id] : NoteBlockInstrument.HARP;
        level.playSound(null, pos, ins.getSoundEvent().value(), SoundSource.BLOCKS, 3.0F, NoteBlock.getPitchFromNote(param));
        level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, param / 24.0, 0.0, 0.0);
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileArcaneEar(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ConfigBlocks.ARCANE_EAR_TILE.get(), TileArcaneEar::tick);
    }
}
