package thaumcraft.common.container.slot;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ArcaneCraftingInput;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;

/**
 * Слот результата магического верстака (порт SlotCraftingArcaneWorkbench): при взятии тратит Вис (со скидкой),
 * по одному предмету из каждого занятого слота сетки и кристаллы рецепта; без магического рецепта работает как обычный верстак.
 */
public class SlotCraftingArcaneWorkbench extends Slot {
    private final Player player;
    private final TileArcaneWorkbench tile;
    private int amountCrafted;

    public SlotCraftingArcaneWorkbench(TileArcaneWorkbench te, Player player, Container result, int index, int x, int y) {
        super(result, index, x, y);
        this.player = player;
        this.tile = te;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    /** Результат мог устареть между пересчётом меню и кликом (знания, экипировка, Вис ауры). */
    @Override
    public boolean mayPickup(Player player) {
        if (player.level().isClientSide) return true;
        if (!this.hasItem()) return false;
        ArcaneCraftingInput input = this.tile.getCraftingInput();
        IArcaneRecipe recipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(input, player);
        ItemStack expected = ItemStack.EMPTY;
        if (recipe != null) {
            this.tile.getAura();
            int cost = (int) (recipe.getVis() * (1.0F - CasterManager.getTotalVisDiscount(player)));
            boolean ready = this.tile.auraVisServer >= cost;
            AspectList crystals = recipe.getCrystals();
            if (crystals != null) {
                for (Aspect aspect : crystals.getAspects()) {
                    int have = 0;
                    for (int i = 0; i < ArcaneCraftingInput.CRYSTAL_SLOTS; i++) {
                        ItemStack stack = input.getCrystal(i);
                        if (SlotCrystal.isValidCrystal(stack, aspect)) have += stack.getCount();
                    }
                    if (have < crystals.getAmount(aspect)) ready = false;
                }
            }
            if (ready) expected = recipe.assemble(input, player.level().registryAccess());
        } else {
            var vanilla = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input.grid(), player.level());
            if (vanilla.isPresent()) expected = vanilla.get().value().assemble(input.grid(), player.level().registryAccess());
        }
        if (!expected.isEmpty() && ItemStack.matches(expected, this.getItem())) return true;
        this.container.setItem(this.getSlotIndex(), ItemStack.EMPTY);
        this.setChanged();
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.amountCrafted += Math.min(amount, this.getItem().getCount());
        }
        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.amountCrafted += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void onSwapCraft(int amount) {
        this.amountCrafted += amount;
    }

    /** Содержимое верстака для события крафта (в оригинале — сам InventoryCrafting). */
    private Container matrixView() {
        SimpleContainer c = new SimpleContainer(ArcaneCraftingInput.SIZE);
        for (int i = 0; i < ArcaneCraftingInput.SIZE; i++) {
            c.setItem(i, this.tile.getStackInSlot(i));
        }
        return c;
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        if (this.amountCrafted > 0) {
            stack.onCraftedBy(this.player.level(), this.player, this.amountCrafted);
            EventHooks.firePlayerCraftingEvent(this.player, stack, this.matrixView());
        }
        this.amountCrafted = 0;
        if (this.container instanceof RecipeCraftingHolder holder) {
            holder.awardUsedRecipes(this.player, java.util.List.of());
        }
    }

    @Override
    public void onTake(Player thePlayer, ItemStack stack) {
        this.checkTakeAchievements(stack);
        ArcaneCraftingInput input = this.tile.getCraftingInput();
        IArcaneRecipe recipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(input, thePlayer);
        // остатки (вёдра и т.п.) по слотам сетки 0–8
        NonNullList<ItemStack> remaining = NonNullList.withSize(ArcaneCraftingInput.GRID_SLOTS, ItemStack.EMPTY);
        CommonHooks.setCraftingPlayer(thePlayer);
        if (recipe != null) {
            NonNullList<ItemStack> list = recipe.getRemainingItems(input);
            for (int i = 0; i < ArcaneCraftingInput.GRID_SLOTS && i < list.size(); i++) {
                remaining.set(i, list.get(i));
            }
        } else {
            CraftingInput.Positioned pos = CraftingInput.ofPositioned(3, 3, input.items().subList(0, ArcaneCraftingInput.GRID_SLOTS));
            CraftingInput grid = pos.input();
            NonNullList<ItemStack> list = thePlayer.level().getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, grid, thePlayer.level());
            for (int k = 0; k < grid.height(); k++) {
                for (int l = 0; l < grid.width(); l++) {
                    remaining.set(l + pos.left() + (k + pos.top()) * 3, list.get(l + k * grid.width()));
                }
            }
        }
        CommonHooks.setCraftingPlayer(null);
        AspectList crystals = null;
        if (recipe != null) {
            int vis = recipe.getVis();
            vis = (int) (vis * (1.0F - CasterManager.getTotalVisDiscount(thePlayer)));
            crystals = recipe.getCrystals();
            if (vis > 0) {
                this.tile.getAura();
                this.tile.spendAura(vis);
            }
        }
        for (int i = 0; i < ArcaneCraftingInput.GRID_SLOTS; i++) {
            ItemStack itemstack = this.tile.getStackInSlot(i);
            ItemStack itemstack1 = remaining.get(i);
            if (!itemstack.isEmpty()) {
                this.tile.decrStackSize(i, 1);
                itemstack = this.tile.getStackInSlot(i);
            }
            if (!itemstack1.isEmpty()) {
                if (itemstack.isEmpty()) {
                    this.tile.setInventorySlotContents(i, itemstack1);
                } else if (ItemStack.isSameItemSameComponents(itemstack, itemstack1)) {
                    itemstack1.grow(itemstack.getCount());
                    this.tile.setInventorySlotContents(i, itemstack1);
                } else if (!this.player.getInventory().add(itemstack1)) {
                    this.player.drop(itemstack1, false);
                }
            }
        }
        if (crystals != null) {
            for (Aspect aspect : crystals.getAspects()) {
                ItemStack cs = ThaumcraftApiHelper.makeCrystal(aspect, crystals.getAmount(aspect));
                for (int i = 0; i < ArcaneCraftingInput.CRYSTAL_SLOTS; i++) {
                    ItemStack itemstack1 = this.tile.getStackInSlot(ArcaneCraftingInput.GRID_SLOTS + i);
                    if (itemstack1.getItem() == ItemsTC.crystalEssence.get() && ItemStack.isSameItemSameComponents(cs, itemstack1)) {
                        this.tile.decrStackSize(ArcaneCraftingInput.GRID_SLOTS + i, cs.getCount());
                    }
                }
            }
        }
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
