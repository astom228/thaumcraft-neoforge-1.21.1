package thaumcraft.common.container;

import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ArcaneCraftingInput;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.blocks.world.ore.ShardType;
import thaumcraft.common.container.slot.SlotCraftingArcaneWorkbench;
import thaumcraft.common.container.slot.SlotCrystal;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.lib.TCMenus;
import thaumcraft.common.lib.crafting.ConfigRecipes;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;

/**
 * Меню магического верстака (порт ContainerArcaneWorkbench): 0 — результат, 1–9 — сетка, 10–15 — кристаллы, 16–51 — игрок.
 * Вис ауры раз в 0,5 с пересчитывается на сервере и уходит клиенту свойством меню. Результат считает только сервер:
 * магический рецепт — при достатке Вис и кристаллов, иначе — обычный рецепт верстака по сетке 3×3.
 */
public class ContainerArcaneWorkbench extends AbstractContainerMenu {
    public static final int[] xx = new int[] {64, 17, 112, 17, 112, 64};
    public static final int[] yy = new int[] {13, 35, 35, 93, 93, 115};
    private final TileArcaneWorkbench tileEntity;
    private final Inventory ip;
    public final ResultContainer craftResult = new ResultContainer();
    private int lastVis = -1;
    private long lastCheck = 0L;

