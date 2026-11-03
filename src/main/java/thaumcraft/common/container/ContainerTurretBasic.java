package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;
import thaumcraft.common.lib.TCMenus;

/**
 * Окно арбалета-турели (порт ContainerTurretBasic): ячейка стрел — это «рука» турели (SlotTurretBasic), ниже —
 * инвентарь игрока. Открывает только хозяин.
 */
public class ContainerTurretBasic extends AbstractContainerMenu {
    protected final EntityTurretCrossbow turret;

    public ContainerTurretBasic(int id, Inventory inv, FriendlyByteBuf buf) {
        this(TCMenus.TURRET_BASIC.get(), id, inv, (EntityTurretCrossbow) inv.player.level().getEntity(buf.readVarInt()), 80);
    }

    public ContainerTurretBasic(int id, Inventory inv, EntityTurretCrossbow turret) {
        this(TCMenus.TURRET_BASIC.get(), id, inv, turret, 80);
    }

    protected ContainerTurretBasic(MenuType<?> type, int id, Inventory inv, EntityTurretCrossbow turret, int slotX) {
        super(type, id);
        this.turret = turret;
        this.addSlot(new Slot(new HandContainer(turret), 0, slotX, 29) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && stack.getItem() instanceof ArrowItem;
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

    public EntityTurretCrossbow getTurret() {
        return this.turret;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.turret != null && this.turret.isAlive() && player.distanceToSqr(this.turret) <= 64.0;
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

    /** «Рука» турели как контейнер из одной ячейки (SlotMobEquipment оригинала). */
    public static class HandContainer implements Container {
        final net.minecraft.world.entity.Mob turret;

        public HandContainer(net.minecraft.world.entity.Mob turret) {
            this.turret = turret;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return this.turret.getMainHandItem().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.turret.getMainHandItem();
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack s = this.turret.getMainHandItem();
            if (s.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack out = s.split(amount);
            if (s.isEmpty()) {
                this.setItem(0, ItemStack.EMPTY);
            }
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack s = this.turret.getMainHandItem();
            this.setItem(0, ItemStack.EMPTY);
            return s;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.turret.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return this.turret.isAlive();
        }

        @Override
        public void clearContent() {
            this.setItem(0, ItemStack.EMPTY);
        }
    }
}
