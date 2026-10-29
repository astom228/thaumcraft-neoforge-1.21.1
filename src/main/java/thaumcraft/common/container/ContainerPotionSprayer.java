package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.common.lib.TCMenus;
import thaumcraft.common.tiles.devices.TilePotionSprayer;

/** Меню распылителя зелий (порт ContainerPotionSprayer): 0 — зелье, 1–36 — игрок. Рецепт и заряды экран читает из блок-сущности. */
public class ContainerPotionSprayer extends AbstractContainerMenu {
    private final TilePotionSprayer sprayer;

    public ContainerPotionSprayer(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (TilePotionSprayer) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ContainerPotionSprayer(int id, Inventory inv, TilePotionSprayer tile) {
        super(TCMenus.POTION_SPRAYER.get(), id);
        this.sprayer = tile;
        this.addSlot(new SlotItemHandler(tile.getItemHandler(), 0, 56, 64));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 16 + j * 18, 151 + i * 18));
            }
        }
        for (int var5 = 0; var5 < 9; var5++) {
            this.addSlot(new Slot(inv, var5, 16 + var5 * 18, 209));
        }
    }

    public TilePotionSprayer getSprayer() {
        return this.sprayer;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.sprayer != null && !this.sprayer.isRemoved() && this.sprayer.isUsableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slotObject = this.slots.get(slot);
        if (slotObject != null && slotObject.hasItem()) {
            ItemStack stackInSlot = slotObject.getItem();
            stack = stackInSlot.copy();
            if (slot == 0) {
                if (!this.sprayer.isItemValidForSlot(slot, stackInSlot) || !this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.sprayer.isItemValidForSlot(slot, stackInSlot) || !this.moveItemStackTo(stackInSlot, 0, 1, false)) {
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
