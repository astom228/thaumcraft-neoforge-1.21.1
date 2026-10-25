package thaumcraft.common.container;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.common.lib.TCMenus;
import thaumcraft.common.tiles.crafting.TileThaumatorium;

/**
 * Меню тауматориума (порт ContainerThaumatorium): слот катализатора и игрок. recipes — рецепты тигля с этим катализатором,
 * известные игроку, плюс уже выбранные; выбор (кнопка меню = номер рецепта) проверяет сервер по своему списку.
 */
public class ContainerThaumatorium extends AbstractContainerMenu {
    private final TileThaumatorium thaumatorium;
    private final Player player;
    public final List<CrucibleRecipe> recipes = new ArrayList<>();

    public ContainerThaumatorium(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (TileThaumatorium) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ContainerThaumatorium(int id, Inventory par1InventoryPlayer, TileThaumatorium tileEntity) {
        super(TCMenus.THAUMATORIUM.get(), id);
        this.player = par1InventoryPlayer.player;
        this.thaumatorium = tileEntity;
        this.addSlot(new SlotItemHandler(tileEntity.getItemHandler(), 0, 48, 16));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(par1InventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int var5 = 0; var5 < 9; var5++) {
            this.addSlot(new Slot(par1InventoryPlayer, var5, 8 + var5 * 18, 142));
        }
        this.updateRecipes();
    }

    public TileThaumatorium getTile() {
        return this.thaumatorium;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        this.updateRecipes();
    }

    public void updateRecipes() {
        this.recipes.clear();
        ItemStack cat = this.thaumatorium.getStackInSlot(0);
        if (!cat.isEmpty() && this.thaumatorium.recipeHash != null) {
            synchronized (ThaumcraftApi.getCraftingRecipes()) {
            for (IThaumcraftRecipe r : ThaumcraftApi.getCraftingRecipes().values()) {
                if (r instanceof CrucibleRecipe creps) {
                    if (ThaumcraftCapabilities.knowsResearchStrict(this.player, creps.getResearch()) && creps.catalystMatches(cat)) {
                        this.recipes.add(creps);
                    } else if (!this.thaumatorium.recipeHash.isEmpty()) {
                        for (Integer hash : this.thaumatorium.recipeHash) {
                            if (creps.hash == hash) {
                                this.recipes.add(creps);
                                break;
                            }
                        }
                    }
                }
            }
            }
        }
    }

    /** enchantItem оригинала: выбрать рецепт или снять выбор. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        this.updateRecipes();
        if (!this.recipes.isEmpty() && button >= 0 && button < this.recipes.size()) {
            boolean found = false;
            for (int a = 0; a < this.thaumatorium.recipeHash.size(); a++) {
                if (this.recipes.get(button).hash == this.thaumatorium.recipeHash.get(a)) {
                    found = true;
                    this.thaumatorium.recipeEssentia.remove(a);
                    this.thaumatorium.recipePlayer.remove(a);
                    this.thaumatorium.recipeHash.remove(a);
                    this.thaumatorium.currentCraft = -1;
                    break;
                }
            }
            if (!found) {
                // в оригинале предел числа рецептов проверял только экран — DEVIATIONS 33
                if (this.thaumatorium.recipeHash.size() >= this.thaumatorium.maxRecipes) {
                    return false;
                }
                this.thaumatorium.recipeEssentia.add(this.recipes.get(button).getAspects().copy());
                this.thaumatorium.recipePlayer.add(player.getGameProfile().getName());
                this.thaumatorium.recipeHash.add(this.recipes.get(button).hash);
            }
            this.thaumatorium.setChanged();
            this.thaumatorium.syncTile(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.thaumatorium.isUsableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int par2) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(par2);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (par2 != 0) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
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
}
