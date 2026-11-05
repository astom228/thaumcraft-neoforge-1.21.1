package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.tools.ItemHandMirror;
import thaumcraft.common.lib.TCMenus;

/**
 * Окно ручного зеркала (порт ContainerHandMirror + InventoryHandMirror): положенный в слот предмет на сервере сразу уходит
 * в связанное зеркало; не ушедший при закрытии выпадает. Само ручное зеркало брать нельзя.
 */
public class ContainerHandMirror extends AbstractContainerMenu {
    private final Player player;
    private final ItemStack mirror;
    private boolean busy = false;
    public final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ContainerHandMirror.this.slotsChanged(this);
        }
    };

    public ContainerHandMirror(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv);
    }

    public ContainerHandMirror(int id, Inventory iinventory) {
        super(TCMenus.HAND_MIRROR.get(), id);
        this.player = iinventory.player;
        this.mirror = iinventory.getSelected();
        this.addSlot(new Slot(this.input, 0, 80, 24));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(iinventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(iinventory, i, 8 + i * 18, 142));
        }
    }

    /** onCraftMatrixChanged: предмет из слота — в зеркало. */
    @Override
    public void slotsChanged(Container inv) {
        super.slotsChanged(inv);
        ItemStack in = this.input.getItem(0);
        if (!this.busy && !this.player.level().isClientSide && !in.isEmpty()) {
            this.busy = true;
            ItemStack is = in.copy();
            this.input.removeItemNoUpdate(0);
            if (!ItemHandMirror.transport(this.mirror, is, this.player)) {
                this.input.setItem(0, is);
            }
            this.busy = false;
            this.broadcastChanges();
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && !((button >= 0 && button <= 8) || button == 40)) return;
        if (slotId >= 0 && slotId < this.slots.size() && this.getSlot(slotId).getItem().getItem() instanceof ItemHandMirror) {
            return;
        }
        // цифровые клавиши не должны переставлять само зеркало (checkHotbarKeys экрана оригинала)
        if (clickType == ClickType.SWAP && player.getInventory().getItem(button).getItem() instanceof ItemHandMirror) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        if (slot < 0 || slot >= this.slots.size()) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.EMPTY;
        Slot slotObject = this.slots.get(slot);
        if (slotObject != null && slotObject.hasItem() && !(slotObject.getItem().getItem() instanceof ItemHandMirror)) {
            ItemStack stackInSlot = slotObject.getItem();
            stack = stackInSlot.copy();
            if (slot == 0) {
                if (!this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
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

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.input);
        }
    }
}
