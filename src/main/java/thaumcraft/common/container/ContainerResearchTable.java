package thaumcraft.common.container;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.lib.TCMenus;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.crafting.TileResearchTable;

/**
 * Меню стола исследований (порт ContainerResearchTable): слоты принадлежностей и бумаги, инвентарь игрока
 * (панель быстрого доступа — сеткой 3×3 слева, как в оригинале). Действия — кнопки меню (enchantItem оригинала → clickMenuButton):
 * 1 — подтвердить выбор, 2/3 — вытянуть 2/3 карточки (тратит бумагу), 4–6 — разыграть карточку, 7 — завершить теорию, 9 — бросить.
 * Раздел 7: все действия проверяются на сервере (есть ли теория, допустим ли номер карточки).
 */
public class ContainerResearchTable extends AbstractContainerMenu {
    public final TileResearchTable tileEntity;
    final Player player;
    /** Защита от частых нажатий на карточки (333 мс), только сервер. */
    static final Map<UUID, Long> antiSpam = new ConcurrentHashMap<>();

    public ContainerResearchTable(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (TileResearchTable) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ContainerResearchTable(int id, Inventory inv, TileResearchTable tile) {
        super(TCMenus.RESEARCH_TABLE.get(), id);
        this.player = inv.player;
        this.tileEntity = tile;
        this.addSlot(new SlotItemHandler(tile.getItemHandler(), 0, 16, 15));
        this.addSlot(new SlotItemHandler(tile.getItemHandler(), 1, 224, 16));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 77 + j * 18, 190 + i * 18));
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(inv, i + j * 3, 20 + i * 18, 190 + j * 18));
            }
        }
    }

    public BlockPos getPos() {
        return this.tileEntity.getBlockPos();
    }

    @Override
    public boolean clickMenuButton(Player playerIn, int button) {
        ResearchTableData data = this.tileEntity.data;
        if (data == null) {
            return false;
        }
        if (button == 1) {
            if (data.lastDraw != null) {
                data.savedCards.add(data.lastDraw.card.getSeed());
            }
            for (ResearchTableData.CardChoice cc : data.cardChoices) {
                if (cc.selected) {
                    data.lastDraw = cc;
                    break;
                }
            }
            data.cardChoices.clear();
            this.tileEntity.syncTile(false);
            return true;
        }
        if (button == 4 || button == 5 || button == 6) {
            long tn = System.currentTimeMillis();
            long to = antiSpam.getOrDefault(playerIn.getUUID(), 0L);
            if (tn - to < 333L) {
                return false;
            }
            antiSpam.put(playerIn.getUUID(), tn);
            if (button - 4 >= data.cardChoices.size()) {
                return false;
            }
            try {
                TheorycraftCard card = data.cardChoices.get(button - 4).card;
                if (card.getRequiredItems() != null) {
                    for (ItemStack stack : card.getRequiredItems()) {
                        if (stack != null && !stack.isEmpty() && !InventoryUtils.isPlayerCarryingAmount(this.player, stack, true)) {
                            return false;
                        }
                    }
                    if (card.getRequiredItemsConsumed() != null && card.getRequiredItemsConsumed().length == card.getRequiredItems().length) {
                        for (int a = 0; a < card.getRequiredItems().length; a++) {
                            if (card.getRequiredItemsConsumed()[a] && card.getRequiredItems()[a] != null && !card.getRequiredItems()[a].isEmpty()) {
                                InventoryUtils.consumePlayerItem(this.player, card.getRequiredItems()[a], true, true);
                            }
                        }
                    }
                }
                if (card.activate(playerIn, data)) {
                    this.tileEntity.consumeInkFromTable();
                    data.cardChoices.get(button - 4).selected = true;
                    data.addInspiration(-card.getInspirationCost());
                    this.tileEntity.syncTile(false);
                    return true;
                }
            } catch (Exception e) {
                Thaumcraft.log.warn("Theorycraft card failed", e);
            }
        }
        if (button == 7 && data.isComplete()) {
            this.tileEntity.finishTheory(playerIn);
            this.tileEntity.syncTile(false);
            return true;
        }
        if (button == 9 && !data.isComplete()) {
            this.tileEntity.data = null;
            this.tileEntity.syncTile(false);
            return true;
        }
        if (button != 2 && button != 3) {
            return false;
        }
        if (!data.isComplete() && this.tileEntity.consumepaperFromTable()) {
            data.drawCards(button, playerIn);
            this.tileEntity.syncTile(false);
        }
        return true;
    }

    /**
     * Shift-клик: из стола — в инвентарь, из инвентаря — в подходящий слот стола. В оригинале перенос в стол не работал
     * (проверялся номер слота меню вместо слота стола) — исправлено (DEVIATIONS.md).
     */
    @Override
    public ItemStack quickMoveStack(Player par1EntityPlayer, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slotObject = this.slots.get(slot);
        if (slotObject.hasItem()) {
            ItemStack stackInSlot = slotObject.getItem();
            stack = stackInSlot.copy();
            if (slot < 2) {
                if (!this.moveItemStackTo(stackInSlot, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, 2, false)) {
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
        return this.tileEntity != null && this.tileEntity.isUsableByPlayer(player);
    }
}
