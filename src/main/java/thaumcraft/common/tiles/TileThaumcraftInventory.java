package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * BlockEntity с инвентарём (порт TileThaumcraftInventory). Раздел 7: инвентарь — IItemHandler (возможность блока со всех
 * сторон, как getSlotsForFace оригинала): вставка — только допустимых предметов (isItemValidForSlot), извлечение — любое.
 * syncedSlots — слоты, содержимое которых видно клиенту (уходят с данными синхронизации).
 * Отложенная синхронизация через 50 тиков (update оригинала) не нужна: данные синхронизации приходят клиенту вместе с чанком.
 */
public class TileThaumcraftInventory extends TileThaumcraft {
    protected int[] syncedSlots = new int[0];
    protected final ItemStackHandler inventory;

    public TileThaumcraftInventory(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.inventory = new ItemStackHandler(size) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return TileThaumcraftInventory.this.isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return TileThaumcraftInventory.this.getInventoryStackLimit();
            }

            @Override
            protected void onContentsChanged(int slot) {
                TileThaumcraftInventory.this.setChanged();
                if (TileThaumcraftInventory.this.isSyncedSlot(slot)) {
                    TileThaumcraftInventory.this.syncTile(false);
                }
            }
        };
    }

    public IItemHandler getItemHandler() {
        return this.inventory;
    }

    public int getSizeInventory() {
        return this.inventory.getSlots();
    }

    public ItemStack getStackInSlot(int index) {
        return this.inventory.getStackInSlot(index);
    }

    public void setInventorySlotContents(int index, ItemStack stack) {
        this.inventory.setStackInSlot(index, stack);
    }

    public ItemStack decrStackSize(int index, int count) {
        return this.inventory.extractItem(index, count, false);
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    /** getInventoryStackLimit оригинала. */
    public int getInventoryStackLimit() {
        return 64;
    }

    public boolean isUsableByPlayer(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    private boolean isSyncedSlot(int slot) {
        for (int s : this.syncedSlots) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    /** Выбросить содержимое в мир (InventoryUtils.dropItems при разрушении блока). */
    public void dropContents() {
        if (this.level == null) {
            return;
        }
        NonNullList<ItemStack> list = NonNullList.withSize(this.inventory.getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            list.set(i, this.inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, list);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        if (!nbt.contains("Items")) {
            for (int i = 0; i < this.inventory.getSlots(); i++) {
                if (this.isSyncedSlot(i)) {
                    this.inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        ListTag list = nbt.getList("ItemsSynced", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            int slot = t.getByte("Slot");
            if (this.isSyncedSlot(slot) && slot < this.inventory.getSlots()) {
                this.inventory.setStackInSlot(slot, ItemStack.parseOptional(registries, t.getCompound("Item")));
            }
        }
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            ItemStack s = this.inventory.getStackInSlot(i);
            if (!s.isEmpty() && this.isSyncedSlot(i)) {
                CompoundTag t = new CompoundTag();
                t.putByte("Slot", (byte) i);
                t.put("Item", s.save(registries));
                list.add(t);
            }
        }
        nbt.put("ItemsSynced", list);
        return nbt;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        if (nbt.contains("Items")) {
            this.inventory.deserializeNBT(registries, nbt.getCompound("Items"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.put("Items", this.inventory.serializeNBT(registries));
    }
}
