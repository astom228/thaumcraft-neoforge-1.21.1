package thaumcraft.common.container;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.casters.ItemFocusPouch;
import thaumcraft.common.lib.TCMenus;

/**
 * Меню сумки для фокусов (порт ContainerFocusPouch): 18 слотов только для фокусов и инвентарь игрока. Сумка — в выбранной
 * ячейке хотбара; эту ячейку трогать нельзя (и цифровой клавишей). Содержимое пишется в сумку при каждом изменении
 * (в оригинале — при закрытии); меню закрывается, если сумки в ячейке больше нет.
 */
public class ContainerFocusPouch extends AbstractContainerMenu {
    private final Player player;
    private final int pouchSlot;
    private final int blockSlot;
    public final SimpleContainer input = new SimpleContainer(ItemFocusPouch.SIZE) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof ItemFocus;
        }
    };
    private boolean loading = false;

    public ContainerFocusPouch(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readVarInt());
    }

    public ContainerFocusPouch(int id, Inventory inventory, int pouchSlot) {
        super(TCMenus.FOCUS_POUCH.get(), id);
        this.player = inventory.player;
        this.pouchSlot = pouchSlot;
        this.blockSlot = pouchSlot + 45;
        for (int a = 0; a < ItemFocusPouch.SIZE; a++) {
            this.addSlot(new Slot(this.input, a, 37 + a % 6 * 18, 51 + a / 6 * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ItemFocus;
                }
            });
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 151 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 209));
        }
        if (!this.player.level().isClientSide) {
            ItemStack pouch = this.pouch();
            if (pouch.getItem() instanceof ItemFocusPouch p) {
                this.loading = true;
                NonNullList<ItemStack> list = p.getInventory(pouch);
                for (int a = 0; a < list.size(); a++) {
                    this.input.setItem(a, list.get(a));
                }
                this.loading = false;
            }
        }
        this.input.addListener(this::saveToPouch);
    }

    private ItemStack pouch() {
        return this.player.getInventory().getItem(this.pouchSlot);
    }

    private void saveToPouch(Container c) {
        if (this.loading || this.player.level().isClientSide) {
            return;
        }
        ItemStack pouch = this.pouch();
        if (pouch.getItem() instanceof ItemFocusPouch p) {
            NonNullList<ItemStack> list = NonNullList.withSize(ItemFocusPouch.SIZE, ItemStack.EMPTY);
            for (int a = 0; a < list.size(); a++) {
                list.set(a, this.input.getItem(a));
            }
            p.setInventory(pouch, list);
        }
    }

    public int getPouchSlot() {
        return this.pouchSlot;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player p) {
        if (slotId == this.blockSlot || clickType == ClickType.SWAP && button == this.pouchSlot) {
            return;
        }
        super.clicked(slotId, button, clickType, p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        if (slot == this.blockSlot) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ItemStack.EMPTY;
        Slot slotObject = this.slots.get(slot);
        if (slotObject != null && slotObject.hasItem()) {
            ItemStack stackInSlot = slotObject.getItem();
            stack = stackInSlot.copy();
            if (!(stackInSlot.getItem() instanceof ItemFocus)) {
                return ItemStack.EMPTY;
            }
            if (slot < ItemFocusPouch.SIZE) {
                if (!this.moveItemStackTo(stackInSlot, ItemFocusPouch.SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, ItemFocusPouch.SIZE, false)) {
                return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) {
                slotObject.set(ItemStack.EMPTY);
            } else {
                slotObject.setChanged();
            }
        }
        return stack;
    }

    /** Сумка должна оставаться в своей ячейке (в оригинале — закрытие на клиенте). */
    @Override
    public boolean stillValid(Player p) {
        return this.pouch().getItem() instanceof ItemFocusPouch;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        this.saveToPouch(this.input);
    }
}
