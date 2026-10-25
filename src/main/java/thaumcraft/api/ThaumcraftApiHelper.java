package thaumcraft.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.items.ItemGenericEssentiaContainer;
import thaumcraft.api.items.ItemsTC;

/** Порт ThaumcraftApiHelper (методы добавляются по этапам). */
public class ThaumcraftApiHelper {
    /**
     * Все предметы тега (в оригинале — предметы OreDictionary, с раскрытием metadata 32767).
     * Теги должны быть уже загружены (вызывается при построении таблицы аспектов).
     */
    public static List<ItemStack> getOresWithWildCards(TagKey<Item> tag) {
        List<ItemStack> out = new ArrayList<>();
        for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            out.add(new ItemStack(h));
        }
        return out;
    }

    /** Вис-кристалл с одной единицей аспекта (null, если аспект не задан). */
    public static ItemStack makeCrystal(Aspect aspect, int stackSize) {
        if (aspect == null) {
            return null;
        }
        ItemStack is = new ItemStack(ItemsTC.crystalEssence.get(), stackSize);
        ((ItemGenericEssentiaContainer) ItemsTC.crystalEssence.get()).setAspects(is, new AspectList().add(aspect, 1));
        return is;
    }

    public static ItemStack makeCrystal(Aspect aspect) {
        return makeCrystal(aspect, 1);
    }

    /**
     * Подходит ли предмет под образец рецепта (areItemStacksEqualForCrafting): in — ItemStack или тег предметов (вместо имени
     * OreDictionary). Компоненты сравниваются, как NBT в оригинале: если у stack0 есть изменённые компоненты, каждый
     * изменённый компонент образца должен быть у stack0 таким же; у stack0 без компонентов образец с компонентами подходит.
     */
    public static boolean areItemStacksEqualForCrafting(ItemStack stack0, Object in) {
        if (stack0 == null || in == null) {
            return stack0 == null && in == null;
        }
        if (in instanceof Object[]) {
            return true;
        }
        if (in instanceof TagKey<?> tag) {
            @SuppressWarnings("unchecked")
            TagKey<Item> itemTag = (TagKey<Item>) tag;
            return stack0.is(itemTag);
        }
        if (!(in instanceof ItemStack recipe)) {
            return false;
        }
        boolean t1 = stack0.getComponentsPatch().isEmpty() || areItemStackTagsEqualForCrafting(stack0, recipe);
        return t1 && !recipe.isEmpty() && recipe.getItem() == stack0.getItem();
    }

    public static boolean areItemStackTagsEqualForCrafting(ItemStack slotItem, ItemStack recipeItem) {
        if (recipeItem == null || slotItem == null) {
            return false;
        }
        if (!recipeItem.getComponentsPatch().isEmpty() && slotItem.getComponentsPatch().isEmpty()) {
            return false;
        }
        for (var e : recipeItem.getComponentsPatch().entrySet()) {
            var other = slotItem.getComponentsPatch().get(e.getKey());
            if (other == null || !other.equals(e.getValue())) {
                return false;
            }
        }
        return true;
    }

    /** Инвентарь блока со стороны side (возможность IItemHandler) или null. Незагруженные чанки не трогаются. */
    public static IItemHandler getItemHandlerAt(Level world, BlockPos pos, Direction side) {
        if (!world.hasChunkAt(pos)) {
            return null;
        }
        return world.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    /**
     * Ингредиент из описания рецепта оригинала: ItemStack с компонентами — совпадение по этим компонентам (IngredientNBTTC),
     * ItemStack / Item / Block — предмет, TagKey<Item> — тег (вместо имени OreDictionary), Ingredient — как есть.
     */
    @SuppressWarnings("unchecked")
    public static Ingredient getIngredient(Object obj) {
        if (obj instanceof Ingredient ing) {
            return ing;
        }
        if (obj instanceof ItemStack stack) {
            return stack.getComponentsPatch().isEmpty() ? Ingredient.of(stack) : DataComponentIngredient.of(false, stack);
        }
        if (obj instanceof ItemLike item) {
            return Ingredient.of(item);
        }
        if (obj instanceof TagKey<?> tag) {
            return Ingredient.of((TagKey<Item>) tag);
        }
        return null;
    }

    /** Соседний блок (по стороне face), передающий эссенцию и готовый соединиться с этой стороны; незагруженный чанк — null. */
    public static net.minecraft.world.level.block.entity.BlockEntity getConnectableTile(net.minecraft.world.level.BlockGetter world, BlockPos pos, Direction face) {
        BlockPos p = pos.relative(face);
        if (world instanceof net.minecraft.world.level.LevelReader lr && !lr.hasChunkAt(p)) {
            return null;
        }
        net.minecraft.world.level.block.entity.BlockEntity te = world.getBlockEntity(p);
        return te instanceof thaumcraft.api.aspects.IEssentiaTransport et && et.isConnectable(face.getOpposite()) ? te : null;
    }
}
