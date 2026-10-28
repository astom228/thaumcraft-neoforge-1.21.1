package thaumcraft.api.casters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;

/** Узел фокуса (порт FocusNode): сложность, аспект, что требует и что даёт следующим узлам, настройки. */
public abstract class FocusNode implements IFocusElement {
    FocusPackage pack;
    private FocusNode parent;
    final HashMap<String, NodeSetting> settings = new HashMap<>();

    public FocusNode() {
        this.initialize();
    }

    public String getUnlocalizedName() {
        return this.getKey() + ".name";
    }

    public String getUnlocalizedText() {
        return this.getKey() + ".text";
    }

    public abstract int getComplexity();

    public abstract Aspect getAspect();

    public abstract EnumSupplyType[] mustBeSupplied();

    public abstract EnumSupplyType[] willSupply();

    public boolean canSupply(EnumSupplyType type) {
        if (this.willSupply() != null) {
            for (EnumSupplyType st : this.willSupply()) {
                if (st == type) {
                    return true;
                }
            }
        }
        return false;
    }

    public HitResult[] supplyTargets() {
        return null;
    }

    public Trajectory[] supplyTrajectories() {
        return null;
    }

    public final void setPackage(FocusPackage pack) {
        this.pack = pack;
    }

    public final FocusPackage getPackage() {
        return this.pack;
    }

    /** Часть пакета после этого узла (её выполняет промежуточная сущность — снаряд, облако, мина). */
    public final FocusPackage getRemainingPackage() {
        FocusPackage p = this.getPackage();
        List<IFocusElement> l = p.nodes.subList(p.index + 1, p.nodes.size());
        List<IFocusElement> l2 = Collections.synchronizedList(new ArrayList<>());
        l2.addAll(l);
        FocusPackage p2 = new FocusPackage();
        p2.setUniqueID(p.getUniqueID());
        p2.world = p.world;
        p2.multiplyPower(p.getPower());
        p2.nodes = l2;
        p2.setCasterUUID(p.getCasterUUID());
        return l2.isEmpty() ? null : p2;
    }

    public final FocusNode getParent() {
        return this.parent;
    }

    public final Set<String> getSettingList() {
        return this.settings.keySet();
    }

    public final NodeSetting getSetting(String key) {
        return this.settings.get(key);
    }

    public final int getSettingValue(String key) {
        return this.settings.containsKey(key) ? this.settings.get(key).getValue() : 0;
    }

    public NodeSetting[] createSettings() {
        return null;
    }

    public final void initialize() {
        NodeSetting[] set = this.createSettings();
        if (set != null) {
            for (NodeSetting setting : set) {
                this.settings.put(setting.key, setting);
            }
        }
    }

    public void setParent(FocusNode parent) {
        this.parent = parent;
    }

    public float getPowerMultiplier() {
        return 1.0F;
    }

    public enum EnumSupplyType {
        TARGET,
        TRAJECTORY
    }
}
