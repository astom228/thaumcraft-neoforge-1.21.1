package thaumcraft.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
}
