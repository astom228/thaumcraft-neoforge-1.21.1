package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraftInventory;

/** Пьедестал (порт TilePedestal): один предмет, виден клиентам. События блока 11/12 — облачка у предмета. */
public class TilePedestal extends TileThaumcraftInventory {

    public TilePedestal(BlockPos pos, BlockState state) {
        super(ConfigBlocks.PEDESTAL_TILE.get(), pos, state, 1);
        this.syncedSlots = new int[] {0};
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.isEmpty() || this.getStackInSlot(slot).isEmpty();
    }

    /** Результат наполнения кладётся на пьедестал и сразу рассылается клиентам. */
    public void setInventorySlotContentsFromInfusion(int slot, ItemStack stack) {
        this.setInventorySlotContents(slot, stack);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.syncTile(false);
        }
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == 11) {
            if (this.level != null && this.level.isClientSide) {
                BlockPos p = this.worldPosition.above();
                IFXDispatcher.get().drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0.75F, 0.0F, 0.5F, true, true, null);
            }
            return true;
        }
        if (i == 12) {
            if (this.level != null && this.level.isClientSide) {
                BlockPos p = this.worldPosition.above();
                IFXDispatcher.get().drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, true, true, null);
            }
            return true;
        }
        return super.triggerEvent(i, j);
    }
}
