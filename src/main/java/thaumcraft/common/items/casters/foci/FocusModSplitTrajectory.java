package thaumcraft.common.items.casters.foci;

import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.Trajectory;

/** Модификатор «Разделение» (порт FocusModSplitTrajectory): после него — несколько веток, каждая с теми же траекториями; сила ×0.75. */
public class FocusModSplitTrajectory extends FocusModSplit {
    @Override
    public String getResearch() {
        return "FOCUSSPLIT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.SPLITTRAJECTORY";
    }

    @Override
    public int getComplexity() {
        return 5;
    }

    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return new EnumSupplyType[] {EnumSupplyType.TRAJECTORY};
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] {EnumSupplyType.TRAJECTORY};
    }

    @Override
    public Trajectory[] supplyTrajectories() {
        return this.getParent().supplyTrajectories();
    }

    @Override
    public boolean execute() {
        return true;
    }
}
