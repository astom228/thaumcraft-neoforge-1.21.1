package thaumcraft.common.items.casters.foci;

import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.Trajectory;

/** Модификатор «Разделение» (порт FocusModSplitTarget): после него — несколько веток, каждая с теми же целями; сила ×0.75. */
public class FocusModSplitTarget extends FocusModSplit {
    @Override
    public String getResearch() {
        return "FOCUSSPLIT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.SPLITTARGET";
    }

    @Override
    public int getComplexity() {
        return 4;
    }

    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return new EnumSupplyType[] {EnumSupplyType.TARGET};
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] {EnumSupplyType.TARGET};
    }

    @Override
    public net.minecraft.world.phys.HitResult[] supplyTargets() {
        return this.getParent().supplyTargets();
    }

    @Override
    public boolean execute() {
        return true;
    }
}
