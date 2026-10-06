package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.items.IScribeTools;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraftInventory;

/**
 * Стол исследований (порт TileResearchTable): слот 0 — письменные принадлежности, слот 1 — бумага; оба видны клиенту.
 * Теоретизирование (ResearchTableData) — подшаг 7.6б.
 */
public class TileResearchTable extends TileThaumcraftInventory {
    public TileResearchTable(BlockPos pos, BlockState state) {
        super(ConfigBlocks.RESEARCH_TABLE_TILE.get(), pos, state, 2);
        this.syncedSlots = new int[] {0, 1};
    }

    /** Потратить одну единицу чернил (прочность письменных принадлежностей). */
    public boolean consumeInkFromTable() {
        ItemStack tools = this.getStackInSlot(0);
        if (tools.getItem() instanceof IScribeTools && tools.getDamageValue() < tools.getMaxDamage()) {
            ItemStack copy = tools.copy();
            copy.setDamageValue(copy.getDamageValue() + 1);
            this.setInventorySlotContents(0, copy);
            this.syncTile(false);
            this.setChanged();
            return true;
        }
        return false;
    }

    public boolean consumepaperFromTable() {
        if (this.getStackInSlot(1).is(Items.PAPER) && this.getStackInSlot(1).getCount() > 0) {
            this.decrStackSize(1, 1);
            this.syncTile(false);
            this.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return switch (i) {
            case 0 -> itemstack.getItem() instanceof IScribeTools;
            case 1 -> itemstack.is(Items.PAPER);
            default -> false;
        };
    }
}
