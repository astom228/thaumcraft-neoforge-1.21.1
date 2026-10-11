package thaumcraft.api.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Содержимое магического верстака для поиска рецептов (InventoryArcaneWorkbench оригинала, 5×3 = 15 слотов):
 * 0–8 — сетка 3×3, 9–14 — кристаллы (воздух, огонь, вода, земля, порядок, энтропия).
 * В 1.12 рецепт получал сам инвентарь и проверял {@code instanceof IArcaneWorkbench}; в 1.21 магические рецепты —
 * отдельный тип и получают этот ввод.
 */
public record ArcaneCraftingInput(List<ItemStack> items, CraftingInput grid) implements RecipeInput, IArcaneWorkbench {
    public static final int GRID_SLOTS = 9;
    public static final int CRYSTAL_SLOTS = 6;
    public static final int SIZE = GRID_SLOTS + CRYSTAL_SLOTS;

    /** @param stacks содержимое слотов по порядку; недостающие слоты считаются пустыми. */
    public static ArcaneCraftingInput of(List<ItemStack> stacks) {
        List<ItemStack> list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            list.add(i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
        return new ArcaneCraftingInput(List.copyOf(list), CraftingInput.of(3, 3, list.subList(0, GRID_SLOTS)));
    }

    @Override
    public ItemStack getItem(int index) {
        return this.items.get(index);
    }

    @Override
    public int size() {
        return SIZE;
    }

    /** Слот кристалла 0–5. */
    public ItemStack getCrystal(int index) {
        return this.items.get(GRID_SLOTS + index);
    }
}
