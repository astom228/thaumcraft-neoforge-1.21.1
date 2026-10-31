package thaumcraft.common.entities.construct.golem.seals;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/**
 * Улучшенная печать опустошения (порт SealEmptyAdvanced): 9 образцов, настройки; «перебирать белый список» — каждая
 * задача берёт следующий образец по кругу. Только умные големы.
 */
public class SealEmptyAdvanced extends SealEmpty implements ISealConfigToggles {
    ResourceLocation iconAdv = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_empty_advanced");

    @Override
    public String getKey() {
        return "thaumcraft:empty_advanced";
    }

    @Override
    public int getFilterSize() {
        return 9;
    }

    @Override
    public ResourceLocation getSealIcon() {
        return this.iconAdv;
    }

    @Override
    public NonNullList<ItemStack> getInv(int c) {
        if (this.getToggles()[4].getValue() && !this.isBlacklist()) {
            List<ItemStack> w = new ArrayList<>();
            for (ItemStack s : super.getInv()) {
                if (!s.isEmpty()) {
                    w.add(s);
                }
            }
            if (!w.isEmpty()) {
                return NonNullList.withSize(1, w.get(Math.abs(c % w.size())));
            }
        }
        return super.getInv();
    }

    @Override
    public SealToggle[] getToggles() {
        return this.props;
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] {1, 3, 0, 4};
    }

    @Override
    public void setToggle(int indx, boolean value) {
        this.props[indx].setValue(value);
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] {EnumGolemTrait.SMART};
    }
}
