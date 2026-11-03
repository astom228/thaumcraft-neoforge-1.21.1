package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import thaumcraft.common.entities.construct.EntityArcaneBore;
import thaumcraft.common.lib.TCMenus;

/** Окно магического бура (порт ContainerArcaneBore): ячейка кирки — «рука» бура (SlotArcaneBorePickaxe), инвентарь игрока. */
public class ContainerArcaneBore extends AbstractContainerMenu {
    private final EntityArcaneBore bore;

    public ContainerArcaneBore(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (EntityArcaneBore) inv.player.level().getEntity(buf.readVarInt()));
    }

    public ContainerArcaneBore(int id, Inventory inv, EntityArcaneBore bore) {
        super(TCMenus.ARCANE_BORE.get(), id);
        this.bore = bore;
        this.addSlot(new Slot(new ContainerTurretBasic.HandContainer(bore), 0, 80, 29) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && (stack.getItem() instanceof PickaxeItem || stack.is(ItemTags.PICKAXES));
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(inv, k, 8 + k * 18, 142));
        }
    }

    public EntityArcaneBore getBore() {
        return this.bore;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.bore != null && this.bore.isAlive() && player.distanceToSqr(this.bore) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slotObject = this.slots.get(slot);
        if (slotObject.hasItem()) {
            ItemStack stackInSlot = slotObject.getItem();
            stack = stackInSlot.copy();
            if (slot == 0) {
                if (!this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.slots.get(0).mayPlace(stackInSlot) || !this.moveItemStackTo(stackInSlot, 0, 1, false)) {
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
