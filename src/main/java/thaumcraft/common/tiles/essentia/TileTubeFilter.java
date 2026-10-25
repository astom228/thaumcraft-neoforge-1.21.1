package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.config.ConfigBlocks;

/** Фильтрующая трубка: всасывает только аспект фильтра (задаётся фиалом/кристаллом/банкой с эссенцией, снимается с Shift). */
public class TileTubeFilter extends TileTube implements IAspectContainer {
    public Aspect aspectFilter = null;

    public TileTubeFilter(BlockPos pos, BlockState state) {
        super(ConfigBlocks.TUBE_FILTER_TILE.get(), pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        super.readSyncNBT(nbttagcompound, registries);
        this.aspectFilter = Aspect.getAspect(nbttagcompound.getString("AspectFilter"));
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        nbttagcompound = super.writeSyncNBT(nbttagcompound, registries);
        if (this.aspectFilter != null) {
            nbttagcompound.putString("AspectFilter", this.aspectFilter.getTag());
        }
        return nbttagcompound;
    }

    @Override
    void calculateSuction(Aspect filter, boolean restrict, boolean dir) {
        super.calculateSuction(this.aspectFilter, restrict, dir);
    }

    @Override
    public AspectList getAspects() {
        return this.aspectFilter != null ? new AspectList().add(this.aspectFilter, -1) : null;
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return false;
    }

    @Override
    public int addToContainer(Aspect tag, int amount) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return 0;
    }
}
