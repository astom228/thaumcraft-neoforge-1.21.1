package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.common.lib.TCMenus;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

/**
 * Окно пресса големов (порт ContainerGolemBuilder): слот выдачи (только забрать) и инвентарь игрока; стоимость и полная
 * стоимость текущей сборки — числами окна. Флаг «пересчитать детали» оригинала (общий static redo) — на стороне экрана.
 */
public class ContainerGolemBuilder extends AbstractContainerMenu {
    private final TileGolemBuilder builder;

    public ContainerGolemBuilder(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (TileGolemBuilder) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ContainerGolemBuilder(int id, Inventory inv, TileGolemBuilder tile) {
        super(TCMenus.GOLEM_BUILDER.get(), id);
        this.builder = tile;
        this.addSlot(new SlotItemHandler(tile.getItemHandler(), 0, 160, 104) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 24 + j * 18, 142 + i * 18));
            }
        }
        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(inv, k, 24 + k * 18, 200));
        }
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ContainerGolemBuilder.this.builder.cost;
            }

            @Override
            public void set(int value) {
                ContainerGolemBuilder.this.builder.cost = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ContainerGolemBuilder.this.builder.maxCost;
            }

            @Override
            public void set(int value) {
                ContainerGolemBuilder.this.builder.maxCost = value;
            }
        });
    }

    public TileGolemBuilder getTile() {
        return this.builder;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.builder.isUsableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slotObject = this.slots.get(slot);
        if (slotObject.hasItem()) {
            ItemStack stackInSlot = slotObject.getItem();
            stack = stackInSlot.copy();
            if (slot == 0) {
                if (!this.builder.isItemValidForSlot(slot, stackInSlot) || !this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // в оригинале — попытка положить в слот выдачи; он только для выдачи, так что перенос не выполняется
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
