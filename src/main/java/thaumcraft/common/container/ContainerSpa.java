package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.common.lib.TCMenus;
import thaumcraft.common.tiles.devices.TileSpa;

/**
 * Меню купели (порт ContainerSpa): 0 — соли для ванн, 1–36 — игрок. Кнопка 1 — переключить смешивание (сервер проверяет,
 * что меню открыто и игрок рядом). Бак и режим экран читает из блок-сущности (она синхронизируется).
 */
public class ContainerSpa extends AbstractContainerMenu {
    private final TileSpa spa;

    public ContainerSpa(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (TileSpa) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ContainerSpa(int id, Inventory inv, TileSpa tile) {
        super(TCMenus.SPA.get(), id);
        this.spa = tile;
        this.addSlot(new SlotItemHandler(tile.getItemHandler(), 0, 65, 31));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int var5 = 0; var5 < 9; var5++) {
            this.addSlot(new Slot(inv, var5, 8 + var5 * 18, 142));
        }
    }

    public TileSpa getSpa() {
        return this.spa;
    }

    @Override
    public boolean clickMenuButton(Player p, int button) {
        if (button == 1) {
            this.spa.toggleMix();
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.spa != null && !this.spa.isRemoved() && this.spa.isUsableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slotObject = this.slots.get(slot);
        if (slotObject != null && slotObject.hasItem()) {
            ItemStack stackInSlot = slotObject.getItem();
            stack = stackInSlot.copy();
            if (slot == 0) {
                if (!this.spa.isItemValidForSlot(slot, stackInSlot) || !this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.spa.isItemValidForSlot(slot, stackInSlot) || !this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) {
                slotObject.setByPlayer(ItemStack.EMPTY);
            } else {
                slotObject.setChanged();
            }
        }
        return stack;
    }
}
