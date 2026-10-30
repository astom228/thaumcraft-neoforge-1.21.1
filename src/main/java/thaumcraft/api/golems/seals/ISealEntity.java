package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/** Поставленная печать (порт ISealEntity): позиция, приоритет, зона, замок, красный камень, владелец, цвет. */
public interface ISealEntity {
    void tickSealEntity(Level world);

    ISeal getSeal();

    SealPos getSealPos();

    byte getPriority();

    void setPriority(byte priority);

    void readNBT(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries);

    CompoundTag writeNBT(net.minecraft.core.HolderLookup.Provider registries);

    void syncToClient(Level world);

    BlockPos getArea();

    void setArea(BlockPos area);

    boolean isLocked();

    void setLocked(boolean locked);

    boolean isRedstoneSensitive();

    void setRedstoneSensitive(boolean sensitive);

    String getOwner();

    void setOwner(String owner);

    byte getColor();

    void setColor(byte color);

    boolean isStoppedByRedstone(Level world);
}
