package thaumcraft.common.tiles.crafting;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Фокусный манипулятор (порт TileFocalManipulator): дерево узлов будущего заклинания (редактирует экран, присылает сервер
 * проверяет), запуск — плата опытом и вис-кристаллами аспектов узлов, затем Вис из ауры (по 20 в секунду; с зарядником
 * сверху — с 3×3 чанков); по окончании фокус получает пакет и имя.
 */
public class TileFocalManipulator extends TileThaumcraftInventory {
    public float vis = 0.0F;
    public HashMap<Integer, FocusElementNode> data = new HashMap<>();
    public String focusName = "";
    int ticks = 0;
    public boolean doGather;
    public float visCost = 0.0F;
    public int xpCost = 0;
    private AspectList crystals = new AspectList();
    public AspectList crystalsSync = new AspectList();
    /** Только клиент: экран должен начать дерево заново (фокус сменился). */
    public boolean doGuiReset = false;
    private ItemStack lastFocus = ItemStack.EMPTY;
    private boolean readingSync;

    public TileFocalManipulator(BlockPos pos, BlockState state) {
        super(ConfigBlocks.FOCAL_MANIPULATOR_TILE.get(), pos, state, 1);
        this.syncedSlots = new int[] {0};
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        ItemStack previousFocus = this.getStackInSlot(0).copy();
        this.readingSync = true;
        try {
            super.readSyncNBT(nbt, registries);
        } finally {
            this.readingSync = false;
        }
        this.vis = nbt.getFloat("vis");
        this.focusName = nbt.getString("focusName");
        this.crystalsSync = new AspectList();
        this.crystalsSync.readFromNBT(nbt, "crystals");
        ListTag nodelist = nbt.getList("nodes", Tag.TAG_COMPOUND);
        this.data.clear();
        for (int x = 0; x < nodelist.size(); x++) {
            FocusElementNode node = new FocusElementNode();
            node.deserialize(nodelist.getCompound(x));
            this.data.put(node.id, node);
        }
        if (this.level != null && this.level.isClientSide) {
            // ItemsSynced temporarily clears the slot before restoring it. That is not
            // a player changing the focus: keep the authoritative in-progress tree.
            this.lastFocus = this.getStackInSlot(0).copy();
            this.doGuiReset = this.data.isEmpty() && !ItemStack.matches(previousFocus, this.lastFocus);
            this.doGather = true;
        }
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeSyncNBT(nbt, registries);
        nbt.putFloat("vis", this.vis);
        nbt.putString("focusName", this.focusName);
        this.crystalsSync.writeToNBT(nbt, "crystals");
        ListTag nodelist = new ListTag();
        for (FocusElementNode node : this.data.values()) {
            nodelist.add(node.serialize());
        }
        nbt.put("nodes", nodelist);
        return nbt;
    }

    /** setInventorySlotContents оригинала: другой фокус — клиент сбрасывает экран, сервер прерывает работу. */
    @Override
    protected void onSlotContentsChanged(int slot) {
        if (this.readingSync && this.level != null && this.level.isClientSide) {
            return;
        }
        ItemStack stack = this.getStackInSlot(slot);
        boolean changed = stack.isEmpty() || !ItemStack.matches(stack, this.lastFocus);
        this.lastFocus = stack.copy();
        if (!changed || this.level == null) {
            return;
        }
        if (this.level.isClientSide) {
            this.data.clear();
            this.doGuiReset = true;
        } else {
            this.vis = 0.0F;
            this.crystalsSync = new AspectList();
            this.setChanged();
            this.syncTile(false);
        }
    }

    public float spendAura(float vis) {
        if (this.level.getBlockState(this.getBlockPos().above()).getBlock() != BlocksTC.arcaneWorkbenchCharger.get()) {
            return AuraHandler.drainVis(this.level, this.getBlockPos(), vis, false);
        }
        float q = vis;
        float z = vis / 9.0F;
        for (int xx = -1; xx <= 1; xx++) {
            for (int zz = -1; zz <= 1; zz++) {
                if (z > q) {
                    z = q;
                }
                q -= AuraHandler.drainVis(this.level, this.getBlockPos().offset(xx * 16, 0, zz * 16), z, false);
                if (q <= 0.0F) {
                    return vis - q;
                }
            }
        }
        return vis - q;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileFocalManipulator tile) {
        tile.update();
    }

