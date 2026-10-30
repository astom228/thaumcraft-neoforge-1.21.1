package thaumcraft.api.golems;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Голем глазами печатей и деталей (порт IGolemAPI). */
public interface IGolemAPI {
    LivingEntity getGolemEntity();

    IGolemProperties getProperties();

    void setProperties(IGolemProperties properties);

    Level getGolemWorld();

    /** Взять предмет; возвращает остаток. */
    ItemStack holdItem(ItemStack stack);

    /** Отдать предмет (пустой — первый несомый); возвращает отданное. */
    ItemStack dropItem(ItemStack stack);

    boolean canCarry(ItemStack stack, boolean partial);

    boolean isCarrying(ItemStack stack);

    NonNullList<ItemStack> getCarrying();

    void addRankXp(int xp);

    byte getGolemColor();

    void swingArm();

    boolean isInCombat();
}
