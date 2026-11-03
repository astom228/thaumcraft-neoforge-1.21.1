package thaumcraft.common.container;

import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.common.entities.construct.golem.seals.SealEntity;
import thaumcraft.common.entities.construct.golem.seals.SealHandler;
import thaumcraft.common.entities.construct.golem.seals.SealProvide;
import thaumcraft.common.lib.TCMenus;

/**
 * Окно логистики (порт ContainerLogistics): 81 ячейка-витрина с суммарным содержимым контейнеров под печатями снабжения
 * игрока в 32 блоках (по фильтрам печатей и строке поиска), прокрутка по 9. Кнопки: 0/1 — строка вниз/вверх, 22 —
 * обновить, 100+n — прокрутить к строке n. Куда доставлять (блок или сам игрок) хранит серверное меню.
 */
public class ContainerLogistics extends AbstractContainerMenu {
    public static final int SIZE = 81;
    private final Level worldObj;
    final Player player;
    public final SimpleContainer input = new SimpleContainer(SIZE) {
        @Override
        public int getMaxStackSize() {
            return Integer.MAX_VALUE;
        }
    };
    TreeMap<String, ItemStack> items = new TreeMap<>();
    int lastTotal = 0;
    public int start = 0;
    public int end = 0;
    public String searchText = "";
    int lastStart = -1;
    public boolean updated = false;
    private boolean refreshPending;
    private long nextFullRefresh = Long.MIN_VALUE;
    /** Куда везти: блок и сторона (null — игроку). Только на сервере. */
    public final BlockPos target;
    public final Direction side;

    public ContainerLogistics(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, null);
    }

    public ContainerLogistics(int id, Inventory inv, BlockPos target, Direction side) {
        super(TCMenus.LOGISTICS.get(), id);
        this.worldObj = inv.player.level();
        this.player = inv.player;
        this.target = target;
        this.side = side;
        for (int a = 0; a < SIZE; a++) {
            this.addSlot(new Slot(this.input, a, 19 + a % 9 * 19, 19 + a / 9 * 19) {
                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public int getMaxStackSize() {
                    return Integer.MAX_VALUE;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return Integer.MAX_VALUE;
                }
            });
        }
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ContainerLogistics.this.start;
            }

            @Override
            public void set(int v) {
                ContainerLogistics.this.start = v;
                ContainerLogistics.this.updated = true;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ContainerLogistics.this.end;
            }

            @Override
            public void set(int v) {
                ContainerLogistics.this.end = v;
                ContainerLogistics.this.updated = true;
            }
        });
        if (!this.worldObj.isClientSide) {
            this.refreshItemList(true);
        }
    }

    private static String key(ItemStack stack) {
        return stack.getHoverName().getString() + "|" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|"
                + stack.getComponentsPatch();
    }

    /** Частые пакеты объединяются: сканируем инвентари не чаще раза в 5 серверных тиков. */
    public void setSearchText(String text) {
        if (!this.worldObj.isClientSide && !this.searchText.equals(text)) {
            this.searchText = text;
            this.refreshPending = true;
        }
    }

    @Override
    public void broadcastChanges() {
        if (!this.worldObj.isClientSide && this.refreshPending && this.worldObj.getGameTime() >= this.nextFullRefresh) {
            this.refreshPending = false;
            this.nextFullRefresh = this.worldObj.getGameTime() + 5;
            this.refreshItemList(true);
        }
        super.broadcastChanges();
    }

    public void refreshItemList(boolean full) {
        if (this.worldObj.isClientSide) {
            return;
        }
        int newTotal = this.lastTotal;
        TreeMap<String, ItemStack> ti = new TreeMap<>();
        if (full) {
            newTotal = 0;
            for (SealEntity seal : SealHandler.getSealsInRange(this.worldObj, this.player.blockPosition(), 32)) {
                if (seal.getSeal() instanceof SealProvide sp && this.player.getUUID().toString().equals(seal.getOwner())) {
                    IItemHandler handler = ThaumcraftApiHelper.getItemHandlerAt(this.worldObj, seal.getSealPos().pos, seal.getSealPos().face);
                    if (handler == null) {
                        continue;
                    }
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        ItemStack stack = handler.getStackInSlot(slot).copy();
                        if (!stack.isEmpty() && sp.matchesFilters(stack)
                                && (this.searchText.isEmpty() || stack.getHoverName().getString().toLowerCase().contains(this.searchText.toLowerCase()))) {
                            String k = key(stack);
                            if (ti.containsKey(k)) {
                                stack.grow(ti.get(k).getCount());
                            }
                            ti.put(k, stack);
                            newTotal += stack.getCount();
                        }
                    }
                }
            }
        }
        if (full || this.lastTotal != newTotal || this.start != this.lastStart) {
            this.lastTotal = newTotal;
            this.lastStart = this.start;
            if (full) {
                this.items = ti;
            }
            this.input.clearContent();
            int j = 0;
            int q = 0;
            for (ItemStack s : this.items.values()) {
                if (++j > this.start * 9) {
                    this.input.setItem(q, s.copy());
                    if (++q >= SIZE) {
                        break;
                    }
                }
            }
            this.end = this.items.size() / 9 - 8;
        }
    }

    /** Сколько такого предмета есть в доступных контейнерах (для проверки заказа). */
    public int available(ItemStack stack) {
        ItemStack s = this.items.get(key(stack));
        return s == null ? 0 : s.getCount();
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id == 22) {
            this.refreshPending = true;
            return true;
        }
        if (id == 0) {
            if (this.start < this.items.size() / 9 - 8) {
                this.start++;
                this.refreshItemList(false);
            }
            return true;
        }
        if (id == 1) {
            if (this.start > 0) {
                this.start--;
                this.refreshItemList(false);
            }
            return true;
        }
        if (id >= 100) {
            int s = id - 100;
            if (s <= this.items.size() / 9 - 8) {
                this.start = s;
                this.refreshItemList(false);
            }
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player p) {
        if (slotId >= 0 && slotId < SIZE) {
            return;
        }
        super.clicked(slotId, button, type, p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return true;
    }
}
