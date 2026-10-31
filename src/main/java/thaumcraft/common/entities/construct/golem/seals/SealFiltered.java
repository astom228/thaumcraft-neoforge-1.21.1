package thaumcraft.common.entities.construct.golem.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.common.lib.utils.InventoryUtils;

/** Печать с фильтром предметов (порт SealFiltered): образцы по 1 шт., ограничения количества, чёрный/белый список. */
public abstract class SealFiltered implements ISeal, ISealGui, ISealConfigFilter {
    NonNullList<ItemStack> filter = NonNullList.withSize(this.getFilterSize(), ItemStack.EMPTY);
    NonNullList<Integer> filterSize = NonNullList.withSize(this.getFilterSize(), 0);
    boolean blacklist = true;

    @Override
    public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.filter = NonNullList.withSize(this.getFilterSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, this.filter, registries);
        for (ItemStack s : this.filter) {
            if (s.getCount() > 1) {
                s.setCount(1);
            }
        }
        this.blacklist = nbt.getBoolean("bl");
        this.filterSize = NonNullList.withSize(this.getFilterSize(), 0);
        ListTag list = nbt.getList("Sizes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            int j = t.getByte("Slot") & 255;
            if (j < this.filterSize.size()) {
                this.filterSize.set(j, t.getInt("Size"));
            }
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(nbt, this.filter, registries);
        nbt.putBoolean("bl", this.blacklist);
        ListTag list = new ListTag();
        for (int i = 0; i < this.filterSize.size(); i++) {
            int size = this.filterSize.get(i);
            if (size != 0) {
                CompoundTag t = new CompoundTag();
                t.putByte("Slot", (byte) i);
                t.putInt("Size", size);
                list.add(t);
            }
        }
        nbt.put("Sizes", list);
    }

    /** Окно открывает колокольчик (SealBaseContainer.open). */
    @Override
    public Object returnContainer(Level world, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        return null;
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] {0};
    }

    @Override
    public int getFilterSize() {
        return 1;
    }

    @Override
    public NonNullList<ItemStack> getInv() {
        return this.filter;
    }

    public NonNullList<Integer> getSizes() {
        return this.filterSize;
    }

    @Override
    public ItemStack getFilterSlot(int i) {
        return this.filter.get(i);
    }

    @Override
    public int getFilterSlotSize(int i) {
        return this.filterSize.get(i);
    }

    @Override
    public void setFilterSlot(int i, ItemStack stack) {
        this.filter.set(i, stack.copy());
    }

    @Override
    public void setFilterSlotSize(int i, int size) {
        this.filterSize.set(i, size);
    }

    @Override
    public boolean isBlacklist() {
        return this.blacklist;
    }

    @Override
    public void setBlacklist(boolean black) {
        this.blacklist = black;
    }

    @Override
    public boolean hasStacksizeLimiters() {
        return false;
    }

    /** Фильтр по переключателям «метаданные, NBT, словарь руд, мод» (первые четыре у предметных печатей). */
    static InventoryUtils.InvFilter invFilter(thaumcraft.api.golems.seals.ISealConfigToggles.SealToggle[] props) {
        return new InventoryUtils.InvFilter(!props[0].getValue(), !props[1].getValue(), props[2].getValue(), props[3].getValue());
    }
}
