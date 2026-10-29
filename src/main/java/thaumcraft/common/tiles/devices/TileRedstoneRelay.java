package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraft;

/** Редстоун-реле (порт TileRedstoneRelay): порог включения in (1–15) и сила выхода out (1–15), меняются щелчком по ручкам. */
public class TileRedstoneRelay extends TileThaumcraft {
    private int in = 1;
    private int out = 15;

    public TileRedstoneRelay(BlockPos pos, BlockState state) {
        super(ConfigBlocks.REDSTONE_RELAY_TILE.get(), pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.in = Math.max(1, Math.min(15, nbt.getByte("in")));
        this.out = Math.max(1, Math.min(15, nbt.getByte("out")));
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putByte("in", (byte) this.in);
        nbt.putByte("out", (byte) this.out);
        return nbt;
    }

    public void increaseIn() {
        if (this.level != null && !this.level.isClientSide) {
            this.in = this.in >= 15 ? 1 : this.in + 1;
            this.setChanged();
            this.syncTile(false);
        }
    }

    public void increaseOut() {
        if (this.level != null && !this.level.isClientSide) {
            this.out = this.out >= 15 ? 1 : this.out + 1;
            this.setChanged();
            this.syncTile(false);
        }
    }

    public int getIn() {
        return this.in;
    }

    public int getOut() {
        return this.out;
    }
}
