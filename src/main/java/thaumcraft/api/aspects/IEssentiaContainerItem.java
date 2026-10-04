package thaumcraft.api.aspects;

import net.minecraft.world.item.ItemStack;

/** Предмет, хранящий аспекты в самом стеке (кристаллы, фиалы, ярлыки…). */
public interface IEssentiaContainerItem {
    AspectList getAspects(ItemStack stack);

    void setAspects(ItemStack stack, AspectList aspects);

    /** true — аспекты содержимого не подменяют аспекты предмета при сканировании/в подсказке. */
    boolean ignoreContainedAspects();
}