    public ContainerArcaneWorkbench(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (TileArcaneWorkbench) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ContainerArcaneWorkbench(int id, Inventory par1InventoryPlayer, TileArcaneWorkbench e) {
        super(TCMenus.ARCANE_WORKBENCH.get(), id);
        this.tileEntity = e;
        this.ip = par1InventoryPlayer;
        if (!e.getLevel().isClientSide) {
            e.addContainer(this);
        }
        e.getAura();
        this.addSlot(new SlotCraftingArcaneWorkbench(this.tileEntity, par1InventoryPlayer.player, this.craftResult, 0, 160, 64));
        for (int var6 = 0; var6 < 3; var6++) {
            for (int var7 = 0; var7 < 3; var7++) {
                this.addSlot(new SlotItemHandler(this.tileEntity.getItemHandler(), var7 + var6 * 3, 40 + var7 * 24, 40 + var6 * 24));
            }
        }
        for (ShardType st : ShardType.values()) {
            if (st.getMetadata() < 6) {
                this.addSlot(new SlotCrystal(st.getAspect(), this.tileEntity.getItemHandler(), st.getMetadata() + 9, xx[st.getMetadata()], yy[st.getMetadata()]));
            }
        }
        for (int var9 = 0; var9 < 3; var9++) {
            for (int var7 = 0; var7 < 9; var7++) {
                this.addSlot(new Slot(par1InventoryPlayer, var7 + var9 * 9 + 9, 16 + var7 * 18, 151 + var9 * 18));
            }
        }
        for (int var10 = 0; var10 < 9; var10++) {
            this.addSlot(new Slot(par1InventoryPlayer, var10, 16 + var10 * 18, 209));
        }
        // свойство 0 — Вис ауры (updateProgressBar оригинала)
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ContainerArcaneWorkbench.this.tileEntity.auraVisServer;
            }

            @Override
            public void set(int value) {
                ContainerArcaneWorkbench.this.tileEntity.auraVisClient = value;
            }
        });
        this.onCraftMatrixChanged();
    }

    public TileArcaneWorkbench getTile() {
        return this.tileEntity;
    }

    @Override
    public void broadcastChanges() {
        long t = System.currentTimeMillis();
        if (t > this.lastCheck) {
            this.lastCheck = t + 500L;
            this.tileEntity.getAura();
        }
        if (this.lastVis != this.tileEntity.auraVisServer) {
            this.onCraftMatrixChanged();
        }
        this.lastVis = this.tileEntity.auraVisServer;
        super.broadcastChanges();
    }

    /** onCraftMatrixChanged оригинала: вызывается и блоком при изменении содержимого. */
    public void onCraftMatrixChanged() {
        Level level = this.tileEntity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        ArcaneCraftingInput input = this.tileEntity.getCraftingInput();
        Optional<RecipeHolder<IArcaneRecipe>> found = level.getRecipeManager().getRecipeFor(ConfigRecipes.ARCANE_CRAFTING.get(), input, level);
        boolean hasVis = true;
        boolean hasCrystals = true;
        if (found.isPresent()) {
            IArcaneRecipe recipe = found.get().value();
            int vis = recipe.getVis();
            vis = (int) (vis * (1.0F - CasterManager.getTotalVisDiscount(this.ip.player)));
            AspectList crystals = recipe.getCrystals();
            this.tileEntity.getAura();
            hasVis = this.tileEntity.auraVisServer >= vis;
            if (crystals != null && crystals.size() > 0) {
                for (Aspect aspect : crystals.getAspects()) {
                    int have = 0;
                    for (int i = 0; i < ArcaneCraftingInput.CRYSTAL_SLOTS; i++) {
                        ItemStack s = input.getCrystal(i);
                        if (SlotCrystal.isValidCrystal(s, aspect)) {
                            have += s.getCount();
                        }
                    }
                    if (have < crystals.getAmount(aspect)) {
                        hasCrystals = false;
                        break;
                    }
                }
            }
        }
        ItemStack itemstack = ItemStack.EMPTY;
        // в оригинале при нехватке Вис или кристаллов результат не пересчитывался и мог остаться прежним (DEVIATIONS.md)
        if (hasVis && hasCrystals) {
            if (found.isPresent()) {
                this.craftResult.setRecipeUsed(found.get());
                itemstack = found.get().value().assemble(input, level.registryAccess());
            } else {
                CraftingInput grid = input.grid();
                Optional<RecipeHolder<CraftingRecipe>> vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
                if (vanilla.isPresent() && this.ip.player instanceof ServerPlayer sp && this.craftResult.setRecipeUsed(level, sp, vanilla.get())) {
                    itemstack = vanilla.get().value().assemble(grid, level.registryAccess());
                }
            }
        }
        this.craftResult.setItem(0, itemstack);
        if (this.ip.player instanceof ServerPlayer sp) {
            this.setRemoteSlot(0, itemstack);
            sp.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, itemstack));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!this.tileEntity.getLevel().isClientSide) {
            this.tileEntity.removeContainer(this);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.tileEntity.isUsableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int par1) {
        ItemStack var2x = ItemStack.EMPTY;
        Slot var3 = this.slots.get(par1);
        if (var3 != null && var3.hasItem()) {
            ItemStack var4 = var3.getItem();
            var2x = var4.copy();
            if (par1 == 0) {
                if (!this.moveItemStackTo(var4, 16, 52, true)) {
                    return ItemStack.EMPTY;
                }
                var3.onQuickCraft(var4, var2x);
            } else if (par1 >= 16 && par1 < 52) {
                for (ShardType st : ShardType.values()) {
                    if (st.getMetadata() < 6 && SlotCrystal.isValidCrystal(var4, st.getAspect())) {
                        if (!this.moveItemStackTo(var4, 10 + st.getMetadata(), 11 + st.getMetadata(), false)) {
                            return ItemStack.EMPTY;
                        }
                        if (var4.getCount() == 0) {
                            break;
                        }
                    }
                }
                if (var4.getCount() != 0) {
                    if (par1 < 43) {
                        if (!this.moveItemStackTo(var4, 43, 52, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(var4, 16, 43, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(var4, 16, 52, false)) {
                return ItemStack.EMPTY;
            }
            if (var4.getCount() == 0) {
                var3.setByPlayer(ItemStack.EMPTY);
            } else {
                var3.setChanged();
            }
            if (var4.getCount() == var2x.getCount()) {
                return ItemStack.EMPTY;
            }
            var3.onTake(player, var4);
        }
        return var2x;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.craftResult && super.canTakeItemForPickAll(stack, slot);
    }
}
