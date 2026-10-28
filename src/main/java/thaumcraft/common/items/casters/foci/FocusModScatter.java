package thaumcraft.common.items.casters.foci;

import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.casters.FocusMod;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

/** Модификатор «Рассеивание» (порт FocusModScatter): траектория разветвляется на 2–10 в конусе; сила делится. */
public class FocusModScatter extends FocusMod {
    @Override
    public String getResearch() {
        return "FOCUSSCATTER";
    }

    @Override
    public String getKey() {
        return "thaumcraft.SCATTER";
    }

    @Override
    public int getComplexity() {
        return (int) Math.max(2.0F, 2.0F * (this.getSettingValue("forks") - this.getSettingValue("cone") / 45.0F));
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] angles = {10, 30, 60, 90, 180, 270, 360};
        String[] anglesDesc = {"10", "30", "60", "90", "180", "270", "360"};
        return new NodeSetting[] {
                new NodeSetting("forks", "focus.scatter.forks", new NodeSetting.NodeSettingIntRange(2, 10)),
                new NodeSetting("cone", "focus.scatter.cone", new NodeSetting.NodeSettingIntList(angles, anglesDesc))
        };
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
        if (this.getParent() == null) {
            return new Trajectory[0];
        }
        ArrayList<Trajectory> tT = new ArrayList<>();
        int forks = this.getSettingValue("forks");
        int angle = this.getSettingValue("cone");
        var rand = this.getPackage().world.random;
        if (this.getParent().supplyTrajectories() != null) {
            for (Trajectory sT : this.getParent().supplyTrajectories()) {
                for (int a = 0; a < forks; a++) {
                    Vec3 dV = sT.direction.normalize().add(rand.nextGaussian() * 0.0075F * angle, rand.nextGaussian() * 0.0075F * angle,
                            rand.nextGaussian() * 0.0075F * angle);
                    tT.add(new Trajectory(sT.source, dV.normalize()));
                }
            }
        }
        return tT.toArray(new Trajectory[0]);
    }

    @Override
    public float getPowerMultiplier() {
        return 1.0F / (this.getSettingValue("forks") / 2.0F);
    }

    @Override
    public boolean execute() {
        return true;
    }
}
