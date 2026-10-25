package thaumcraft.common.blocks.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.common.tiles.crafting.TileThaumatorium;
import thaumcraft.common.tiles.crafting.TileThaumatoriumTop;

/**
 * Тауматориум (порт BlockThaumatorium): низ и верх из двух алхимических конструкций над тиглем (соль мира). Без тигля снизу
 * или при снятии любой части обе части снова становятся алхимическими конструкциями; выпадает конструкция.
 */
public class BlockThaumatorium extends BaseEntityBlock {
    public static final MapCodec<BlockThaumatorium> CODEC = simpleCodec(p -> new BlockThaumatorium(false, p));
    public static final MapCodec<BlockThaumatorium> CODEC_TOP = simpleCodec(p -> new BlockThaumatorium(true, p));
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    final boolean top;

    public BlockThaumatorium(boolean top, Properties properties) {
        super(properties);
        this.top = top;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return this.top ? CODEC_TOP : CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.top ? new TileThaumatoriumTop(pos, state) : new TileThaumatorium(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return this.top ? null : createTickerHelper(type, ConfigBlocks.THAUMATORIUM_TILE.get(), TileThaumatorium::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return this.top ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide && !player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
            BlockPos p = this.top ? pos.below() : pos;
            if (world.getBlockEntity(p) instanceof TileThaumatorium tile) {
                sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ContainerThaumatorium(id, inv, tile),
                        net.minecraft.network.chat.Component.translatable("block.thaumcraft.thaumatorium")), buf -> buf.writeBlockPos(p));
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(BlocksTC.metalAlchemical.get());
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!this.top && world.getBlockEntity(pos) instanceof TileThaumatorium tile) {
                tile.dropContents();
            }
            super.onRemove(state, world, pos, newState, isMoving);
            if (this.top && world.getBlockState(pos.below()).is(BlocksTC.thaumatorium.get())) {
                world.setBlock(pos.below(), BlocksTC.metalAlchemical.get().defaultBlockState(), 3);
            }
            if (!this.top && world.getBlockState(pos.above()).is(BlocksTC.thaumatoriumTop.get())) {
                world.setBlock(pos.above(), BlocksTC.metalAlchemical.get().defaultBlockState(), 3);
            }
            return;
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    /** Без тигля снизу тауматориум распадается. */
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!this.top && !world.isClientSide && !world.getBlockState(pos.below()).is(BlocksTC.crucible.get())) {
            world.setBlock(pos, BlocksTC.metalAlchemical.get().defaultBlockState(), 3);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return !this.top;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof TileThaumatorium tile ? ItemHandlerHelper.calcRedstoneFromInventory(tile.getItemHandler()) : 0;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

}
