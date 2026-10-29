package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import thaumcraft.common.blocks.devices.BlockTCDeviceFacing;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Вис-генератор (порт TileVisGenerator): без сигнала редстоуна, опустев, берёт из ауры 1 Вис и превращает в 1000 FE;
 * отдаёт по 10 FE за тик в хранилище энергии перед выходом. Возможность энергии — только со стороны выхода, только отдача.
 */
public class TileVisGenerator extends TileThaumcraft implements IEnergyStorage {
    protected int energy;
    protected final int capacity = 1000;
    protected final int maxExtract = 10;

    public TileVisGenerator(BlockPos pos, BlockState state) {
        super(ConfigBlocks.VIS_GENERATOR_TILE.get(), pos, state);
    }

    public Direction facing() {
        return this.getBlockState().getValue(BlockTCDeviceFacing.FACING);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileVisGenerator tile) {
        if (!state.getValue(BlockTCDeviceFacing.ENABLED)) {
            return;
        }
        tile.recharge();
        Direction face = state.getValue(BlockTCDeviceFacing.FACING);
        if (!level.isLoaded(pos.relative(face))) {
            return;
        }
        IEnergyStorage capability = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(face), face.getOpposite());
        if (capability != null && capability.canReceive()) {
            int energyExtracted = Math.min(tile.energy, 10);
            energyExtracted = capability.receiveEnergy(energyExtracted, false);
            if (energyExtracted > 0) {
                tile.energy -= energyExtracted;
                tile.setChanged();
                if (tile.energy == 0) {
                    tile.syncTile(false);
                }
            }
        }
    }

    private void recharge() {
        if (this.energy == 0) {
            float vis = AuraHandler.drainVis(this.level, this.worldPosition, 1.0F, false);
            this.energy = (int) (vis * 1000.0F);
            this.setChanged();
            this.syncTile(false);
        }
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.energy = nbt.getInt("energy");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putInt("energy", this.energy);
        return nbt;
    }

    public static IEnergyStorage capability(TileVisGenerator tile, Direction side) {
        return side == tile.facing() ? tile : null;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return this.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return 1000;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
