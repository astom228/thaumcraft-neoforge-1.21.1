package thaumcraft.common.container;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCMenus;

/**
 * Торговля с пехом (порт ContainerPech): ячейка для ценной вещи и 4 ячейки товара. Кнопка (clickMenuButton 0, проверка
 * на сервере) меняет вещь на товары: ценность ± разброс раскладывается на товары ценностью до 5, часть — из рюкзака
 * пеха; с шансом ценность/200 пех после сделки дичает. При закрытии вещи возвращаются игроку (что не влезло — падает
 * с меткой, чтобы пехи не подобрали).
 */
public class ContainerPech extends AbstractContainerMenu {
    private final EntityPech pech;
    private final Player player;
    private final Level level;
    public final SimpleContainer inventory = new SimpleContainer(5) {
        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return index == 0;
        }
    };

    public ContainerPech(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (EntityPech) inv.player.level().getEntity(buf.readVarInt()));
    }

    public ContainerPech(int id, Inventory inv, EntityPech pech) {
        super(TCMenus.PECH.get(), id);
        this.pech = pech;
        this.player = inv.player;
        this.level = inv.player.level();
        if (pech != null) {
            pech.trading = true;
        }
        this.addSlot(new Slot(this.inventory, 0, 36, 29));
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                this.addSlot(new Slot(this.inventory, 1 + j + i * 2, 106 + 18 * j, 20 + 18 * i) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
        }
    }

    public EntityPech getPech() {
        return this.pech;
    }

    /** Можно ли менять: ценная вещь во входной ячейке, ячейки товара пусты. */
    public boolean canTrade() {
        if (this.pech == null) {
            return false;
        }
        ItemStack in = this.inventory.getItem(0);
        return !in.isEmpty() && this.inventory.getItem(1).isEmpty() && this.inventory.getItem(2).isEmpty() && this.inventory.getItem(3).isEmpty()
                && this.inventory.getItem(4).isEmpty() && this.pech.isValued(in);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            this.generateContents();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    private boolean hasStuffInPack() {
        for (ItemStack stack : this.pech.loot) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void generateContents() {
        if (this.level.isClientSide || !this.canTrade()) {
            return;
        }
        var rand = this.level.random;
        int value = this.pech.getValue(this.inventory.getItem(0));
        if (rand.nextInt(100) <= value / 2) {
            this.pech.setTamed(false);
            this.pech.playSound(SoundsTC.pech_trade.get(), 0.4F, 1.0F);
        }
        if (rand.nextInt(5) == 0) {
            value += rand.nextInt(3);
        } else if (rand.nextBoolean()) {
            value -= rand.nextInt(3);
        }
        List<EntityPech.Trade> pos = EntityPech.trades(this.pech.getPechType(), this.level.registryAccess());
        while (value > 0) {
            int am = Math.min(5, Math.max((value + 1) / 2, rand.nextInt(value) + 1));
            value -= am;
            if (am == 1 && rand.nextBoolean() && this.hasStuffInPack()) {
                List<Integer> loot = new ArrayList<>();
                for (int a = 0; a < this.pech.loot.size(); a++) {
                    if (!this.pech.loot.get(a).isEmpty()) {
                        loot.add(a);
                    }
                }
                int r = loot.get(rand.nextInt(loot.size()));
                ItemStack is = this.pech.loot.get(r).copyWithCount(1);
                this.addStack(is);
                this.pech.loot.get(r).shrink(1);
                if (this.pech.loot.get(r).isEmpty()) {
                    this.pech.loot.set(r, ItemStack.EMPTY);
                }
            } else if (am < 4 || !rand.nextBoolean()) {
                final int want = am;
                List<EntityPech.Trade> fit = pos.stream().filter(t -> t.value() == want).toList();
                if (fit.isEmpty()) {
                    continue;
                }
                ItemStack is = fit.get(rand.nextInt(fit.size())).stack().copy();
                is.onCraftedBy(this.level, this.player, 0);
                this.addStack(is);
            }
        }
        this.inventory.removeItem(0, 1);
    }

    /** В оригинале после слияния цикл не прерывался — товар мог удвоиться (DEVIATIONS 59). */
    private void addStack(ItemStack s) {
        for (int a = 1; a < 5; a++) {
            ItemStack cur = this.inventory.getItem(a);
            if (cur.isEmpty()) {
                this.inventory.setItem(a, s);
                return;
            }
            if (ItemStack.isSameItemSameComponents(cur, s) && cur.getCount() + s.getCount() < cur.getMaxStackSize()) {
                cur.grow(s.getCount());
                this.inventory.setChanged();
                return;
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.pech != null && this.pech.isAlive() && this.pech.isTamed() && player.distanceToSqr(this.pech) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 5) {
                if (!this.moveItemStackTo(itemstack1, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 1, true)) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.pech != null) {
            this.pech.trading = false;
        }
        if (!this.level.isClientSide) {
            for (int a = 0; a < 5; a++) {
                ItemStack itemstack = this.inventory.removeItemNoUpdate(a);
                if (!itemstack.isEmpty() && !player.getInventory().add(itemstack)) {
                    ItemEntity ei = player.drop(itemstack, false);
                    if (ei != null) {
                        ei.getPersistentData().putBoolean(EntityPech.PECH_DROP, true);
                    }
                }
            }
        }
    }
}
