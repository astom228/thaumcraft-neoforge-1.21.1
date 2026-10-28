package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCMenus;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

/**
 * Меню фокусного манипулятора (порт ContainerFocalManipulator): слот фокуса и инвентарь игрока сбоку (колонками).
 * Кнопка 0 — запуск (всё проверяет сервер).
 */
public class ContainerFocalManipulator extends AbstractContainerMenu {
    private final TileFocalManipulator table;

    public ContainerFocalManipulator(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (TileFocalManipulator) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ContainerFocalManipulator(int id, Inventory inventoryPlayer, TileFocalManipulator tileEntity) {
        super(TCMenus.FOCAL_MANIPULATOR.get(), id);
        this.table = tileEntity;
        this.addSlot(new SlotItemHandler(tileEntity.getItemHandler(), 0, 31, 191));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, i * 18 - 62, 64 + j * 18));
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(inventoryPlayer, i + j * 3, i * 18 - 62, j * 18 + 7));
            }
        }
    }

    public TileFocalManipulator getTile() {
        return this.table;
    }

    @Override
    public boolean clickMenuButton(Player p, int button) {
        if (button == 0 && !this.table.startCraft(button, p)) {
            this.table.getLevel().playSound(p, this.table.getBlockPos(), SoundsTC.craftfail.get(), SoundSource.BLOCKS, 0.33F, 1.0F);
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.table.isUsableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int par2) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(par2);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (par2 != 0) {
                if (itemstack1.getItem() instanceof ItemFocus) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (par2 < 28) {
                    if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (par2 < 37 && !this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
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