    void update() {
        boolean complete = false;
        this.ticks++;
        if (!this.level.isClientSide) {
            if (this.ticks % 20 == 0) {
                ItemStack focus = this.getStackInSlot(0);
                if (this.vis > 0.0F && (focus.isEmpty() || !(focus.getItem() instanceof ItemFocus))) {
                    complete = true;
                    this.vis = 0.0F;
                    this.level.playSound(null, this.worldPosition, SoundsTC.wandfail.get(), SoundSource.BLOCKS, 0.33F, 1.0F);
                }
                if (!complete && this.vis > 0.0F) {
                    float amt = this.spendAura(Math.min(20.0F, this.vis));
                    if (amt > 0.0F) {
                        this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 5, 1);
                        this.vis -= amt;
                        this.syncTile(false);
                        this.setChanged();
                    }
                    if (this.vis <= 0.0F && !focus.isEmpty() && focus.getItem() instanceof ItemFocus) {
                        complete = true;
                        this.endCraft();
                    }
                }
            }
        } else if (this.vis > 0.0F) {
            var r = this.level.random;
            thaumcraft.common.lib.fx.GenPart pp = new thaumcraft.common.lib.fx.GenPart();
            pp.redStart = pp.redEnd = 0.5F + r.nextFloat() * 0.4F;
            pp.greenStart = pp.greenEnd = 1.0F - r.nextFloat() * 0.4F;
            pp.blueStart = pp.blueEnd = 1.0F - r.nextFloat() * 0.4F;
            pp.alpha = new float[] {0.8F};
            pp.partStart = 448;
            pp.partNum = 9;
            pp.partInc = 1;
            pp.age = 6 + r.nextInt(5);
            pp.scale = new float[] {0.3F + r.nextFloat() * 0.3F};
            IFXDispatcher.get().drawGenericParticles(this.worldPosition.getX() + 0.5 + (r.nextFloat() - r.nextFloat()) * 0.3F,
                    this.worldPosition.getY() + 1.4 + (r.nextFloat() - r.nextFloat()) * 0.3F,
                    this.worldPosition.getZ() + 0.5 + (r.nextFloat() - r.nextFloat()) * 0.3F, 0.0, 0.0, 0.0, pp);
        }
        if (complete) {
            this.vis = 0.0F;
            this.syncTile(false);
            this.setChanged();
        }
    }

    private FocusPackage generateFocus() {
        if (this.data == null || this.data.isEmpty()) {
            return null;
        }
        FocusPackage core = new FocusPackage();
        int totalComplexity = 0;
        HashMap<String, Integer> compCount = new HashMap<>();
        for (FocusElementNode node : this.data.values()) {
            if (node.node != null) {
                int a = compCount.getOrDefault(node.node.getKey(), 0);
                node.complexityMultiplier = 0.5F * (++a + 1);
                compCount.put(node.node.getKey(), a);
                totalComplexity = (int) (totalComplexity + node.node.getComplexity() * node.complexityMultiplier);
            }
        }
        core.setComplexity(totalComplexity);
        this.traverseChildren(core, this.data.get(0));
        return core;
    }

    private void traverseChildren(FocusPackage currentPackage, FocusElementNode currentNode) {
        if (currentPackage == null || currentNode == null) {
            return;
        }
        currentPackage.addNode(currentNode.node);
        if (currentNode.children != null && currentNode.children.length != 0) {
            if (currentNode.children.length == 1) {
                this.traverseChildren(currentPackage, this.data.get(currentNode.children[0]));
            } else {
                FocusModSplit splitNode = (FocusModSplit) currentNode.node;
                splitNode.getSplitPackages().clear();
                for (int c : currentNode.children) {
                    FocusPackage splitPackage = new FocusPackage();
                    this.traverseChildren(splitPackage, this.data.get(c));
                    splitNode.getSplitPackages().add(splitPackage);
                }
            }
        }
    }

    public void endCraft() {
        this.vis = 0.0F;
        ItemStack focus = this.getStackInSlot(0);
        if (focus.isEmpty() || !(focus.getItem() instanceof ItemFocus)) {
            return;
        }
        // повторная проверка: дерево могли подменить пакетом во время работы
        if (!FocusElementNode.isStructureValid(this.data) || !FocusElementNode.isCompositionValid(this.data)) {
            this.syncTile(false);
            return;
        }
        FocusPackage core = this.generateFocus();
        if (core != null) {
            this.level.playSound(null, this.worldPosition, SoundsTC.wand.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            ItemStack out = focus.copy();
            if (!this.focusName.isEmpty()) {
                out.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(this.focusName));
            }
            ItemFocus.setPackage(out, core);
            this.lastFocus = out.copy();
            this.setInventorySlotContents(0, out);
            this.crystalsSync = new AspectList();
            this.data.clear();
            this.syncTile(false);
            this.setChanged();
        }
    }

    /** Запуск (кнопка меню). Сервер проверяет дерево, сложность, исследования, опыт и кристаллы. */
    public boolean startCraft(int id, Player p) {
        ItemStack focus = this.getStackInSlot(0);
        if (this.data == null || this.data.isEmpty() || this.vis > 0.0F || focus.isEmpty() || !(focus.getItem() instanceof ItemFocus itemFocus)) {
            return false;
        }
        if (!FocusElementNode.isStructureValid(this.data) || !FocusElementNode.isCompositionValid(this.data)) {
            return false;
        }
        int maxComplexity = itemFocus.getMaxComplexity();
        int totalComplexity = 0;
        this.crystals = new AspectList();
        HashMap<String, Integer> compCount = new HashMap<>();
        for (FocusElementNode node : this.data.values()) {
            if (node.node == null || !ThaumcraftCapabilities.knowsResearchStrict(p, node.node.getResearch())) {
                return false;
            }
            int a = compCount.getOrDefault(node.node.getKey(), 0);
            node.complexityMultiplier = 0.5F * (++a + 1);
            compCount.put(node.node.getKey(), a);
            totalComplexity = (int) (totalComplexity + node.node.getComplexity() * node.complexityMultiplier);
            if (node.node.getAspect() != null) {
                this.crystals.add(node.node.getAspect(), 1);
            }
        }
        // предел сложности в оригинале проверял только экран
        if (totalComplexity > maxComplexity) {
            return false;
        }
        // как в оригинале: min(1, …) — опыт всегда 1 уровень
        this.xpCost = (int) Math.min(1L, Math.round(Math.sqrt(totalComplexity)));
        if (!p.getAbilities().instabuild && p.experienceLevel < this.xpCost) {
            return false;
        }
        if (this.crystals.getAspects().length <= 0) {
            return false;
        }
        ItemStack[] components = new ItemStack[this.crystals.getAspects().length];
        int r = 0;
        for (Aspect as : this.crystals.getAspects()) {
            components[r++] = ThaumcraftApiHelper.makeCrystal(as, this.crystals.getAmount(as));
        }
        for (ItemStack c : components) {
            if (!InventoryUtils.isPlayerCarryingAmount(p, c, false)) {
                return false;
            }
        }
        // опыт — после проверки кристаллов (в оригинале снимался раньше и терялся при нехватке кристаллов)
        if (!p.getAbilities().instabuild) {
            p.giveExperienceLevels(-this.xpCost);
        }
        for (ItemStack c : components) {
            InventoryUtils.consumePlayerItem(p, c, true, false);
        }
        this.vis = totalComplexity * 10 + maxComplexity / 5;
        this.crystalsSync = this.crystals.copy();
        this.setChanged();
        this.syncTile(false);
        this.level.playSound(null, this.worldPosition, SoundsTC.craftstart.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int par1, ItemStack stack) {
        return stack.getItem() instanceof ItemFocus;
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == 1) {
            this.doGuiReset = true;
        }
        if (i == 5) {
            if (this.level != null && this.level.isClientSide) {
                var r = this.level.random;
                IFXDispatcher.get().visSparkle(this.worldPosition.getX() + r.nextInt(3) - r.nextInt(3), this.worldPosition.getY() + r.nextInt(3),
                        this.worldPosition.getZ() + r.nextInt(3) - r.nextInt(3), this.worldPosition.getX(), this.worldPosition.getY() + 1,
                        this.worldPosition.getZ(), j);
            }
            return true;
        }
        return super.triggerEvent(i, j);
    }
}
