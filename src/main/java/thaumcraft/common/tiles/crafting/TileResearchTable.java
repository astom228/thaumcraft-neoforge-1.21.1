package thaumcraft.common.tiles.crafting;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.items.IScribeTools;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraftInventory;

/**
 * Стол исследований (порт TileResearchTable): слот 0 — письменные принадлежности, слот 1 — бумага (оба видны клиенту);
 * data — текущая теория (уходит клиенту под ключом "note").
 */
public class TileResearchTable extends TileThaumcraftInventory {
    public ResearchTableData data = null;

    public TileResearchTable(BlockPos pos, BlockState state) {
        super(ConfigBlocks.RESEARCH_TABLE_TILE.get(), pos, state, 2);
        this.syncedSlots = new int[] {0, 1};
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        super.readSyncNBT(nbt, registries);
        if (nbt.contains("note")) {
            this.data = new ResearchTableData(this);
            this.data.deserialize(nbt.getCompound("note"), registries);
        } else {
            this.data = null;
        }
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeSyncNBT(nbt, registries);
        if (this.data != null) {
            nbt.put("note", this.data.serialize(registries));
        } else {
            nbt.remove("note");
        }
        return nbt;
    }

    public void startNewTheory(Player player, Set<String> mutators) {
        this.data = new ResearchTableData(player, this);
        this.data.initialize(player, mutators);
        this.syncTile(false);
        this.setChanged();
    }

    /** Завершение теории: очки категорий (по убыванию) → теория; после начала штрафа — ничего. */
    public void finishTheory(Player player) {
        Comparator<Map.Entry<String, Integer>> valueComparator = (e1, e2) -> e2.getValue().compareTo(e1.getValue());
        Map<String, Integer> sortedMap = this.data.categoryTotals.entrySet().stream().sorted(valueComparator)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        int i = 0;
        for (String cat : sortedMap.keySet()) {
            int tot = Math.round(sortedMap.get(cat) / 100.0F * IPlayerKnowledge.EnumKnowledgeType.THEORY.getProgression());
            if (i > this.data.penaltyStart) {
                tot *= 0;
            }
            ResearchCategory rc = ResearchCategories.getResearchCategory(cat);
            ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.THEORY, rc, tot);
            i++;
        }
        this.data = null;
    }

    /** Помощники в радиусе 4 по горизонтали и 1 по вертикали (блоки) и 5 (существа). Незагруженные чанки не проверяются. */
    public Set<String> checkSurroundingAids() {
        HashMap<String, ITheorycraftAid> mutators = new HashMap<>();
        if (this.level == null) {
            return mutators.keySet();
        }
        for (int y = -1; y <= 1; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos p = this.worldPosition.offset(x, y, z);
                    if (!this.level.hasChunkAt(p)) {
                        continue;
                    }
                    BlockState state = this.level.getBlockState(p);
                    for (String muk : TheorycraftManager.aids.keySet()) {
                        ITheorycraftAid mu = TheorycraftManager.aids.get(muk);
                        if (mu.getAidObject() instanceof Block b) {
                            if (state.getBlock() == b) {
                                mutators.put(muk, mu);
                            }
                        } else if (mu.getAidObject() instanceof ItemStack aid) {
                            ItemStack is = state.getBlock().getCloneItemStack(this.level, p, state);
                            if (!is.isEmpty() && ItemStack.isSameItem(is, aid)) {
                                mutators.put(muk, mu);
                            }
                        }
                    }
                }
            }
        }
        // EntityUtils.getEntitiesInRange: существа в радиусе 5 от центра стола
        AABB box = new AABB(this.worldPosition).inflate(5.0);
        List<Entity> l = this.level.getEntities((Entity) null, box,
                e -> e.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 25.0);
        for (Entity e : l) {
            for (String muk : TheorycraftManager.aids.keySet()) {
                ITheorycraftAid mu = TheorycraftManager.aids.get(muk);
                if (mu.getAidObject() instanceof Class<?> c && e.getClass().isAssignableFrom(c)) {
                    mutators.put(muk, mu);
                }
            }
        }
        return mutators.keySet();
    }

    /** Потратить одну единицу чернил (прочность письменных принадлежностей). */
    public boolean consumeInkFromTable() {
        ItemStack tools = this.getStackInSlot(0);
        if (tools.getItem() instanceof IScribeTools && tools.getDamageValue() < tools.getMaxDamage()) {
            ItemStack copy = tools.copy();
            copy.setDamageValue(copy.getDamageValue() + 1);
            this.setInventorySlotContents(0, copy);
            this.syncTile(false);
            this.setChanged();
            return true;
        }
        return false;
    }

    public boolean consumepaperFromTable() {
        if (this.getStackInSlot(1).is(Items.PAPER) && this.getStackInSlot(1).getCount() > 0) {
            this.decrStackSize(1, 1);
            this.syncTile(false);
            this.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return switch (i) {
            case 0 -> itemstack.getItem() instanceof IScribeTools;
            case 1 -> itemstack.is(Items.PAPER);
            default -> false;
        };
    }
}
