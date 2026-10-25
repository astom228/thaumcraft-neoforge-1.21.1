package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.TCMenus;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.essentia.TileSmelter;

/**
 * Меню плавильни (порт ContainerSmelter): 0 — переплавляемое (только с аспектами), 1 — топливо (как обычный слот оригинала —
 * без проверки), 2–37 — игрок. Свойства: время переплавки, горения, горения текущего топлива, эссенция, длительность переплавки.
 */
public class ContainerSmelter extends AbstractContainerMenu {
    private final TileSmelter furnace;
    private final ContainerData data;

    public ContainerSmelter(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (TileSmelter) inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(5));
    }

    public ContainerSmelter(int id, Inventory inv, TileSmelter tile) {
        this(id, inv, tile, tile.data);
    }

    private ContainerSmelter(int id, Inventory par1InventoryPlayer, TileSmelter tileEntity, ContainerData data) {
        super(TCMenus.SMELTER.get(), id);
        this.furnace = tileEntity;
        this.data = data;
        this.addSlot(new SlotItemHandler(tileEntity.getItemHandler(), 0, 80, 8));
        this.addSlot(new SlotItemHandler(tileEntity.getItemHandler(), 1, 80, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            /** Без проверки допустимости (SlotItemHandler меряет вместимость пробной вставкой — для не-топлива дал бы 0). */
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return Math.min(stack.getMaxStackSize(), this.getMaxStackSize());
            }
        });
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(par1InventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int var5 = 0; var5 < 9; var5++) {
            this.addSlot(new Slot(par1InventoryPlayer, var5, 8 + var5 * 18, 142));
        }
        this.addDataSlots(data);
    }

    public int getCookTime() {
        return this.data.get(0);
    }

    public int getBurnTime() {
        return this.data.get(1);
    }

    public int getItemBurnTime() {
        return this.data.get(2);
    }

    public int getVis() {
        return this.data.get(3);
    }

    public int getSmeltTime() {
        return this.data.get(4);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.furnace != null && this.furnace.isUsableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int par2) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(par2);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (par2 != 1 && par2 != 0) {
                AspectList al = ThaumcraftCraftingManager.getObjectTags(itemstack1);
                if (TileSmelter.isItemFuel(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false) && !this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (al != null && al.size() > 0) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (par2 >= 2 && par2 < 29) {
                    if (!this.moveItemStackTo(itemstack1, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (par2 >= 29 && par2 < 38 && !this.moveItemStackTo(itemstack1, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 2, 38, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }
}
