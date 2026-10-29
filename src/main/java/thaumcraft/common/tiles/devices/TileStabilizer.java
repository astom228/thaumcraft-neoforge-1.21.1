package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.IEnergyStorage;
import thaumcraft.api.crafting.IStabilizable;
import thaumcraft.common.blocks.devices.BlockTCDeviceFacing;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Стабилизатор (порт TileStabilizer): принимает FE с задней стороны (до 100 за раз, запас 1000). Без сигнала, раз в 5 тиков,
 * при 100+ FE ищет перед собой (до 8 блоков) стабилизируемое — сначала сущность, затем блок-сущность — и добавляет стабильность
 * по 1 за каждые 100 FE; не нашёл — пауза 20 тиков. Частицы — по запасу энергии и при передаче (событие блока).
 */
public class TileStabilizer extends TileThaumcraft implements IEnergyStorage {
    private int ticks = 0;
    private int delay = 0;
    protected int energy;

    public TileStabilizer(BlockPos pos, BlockState state) {
        super(ConfigBlocks.STABILIZER_TILE.get(), pos, state);
    }

    private Direction facing() {
        return this.getBlockState().getValue(BlockTCDeviceFacing.FACING);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileStabilizer tile) {
        if (!state.getValue(BlockTCDeviceFacing.ENABLED)) {
            return;
        }
        if (!level.isClientSide) {
            tile.ticks++;
            if (tile.delay <= 0 && tile.ticks % 5 == 0 && tile.energy >= 100) {
                tile.tryAddStability();
            }
            if (tile.delay > 0) {
                tile.delay--;
            }
        } else if (level.random.nextInt(1000) < tile.energy) {
            Direction facing = state.getValue(BlockTCDeviceFacing.FACING);
            double x = pos.getX() + 0.5F + level.random.nextGaussian() * 0.05F;
            double y = pos.getY() + 0.5F + level.random.nextGaussian() * 0.05F;
            double z = pos.getZ() + 0.5F + level.random.nextGaussian() * 0.05F;
            IFXDispatcher.get().drawStabilizerParticles(x, y, z, facing.getStepX() / 50.0, facing.getStepY() / 50.0, facing.getStepZ() / 50.0, 50);
        }
    }

    private void tryAddStability() {
        int m = this.energy / 100;
        boolean flag = false;
        int d = 0;
        Direction facing = this.facing();
        BlockPos pos = this.worldPosition;
        AABB box = new AABB(pos.getX() - (facing.getStepX() < 0 ? 8 : 0), pos.getY() - (facing.getStepY() < 0 ? 8 : 0),
                pos.getZ() - (facing.getStepZ() < 0 ? 8 : 0), pos.getX() + 1 + (facing.getStepX() > 0 ? 8 : 0),
                pos.getY() + 1 + (facing.getStepY() > 0 ? 8 : 0), pos.getZ() + 1 + (facing.getStepZ() > 0 ? 8 : 0));
        for (Entity e : this.level.getEntities((Entity) null, box, e -> e instanceof IStabilizable)) {
            if (e.isAlive()) {
                IStabilizable s = (IStabilizable) e;
                if (s.getStability() != IStabilizable.EnumStability.VERY_STABLE) {
                    s.addStability(m);
                    flag = true;
                    d = (int) Math.sqrt(e.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
                }
                break;
            }
        }
        if (!flag) {
            BlockPos p = pos.relative(facing);
            for (int a = 0; a < 8; a++) {
                if (!this.level.isLoaded(p)) {
                    break;
                }
                BlockEntity t = this.level.getBlockEntity(p);
                if (t instanceof IStabilizable s) {
                    if (s.getStability() != IStabilizable.EnumStability.VERY_STABLE) {
                        s.addStability(m);
                        flag = true;
                        d = a + 1;
                    }
                    break;
                }
                p = p.relative(facing);
            }
        }
        if (flag) {
            this.energy -= 100 * m;
            this.setChanged();
            this.syncTile(false);
            this.level.blockEvent(pos, this.getBlockState().getBlock(), m, d - 1);
        } else {
            this.delay = 20;
        }
    }

    /** Событие блока m (сколько стабильности), j — расстояние − 1: завитки от задней стороны к цели (клиент). */
    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == 0) {
            return super.triggerEvent(i, j);
        }
        if (this.level != null && this.level.isClientSide) {
            Direction facing = this.facing();
            var r = this.level.random;
            BlockPos pos = this.worldPosition;
            for (int a = 0; a < 3 * i; a++) {
                double x = pos.getX() - facing.getStepX() / 1.6F + 0.5F + r.nextGaussian() * 0.025F;
                double y = pos.getY() - facing.getStepY() / 1.6F + 0.5F + r.nextGaussian() * 0.025F;
                double z = pos.getZ() - facing.getStepZ() / 1.6F + 0.5F + r.nextGaussian() * 0.025F;
                IFXDispatcher.get().drawCurlyWisp(x, y, z, facing.getStepX() / 25.0F * j + r.nextGaussian() * 0.0033F,
                        facing.getStepY() / 25.0F * j + r.nextGaussian() * 0.0033F, facing.getStepZ() / 25.0F * j + r.nextGaussian() * 0.0033F,
                        0.5F, 1.0F, 0.5F, 1.0F, 0.5F, null, 1, 0, a % 3 * 2);
                x = pos.getX() - facing.getStepX() / 1.6F + 0.5F + r.nextGaussian() * 0.025F;
                y = pos.getY() - facing.getStepY() / 1.6F + 0.5F + r.nextGaussian() * 0.025F;
                z = pos.getZ() - facing.getStepZ() / 1.6F + 0.5F + r.nextGaussian() * 0.025F;
                IFXDispatcher.get().drawCurlyWisp(x, y, z, facing.getStepX() / 25.0F * j + r.nextGaussian() * 0.002F,
                        facing.getStepY() / 25.0F * j + r.nextGaussian() * 0.002F, facing.getStepZ() / 25.0F * j + r.nextGaussian() * 0.002F,
                        0.25F, r.nextFloat(), r.nextFloat(), r.nextFloat(), 0.5F, null, 1, 0, 1 + a % 3 * 2);
            }
        }
        return true;
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

    /** Возможность энергии — только с задней стороны (противоположной выходу). */
    public static IEnergyStorage capability(TileStabilizer tile, Direction side) {
        return side == tile.facing().getOpposite() ? tile : null;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energyReceived = Math.min(1000 - this.energy, Math.min(100, maxReceive));
        if (!simulate && energyReceived > 0) {
            this.energy += energyReceived;
            this.setChanged();
        }
        return energyReceived;
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
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
