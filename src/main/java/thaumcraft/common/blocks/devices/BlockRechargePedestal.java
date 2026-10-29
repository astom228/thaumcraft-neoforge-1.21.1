package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
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
import thaumcraft.api.items.IRechargable;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.devices.TileRechargePedestal;

/**
 * Пьедестал подзарядки (порт BlockRechargePedestal): заряжаемый предмет в руке кладётся на пустой пьедестал (один);
 * щелчок по занятому — предмет выпадает к игроку.
 */
public class BlockRechargePedestal extends BaseEntityBlock {
    public static final MapCodec<BlockRechargePedestal> CODEC = simpleCodec(BlockRechargePedestal::new);

    public BlockRechargePedestal(Properties props) {
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
        return new TileRechargePedestal(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ConfigBlocks.RECHARGE_PEDESTAL_TILE.get(), TileRechargePedestal::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (world.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof TileRechargePedestal ped) {
            if (ped.getStackInSlot(0).isEmpty() && stack.getItem() instanceof IRechargable) {
                ped.setInventorySlotContents(0, stack.copyWithCount(1));
                stack.shrink(1);
                player.getInventory().setChanged();
                world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
                        ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 1.6F);
                return ItemInteractionResult.SUCCESS;
            }
            if (!ped.getStackInSlot(0).isEmpty()) {
                dropAtPlayer(world, pos, player, ped);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof TileRechargePedestal ped && !ped.getStackInSlot(0).isEmpty()) {
            dropAtPlayer(world, pos, player, ped);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** InventoryUtils.dropItemsAtEntity: предмет появляется у игрока. */
    private static void dropAtPlayer(Level world, BlockPos pos, Player player, TileRechargePedestal ped) {
        ItemStack out = ped.decrStackSize(0, 64);
        if (!out.isEmpty()) {
            world.addFreshEntity(new ItemEntity(world, player.getX(), player.getY() + player.getEyeHeight() / 2.0F, player.getZ(), out));
        }
        world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
                ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 1.5F);
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof TileRechargePedestal ped) {
            ped.dropContents();
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }
}
