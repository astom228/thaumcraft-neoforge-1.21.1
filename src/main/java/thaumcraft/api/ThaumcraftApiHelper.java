package thaumcraft.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
}
