package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Содержимое мешков с добычей по редкости (порт WeightedRandomLoot). Списки заполняются при запуске (ConfigLoot и дополнения
 * через ThaumcraftApi.addLootBagItem) и дальше только читаются на сервере.
 */
public class WeightedRandomLoot {
    public static final List<WeightedRandomLoot> lootBagCommon = new ArrayList<>();
    public static final List<WeightedRandomLoot> lootBagUncommon = new ArrayList<>();
    public static final List<WeightedRandomLoot> lootBagRare = new ArrayList<>();

    public final ItemStack item;
    public final int weight;

    public WeightedRandomLoot(ItemStack stack, int weight) {
        this.item = stack;
        this.weight = weight;
    }

    /** WeightedRandom.getRandomItem оригинала. */
    public static ItemStack pick(List<WeightedRandomLoot> list, RandomSource rand) {
        int total = 0;
        for (WeightedRandomLoot l : list) {
            total += l.weight;
        }
        if (total <= 0) {
            return ItemStack.EMPTY;
        }
        int r = rand.nextInt(total);
        for (WeightedRandomLoot l : list) {
            r -= l.weight;
            if (r < 0) {
                return l.item;
            }
        }
        return ItemStack.EMPTY;
    }
}
