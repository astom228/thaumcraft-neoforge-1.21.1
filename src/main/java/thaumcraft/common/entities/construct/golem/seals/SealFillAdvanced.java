package thaumcraft.common.entities.construct.golem.seals;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/** Улучшенная печать хранения (порт SealFillAdvanced): 9 образцов, настройки сравнения; только умные големы. */
public class SealFillAdvanced extends SealFill implements ISealConfigToggles {
    ResourceLocation iconAdv = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_fill_advanced");

    @Override
    public String getKey() {
        return "thaumcraft:fill_advanced";
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
    public int[] getGuiCategories() {
        return new int[] {1, 3, 0, 4};
    }

    @Override
    public SealToggle[] getToggles() {
        return this.props;
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
