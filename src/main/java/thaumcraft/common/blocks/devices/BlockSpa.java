package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.container.ContainerSpa;
import thaumcraft.common.tiles.devices.TileSpa;

/**
 * Магическая купель (порт BlockSpa): ёмкость с жидкостью в руке — залить в бак (только ту же жидкость), иначе — окно купели.
 * Содержимое слота выпадает при разрушении.
 */
public class BlockSpa extends BaseEntityBlock {
    public static final MapCodec<BlockSpa> CODEC = simpleCodec(BlockSpa::new);

    public BlockSpa(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileSpa(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ConfigBlocks.SPA_TILE.get(), TileSpa::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof TileSpa tile)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        FluidStack fs = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (fs.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!world.isClientSide && tile.tank.getFluidAmount() < tile.tank.getCapacity()
                && (tile.tank.getFluid().isEmpty() || FluidStack.isSameFluidSameComponents(tile.tank.getFluid(), fs))
                && FluidUtil.interactWithFluidHandler(player, hand, tile.tank)) {
            world.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33F,
                    1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.3F);
        }
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide && !player.isShiftKeyDown() && player instanceof ServerPlayer sp && world.getBlockEntity(pos) instanceof TileSpa tile) {
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ContainerSpa(id, inv, tile), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof TileSpa tile) {
            tile.dropContents();
        }
        super.onRemove(state, world, pos, newState, moved);
    }
}
