package thaumcraft.common.entities.construct.golem.gui;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.entities.construct.golem.seals.SealEntity;
import thaumcraft.common.entities.construct.golem.seals.SealHandler;
import thaumcraft.common.lib.TCMenus;

/**
 * Окно настройки печати (порт SealBaseContainer). Разделы (категории): 0 — приоритет, цвет, замок; 1 — фильтр (слоты-
 * призраки); 2 — зона; 3 — переключатели; 4 — нужные/запрещённые черты. Кнопки — clickMenuButton (номера оригинала).
 * Слоты фильтра созданы всегда и активны только в разделе фильтра (в 1.12 список слотов пересоздавался).
 * Клиент работает с копией печати из данных открытия; значения держатся в согласии числами окна.
 * Менять печать могут её владелец, его команда и операторы (в оригинале — кто угодно, DEVIATIONS).
 */
public class SealBaseContainer extends AbstractContainerMenu {
    public static final int MAX_FILTER = 9;
    private final Level world;
    final ISealEntity seal;
    final Player player;
    final SimpleContainer temp;
    final int[] categories;
    int category = -1;
    final int filterSlots;

    public SealBaseContainer(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, readSeal(inv.player.level(), buf));
    }

    private static ISealEntity readSeal(Level level, RegistryFriendlyByteBuf buf) {
        SealEntity se = new SealEntity();
        CompoundTag tag = buf.readNbt();
        if (tag != null) {
            se.readNBT(tag, level.registryAccess());
        }
        return se;
    }

    public SealBaseContainer(int id, Inventory inv, ISealEntity seal) {
        super(TCMenus.SEAL.get(), id);
        this.world = inv.player.level();
        this.player = inv.player;
        this.seal = seal;
        this.categories = seal.getSeal() instanceof ISealGui g ? g.getGuiCategories() : new int[] {0, 4};
        this.category = this.categories.length > 0 ? this.categories[0] : 0;
        int s = seal.getSeal() instanceof ISealConfigFilter f ? Math.min(MAX_FILTER, f.getFilterSize()) : 0;
        this.filterSlots = s;
        this.temp = new SimpleContainer(Math.max(1, s));
        if (seal.getSeal() instanceof ISealConfigFilter f) {
            int sx = 16 + (s - 1) % 3 * 12;
            int sy = 16 + (s - 1) / 3 * 12;
            int middleX = 88;
            int middleY = 72;
            for (int a = 0; a < s; a++) {
                this.temp.setItem(a, f.getFilterSlot(a).copy());
                int x = a % 3;
                int y = a / 3;
                this.addSlot(new SlotGhost(this.temp, a, middleX + x * 24 - sx + 8, middleY + y * 24 - sy + 8));
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 150 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 208));
        }
        this.addDataSlot(slot(() -> this.seal.getPriority(), v -> this.seal.setPriority((byte) v)));
        this.addDataSlot(slot(() -> this.seal.getArea().getX(), v -> this.seal.setArea(new BlockPos(v, this.seal.getArea().getY(), this.seal.getArea().getZ()))));
        this.addDataSlot(slot(() -> this.seal.getArea().getY(), v -> this.seal.setArea(new BlockPos(this.seal.getArea().getX(), v, this.seal.getArea().getZ()))));
        this.addDataSlot(slot(() -> this.seal.getArea().getZ(), v -> this.seal.setArea(new BlockPos(this.seal.getArea().getX(), this.seal.getArea().getY(), v))));
        this.addDataSlot(slot(() -> this.seal.getColor(), v -> this.seal.setColor((byte) v)));
        this.addDataSlot(slot(() -> (this.seal.isLocked() ? 1 : 0) | (this.seal.isRedstoneSensitive() ? 2 : 0)
                | (this.seal.getSeal() instanceof ISealConfigFilter f && f.isBlacklist() ? 4 : 0), v -> {
                    this.seal.setLocked((v & 1) != 0);
                    this.seal.setRedstoneSensitive((v & 2) != 0);
                    if (this.seal.getSeal() instanceof ISealConfigFilter f) {
                        f.setBlacklist((v & 4) != 0);
                    }
                }));
        this.addDataSlot(slot(() -> {
            int m = 0;
            if (this.seal.getSeal() instanceof ISealConfigToggles t) {
                for (int a = 0; a < t.getToggles().length && a < 15; a++) {
                    if (t.getToggles()[a].getValue()) {
                        m |= 1 << a;
                    }
                }
            }
            return m;
        }, v -> {
            if (this.seal.getSeal() instanceof ISealConfigToggles t) {
                for (int a = 0; a < t.getToggles().length && a < 15; a++) {
                    t.setToggle(a, (v & 1 << a) != 0);
                }
            }
        }));
        for (int a = 0; a < s; a++) {
            final int slot = a;
            this.addDataSlot(slot(() -> ((ISealConfigFilter) this.seal.getSeal()).getFilterSlotSize(slot),
                    v -> ((ISealConfigFilter) this.seal.getSeal()).setFilterSlotSize(slot, v)));
        }
    }

    private static DataSlot slot(java.util.function.IntSupplier get, java.util.function.IntConsumer set) {
        return new DataSlot() {
            @Override
            public int get() {
                return get.getAsInt();
            }

            @Override
            public void set(int value) {
                set.accept(value);
            }
        };
    }

    public ISealEntity getSeal() {
        return this.seal;
    }

    public int getCategory() {
        return this.category;
    }

    public void setCategoryClient(int cat) {
        this.category = cat;
    }

    /** Слот-призрак фильтра: виден и щёлкается только в разделе фильтра; предмет не забирается. */
    class SlotGhost extends Slot {
        SlotGhost(SimpleContainer inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isActive() {
            return SealBaseContainer.this.category == 1;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** Может ли игрок менять печать: владелец, его команда, оператор. */
    public static boolean canEdit(Player player, ISealEntity seal) {
        String owner = seal.getOwner();
        if (owner == null || owner.isEmpty() || owner.equals(player.getUUID().toString())) {
            return true;
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        MinecraftServer server = player.getServer();
        PlayerTeam team = player.getTeam() instanceof PlayerTeam t ? t : null;
        if (server != null && team != null) {
            try {
                Optional<GameProfile> prof = server.getProfileCache() == null ? Optional.empty() : server.getProfileCache().get(UUID.fromString(owner));
                return prof.isPresent() && team.getPlayers().contains(prof.get().getName());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.world.isClientSide) {
            return true;
        }
        SealPos sp = this.seal.getSealPos();
        return sp != null && SealHandler.getSealEntity(this.world.dimension(), sp) == this.seal
                && player.distanceToSqr(sp.pos.getX() + 0.5, sp.pos.getY() + 0.5, sp.pos.getZ() + 0.5) <= 64.0;
    }

    private void changed() {
        if (this.world instanceof ServerLevel sl) {
            this.seal.syncToClient(sl);
            SealHandler.markChunkAsDirty(sl, this.seal.getSealPos().pos);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int par2) {
        if (par2 >= 0 && par2 < this.categories.length) {
            this.category = this.categories[par2];
            return true;
        }
        if (!canEdit(player, this.seal)) {
            return false;
        }
        boolean done = this.applyButton(par2);
        if (done) {
            this.changed();
        }
        return done;
    }

    private boolean applyButton(int par2) {
        if (this.category == 3 && this.seal.getSeal() instanceof ISealConfigToggles cp && par2 >= 30 && par2 < 30 + cp.getToggles().length) {
            cp.setToggle(par2 - 30, true);
            return true;
        }
        if (this.category == 3 && this.seal.getSeal() instanceof ISealConfigToggles cp && par2 >= 60 && par2 < 60 + cp.getToggles().length) {
            cp.setToggle(par2 - 60, false);
            return true;
        }
        if (this.category == 0 && par2 >= 25 && par2 <= 26) {
            this.seal.setLocked(par2 == 25);
            return true;
        }
        if (par2 >= 27 && par2 <= 28) {
            this.seal.setRedstoneSensitive(par2 == 27);
            return true;
        }
        if (this.category == 1 && this.seal.getSeal() instanceof ISealConfigFilter cp && par2 >= 20 && par2 <= 21) {
            cp.setBlacklist(par2 == 20);
            return true;
        }
        if (par2 == 80 && this.seal.getPriority() > -5) {
            this.seal.setPriority((byte) (this.seal.getPriority() - 1));
            return true;
        }
        if (par2 == 81 && this.seal.getPriority() < 5) {
            this.seal.setPriority((byte) (this.seal.getPriority() + 1));
            return true;
        }
        if (par2 == 82 && this.seal.getColor() > 0) {
            this.seal.setColor((byte) (this.seal.getColor() - 1));
            return true;
        }
        if (par2 == 83 && this.seal.getColor() < 16) {
            this.seal.setColor((byte) (this.seal.getColor() + 1));
            return true;
        }
        if (this.seal.getSeal() instanceof ISealConfigArea) {
            BlockPos a = this.seal.getArea();
            if (par2 == 90 && a.getY() > 1) {
                this.seal.setArea(a.offset(0, -1, 0));
                return true;
            }
            if (par2 == 91 && a.getY() < 8) {
                this.seal.setArea(a.offset(0, 1, 0));
                return true;
            }
            if (par2 == 92 && a.getX() > 1) {
                this.seal.setArea(a.offset(-1, 0, 0));
                return true;
            }
            if (par2 == 93 && a.getX() < 8) {
                this.seal.setArea(a.offset(1, 0, 0));
                return true;
            }
            if (par2 == 94 && a.getZ() > 1) {
                this.seal.setArea(a.offset(0, 0, -1));
                return true;
            }
            if (par2 == 95 && a.getZ() < 8) {
                this.seal.setArea(a.offset(0, 0, 1));
                return true;
            }
        }
        return false;
    }

    /**
     * Щелчок по слоту фильтра: предмет в руке — образец (копия 1 шт.; с ограничителями количества — повторный щелчок тем же
     * предметом прибавляет его количество), ПКМ — убавить/очистить, пустой рукой ЛКМ — +1. Только на сервере.
     */
    @Override
    public void clicked(int slotId, int button, ClickType mode, Player playerIn) {
        if (slotId >= 0 && slotId < this.slots.size() && this.slots.get(slotId) instanceof SlotGhost slot) {
            if (!playerIn.level().isClientSide && slot.isActive() && canEdit(playerIn, this.seal) && this.seal.getSeal() instanceof ISealConfigFilter cf) {
                ItemStack ic = this.getCarried().isEmpty() ? ItemStack.EMPTY : this.getCarried().copy();
                int idx = slot.getContainerSlot();
                boolean filter = cf.hasStacksizeLimiters();
                if (button == 1) {
                    if (!filter) {
                        slot.set(ItemStack.EMPTY);
                        cf.setFilterSlotSize(idx, 0);
                    } else if (ic.isEmpty()) {
                        if (mode == ClickType.QUICK_MOVE) {
                            slot.set(ItemStack.EMPTY);
                            cf.setFilterSlotSize(idx, 0);
                        } else if (slot.hasItem()) {
                            cf.setFilterSlotSize(idx, cf.getFilterSlotSize(idx) - 1);
                            if (cf.getFilterSlotSize(idx) < 0) {
                                slot.set(ItemStack.EMPTY);
                                cf.setFilterSlotSize(idx, 0);
                            }
                        }
                    } else if (slot.hasItem() && ItemStack.isSameItemSameComponents(ic, slot.getItem())) {
                        cf.setFilterSlotSize(idx, cf.getFilterSlotSize(idx) - ic.getCount());
                        if (cf.getFilterSlotSize(idx) < 0) {
                            slot.set(ItemStack.EMPTY);
                            cf.setFilterSlotSize(idx, 0);
                        }
                    }
                } else if (ic.isEmpty()) {
                    if (filter && slot.hasItem()) {
                        cf.setFilterSlotSize(idx, cf.getFilterSlotSize(idx) + 1);
                    }
                } else {
                    int os = ic.getCount();
                    ic.setCount(1);
                    if (!filter) {
                        cf.setFilterSlotSize(idx, 0);
                    } else if (slot.hasItem() && ItemStack.isSameItemSameComponents(ic, slot.getItem())) {
                        cf.setFilterSlotSize(idx, cf.getFilterSlotSize(idx) + os);
                    } else {
                        cf.setFilterSlotSize(idx, 0);
                    }
                    slot.set(ic);
                }
                cf.setFilterSlot(idx, this.temp.getItem(idx).copy());
                this.changed();
                this.broadcastChanges();
            }
            return;
        }
        super.clicked(slotId, button, mode, playerIn);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** Открыть окно печати (ПКМ колокольчиком). */
    public static void open(net.minecraft.server.level.ServerPlayer sp, ISealEntity se) {
        sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new SealBaseContainer(id, inv, se),
                net.minecraft.network.chat.Component.translatable("item.thaumcraft.golem_bell")), buf -> buf.writeNbt(se.writeNBT(sp.registryAccess())));
    }

}
