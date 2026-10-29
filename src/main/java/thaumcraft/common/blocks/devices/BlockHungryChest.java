package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.devices.TileHungryChest;

/**
 * Голодный сундук (порт BlockHungryChest): одиночный сундук (не соединяется в двойной), «съедает» упавшие на него предметы
 * (со звуком), сигнал компаратору — по заполнению. Рисуется рендером с моделью сундука 1.12 и своей текстурой.
 */
public class BlockHungryChest extends ChestBlock {
    public BlockHungryChest(Properties props) {
        super(props, ConfigBlocks.HUNGRY_CHEST_TILE::get);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(TYPE, ChestType.SINGLE)
                .setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(TYPE, ChestType.SINGLE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileHungryChest(pos, state);
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (world.isClientSide || !(entity instanceof ItemEntity item) || !item.isAlive() || !(world.getBlockEntity(pos) instanceof TileHungryChest chest)) {
            return;
        }
        ItemStack stack = item.getItem();
        ItemStack leftovers = InventoryUtils.insertStackAt(world, pos, Direction.UP, stack.copy(), false);
        if (leftovers.isEmpty() || leftovers.getCount() != stack.getCount()) {
            entity.playSound(SoundEvents.GENERIC_EAT, 0.25F, (world.random.nextFloat() - world.random.nextFloat()) * 0.2F + 1.0F);
        }
        if (!leftovers.isEmpty()) {
            item.setItem(leftovers);
        } else {
            item.discard();
        }
        chest.setChanged();
    }
}
