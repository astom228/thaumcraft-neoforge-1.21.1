package thaumcraft.common.entities.construct.golem.seals;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/** Улучшенная печать охраны (порт SealGuardAdvanced): выбор целей (монстры, животные, игроки); умный боец. */
public class SealGuardAdvanced extends SealGuard implements ISealConfigToggles {
    ResourceLocation iconAdv = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_guard_advanced");

    @Override
    public String getKey() {
        return "thaumcraft:guard_advanced";
    }

    @Override
    public ResourceLocation getSealIcon() {
        return this.iconAdv;
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
    public int[] getGuiCategories() {
        return new int[] {2, 3, 0, 4};
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] {EnumGolemTrait.FIGHTER, EnumGolemTrait.SMART};
    }
}
