package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Неиссякаемая урна (порт TileWaterJug): набирает воду из Вис ауры (до 1000), раз в 5 тиков обходит по одной клетке 5×3×5 вокруг,
 * запоминает приёмники жидкости и наливает им по 25 воды (с видимой струёй). Сверху из неё можно сливать воду трубами,
 * налить в неё нельзя.
 */
public class TileWaterJug extends TileThaumcraft {
    int zone = 0;
    int counter = 0;
    List<Integer> handlers = new ArrayList<>();
    int zc = 0;
    int tcount = 0;
    public final FluidTank tank = new FluidTank(1000);
    /**
     * Возможность жидкости сверху. В оригинале отдавался сам бак: трубы могли налить в урну (хотя fill урны возвращал 0), а слив
     * трубой не помечал блок и не отправлялся клиентам (DEVIATIONS.md).
     */
    public final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return TileWaterJug.this.tank.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int t) {
            return TileWaterJug.this.tank.getFluidInTank(t);
        }

        @Override
        public int getTankCapacity(int t) {
            return TileWaterJug.this.tank.getTankCapacity(t);
        }

        @Override
        public boolean isFluidValid(int t, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return TileWaterJug.this.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return TileWaterJug.this.drain(maxDrain, action);
        }
    };

    public TileWaterJug(BlockPos pos, BlockState state) {
        super(ConfigBlocks.EVERFULL_URN_TILE.get(), pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        this.tank.readFromNBT(registries, nbttagcompound);
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        this.tank.writeToNBT(registries, nbttagcompound);
        return nbttagcompound;
    }

    // В оригинале список приёмников записывался в NBT пустым (значения не добавлялись) — после загрузки они находятся заново.

    public static void tick(Level level, BlockPos pos, BlockState state, TileWaterJug te) {
        te.update();
    }

    private BlockPos zonePos(int zz) {
        int x = zz / 5 % 5;
        int y = zz / 5 / 5 % 3;
        int z = zz % 5;
        return this.worldPosition.offset(x - 2, y - 1, z - 2);
    }

    private IFluidHandler handlerAt(BlockPos p) {
        if (!this.level.isLoaded(p)) {
            return null;
        }
        return this.level.getCapability(Capabilities.FluidHandler.BLOCK, p, null);
    }

    public void update() {
        this.counter++;
        if (this.level.isClientSide) {
            if (this.tcount > 0) {
                if (this.tcount % 5 == 0) {
                    int x = this.zc / 5 % 5;
                    int y = this.zc / 5 / 5 % 3;
                    int z = this.zc % 5;
                    IFXDispatcher.get().waterTrailFx(this.getBlockPos(), this.getBlockPos().offset(x - 2, y - 1, z - 2), this.counter, 2650102, 0.1F);
                }
                this.tcount--;
            }
        } else if (this.counter % 5 == 0) {
            this.zone++;
            BlockPos zp = this.zonePos(this.zone);
            // TODO: алтарь лепестков Botania (IPetalApothecary) — когда будет совместимость с Botania (необязательная зависимость)
            if (!zp.equals(this.worldPosition) && this.handlerAt(zp) != null && !this.handlers.contains(this.zone)) {
                this.handlers.add(this.zone);
                this.setChanged();
            }
            int i = 0;
            while (i < this.handlers.size() && this.tank.getFluidAmount() >= 25) {
                int zz = this.handlers.get(i);
                BlockPos hp = this.zonePos(zz);
                IFluidHandler fh = hp.equals(this.worldPosition) ? null : this.handlerAt(hp);
                if (fh != null) {
                    int q = fh.fill(new FluidStack(Fluids.WATER, 25), IFluidHandler.FluidAction.EXECUTE);
                    if (q > 0) {
                        this.drain(new FluidStack(Fluids.WATER, q), IFluidHandler.FluidAction.EXECUTE);
                        this.setChanged();
                        this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, zz);
                        break;
                    }
                } else {
                    this.handlers.remove(i);
                    this.setChanged();
                    continue;
                }
                i++;
            }
            if (this.tank.getFluidAmount() < 1000) {
                float da = (1000 - this.tank.getFluidAmount()) / 1000.0F;
                if (da > 0.1F) {
                    da = 0.1F;
                }
                float dv = AuraHelper.drainVis(this.getLevel(), this.getBlockPos(), da, false);
                int wa = (int) (1000.0F * dv);
                if (wa > 0) {
                    this.tank.fill(new FluidStack(Fluids.WATER, wa), IFluidHandler.FluidAction.EXECUTE);
                    this.setChanged();
                    if (this.tank.getFluidAmount() >= this.tank.getCapacity()) {
                        this.syncTile(false);
                    }
                }
            }
        }
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == 1) {
            if (this.level.isClientSide) {
                this.zc = j;
                this.tcount = 5;
            }
            return true;
        }
        return super.triggerEvent(i, j);
    }

    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
        boolean f = this.tank.getFluidAmount() >= this.tank.getCapacity();
        FluidStack fs = this.tank.drain(resource, action);
        if (action.execute()) {
            this.setChanged();
            if (f && this.tank.getFluidAmount() < this.tank.getCapacity()) {
                this.syncTile(false);
            }
        }
        return fs;
    }

    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        boolean f = this.tank.getFluidAmount() >= this.tank.getCapacity();
        FluidStack fs = this.tank.drain(maxDrain, action);
        if (action.execute()) {
            this.setChanged();
            if (f && this.tank.getFluidAmount() < this.tank.getCapacity()) {
                this.syncTile(false);
            }
        }
        return fs;
    }

    /** Возможность жидкости урны: только сверху (как в оригинале). */
    public static IFluidHandler capability(TileWaterJug tile, net.minecraft.core.Direction side) {
        return side == net.minecraft.core.Direction.UP ? tile.handler : null;
    }
}
