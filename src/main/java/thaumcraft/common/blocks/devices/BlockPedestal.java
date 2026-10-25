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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.tiles.crafting.TilePedestal;

/**
 * Пьедестал (порт BlockPedestal): магический, древний, жуткий. Щелчок предметом по пустому — положить один; по занятому —
 * предмет выпадает к игроку.
 */
public class BlockPedestal extends BaseEntityBlock {
    public static final MapCodec<BlockPedestal> CODEC = simpleCodec(BlockPedestal::new);

    public BlockPedestal(Properties properties) {
        super(properties);
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
        return new TilePedestal(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (world.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof TilePedestal ped) {
            if (ped.getStackInSlot(0).isEmpty() && !stack.isEmpty()) {
                ItemStack i = stack.copyWithCount(1);
                ped.setInventorySlotContents(0, i);
                stack.shrink(1);
                player.getInventory().setChanged();
                world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
                        ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 1.6F);
                return ItemInteractionResult.SUCCESS;
            }
            if (!ped.getStackInSlot(0).isEmpty()) {
                this.dropAtPlayer(world, pos, player, ped);
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
        if (world.getBlockEntity(pos) instanceof TilePedestal ped && !ped.getStackInSlot(0).isEmpty()) {
            this.dropAtPlayer(world, pos, player, ped);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** InventoryUtils.dropItemsAtEntity: предмет появляется у игрока. */
    private void dropAtPlayer(Level world, BlockPos pos, Player player, TilePedestal ped) {
        ItemStack out = ped.decrStackSize(0, 64);
        if (!out.isEmpty()) {
            ItemEntity e = new ItemEntity(world, player.getX(), player.getY() + player.getEyeHeight() / 2.0F, player.getZ(), out);
            world.addFreshEntity(e);
        }
        world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
                ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 1.5F);
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof TilePedestal ped) {
            ped.dropContents();
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }
}
