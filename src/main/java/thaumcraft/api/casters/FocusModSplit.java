package thaumcraft.api.casters;

import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Разделяющий модификатор (порт FocusModSplit): после него — несколько веток-пакетов. Сила ×0.75. */
public abstract class FocusModSplit extends FocusMod {
    private final ArrayList<FocusPackage> packages = new ArrayList<>();

    public final ArrayList<FocusPackage> getSplitPackages() {
        return this.packages;
    }

    public void deserialize(CompoundTag nbt) {
        ListTag nodelist = nbt.getList("packages", Tag.TAG_COMPOUND);
        this.packages.clear();
        for (int x = 0; x < nodelist.size(); x++) {
            FocusPackage fp = new FocusPackage();
            fp.deserialize(nodelist.getCompound(x));
            this.packages.add(fp);
        }
    }

    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        ListTag nodelist = new ListTag();
        for (FocusPackage node : this.packages) {
            nodelist.add(node.serialize());
        }
        nbt.put("packages", nodelist);
        return nbt;
    }

    @Override
    public float getPowerMultiplier() {
        return 0.75F;
    }
}
