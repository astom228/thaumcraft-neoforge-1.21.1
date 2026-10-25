package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Центрифуга (порт TileCentrifuge): снизу всасывает по единице составной эссенции (64, пустая — 128), за 2 секунды
 * раскладывает её на один из двух компонентов и отдаёт его наверх. Сигнал красного камня останавливает её.
 */
public class TileCentrifuge extends TileThaumcraft implements IAspectContainer, IEssentiaTransport {
    public Aspect aspectOut = null;
    public Aspect aspectIn = null;
    int count = 0;
    int process = 0;
    float rotationSpeed = 0.0F;
    public float rotation = 0.0F;

    public TileCentrifuge(BlockPos pos, BlockState state) {
        super(ConfigBlocks.CENTRIFUGE_TILE.get(), pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.aspectIn = Aspect.getAspect(nbt.getString("aspectIn"));
        this.aspectOut = Aspect.getAspect(nbt.getString("aspectOut"));
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        if (this.aspectIn != null) {
            nbt.putString("aspectIn", this.aspectIn.getTag());
        }
        if (this.aspectOut != null) {
            nbt.putString("aspectOut", this.aspectOut.getTag());
        }
        return nbt;
    }

    /** markAndNotifyBlock оригинала: сохранить, разослать клиентам, обновить соседей (компараторы). */
    private void notifyChange() {
        this.setChanged();
        this.syncTile(false);
        if (this.level != null) {
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
        }
    }

    @Override
    public AspectList getAspects() {
        AspectList al = new AspectList();
        if (this.aspectOut != null) {
            al.add(this.aspectOut, 1);
        }
        return al;
    }

    @Override
    public int addToContainer(Aspect tt, int am) {
        if (am > 0 && this.aspectOut == null) {
            this.aspectOut = tt;
            this.notifyChange();
            am--;
        }
        return am;
    }

    @Override
    public boolean takeFromContainer(Aspect tt, int am) {
        if (this.aspectOut != null && tt == this.aspectOut) {
            this.aspectOut = null;
            this.notifyChange();
            return true;
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amt) {
        return amt == 1 && tag == this.aspectOut;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        for (Aspect tt : ot.getAspects()) {
            if (tt == this.aspectOut) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return tag == this.aspectOut ? 1 : 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face == Direction.UP || face == Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face == Direction.UP;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return face == Direction.DOWN ? (this.gettingPower() ? 0 : (this.aspectIn == null ? 128 : 64)) : 0;
    }

    @Override
    public Aspect getEssentiaType(Direction loc) {
        return this.aspectOut;
    }

    @Override
    public int getEssentiaAmount(Direction loc) {
        return this.aspectOut != null ? 1 : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return this.canOutputTo(face) && this.takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (this.aspectIn == null && !aspect.isPrimal()) {
            this.aspectIn = aspect;
            this.process = 39;
            this.notifyChange();
            return 1;
        }
        return 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileCentrifuge tile) {
        tile.update();
    }

    void update() {
        if (!this.level.isClientSide) {
            if (!this.gettingPower()) {
                if (this.aspectOut == null && this.aspectIn == null && ++this.count % 5 == 0) {
                    this.drawEssentia();
                }
                if (this.process > 0) {
                    this.process--;
                }
                if (this.aspectOut == null && this.aspectIn != null && this.process == 0) {
                    this.processEssentia();
                }
            }
        } else {
            if (this.aspectIn != null && !this.gettingPower() && this.rotationSpeed < 20.0F) {
                this.rotationSpeed += 2.0F;
            }
            if ((this.aspectIn == null || this.gettingPower()) && this.rotationSpeed > 0.0F) {
                this.rotationSpeed -= 0.5F;
            }
            int pr = (int) this.rotation;
            this.rotation = this.rotation + this.rotationSpeed;
            if (this.rotation % 180.0F <= 20.0F && pr % 180 >= 160 && this.rotationSpeed > 0.0F) {
                this.level.playLocalSound(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5,
                        SoundsTC.pump.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
        }
    }

    void processEssentia() {
        Aspect[] comps = this.aspectIn.getComponents();
        this.aspectOut = comps[this.level.random.nextInt(2)];
        this.aspectIn = null;
        this.notifyChange();
    }

    void drawEssentia() {
        BlockEntity te = ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition, Direction.DOWN);
        if (te != null) {
            IEssentiaTransport ic = (IEssentiaTransport) te;
            if (!ic.canOutputTo(Direction.UP)) {
                return;
            }
            Aspect ta = null;
            if (ic.getEssentiaAmount(Direction.UP) > 0 && ic.getSuctionAmount(Direction.UP) < this.getSuctionAmount(Direction.DOWN)
                    && this.getSuctionAmount(Direction.DOWN) >= ic.getMinimumSuction()) {
                ta = ic.getEssentiaType(Direction.UP);
            }
            if (ta != null && !ta.isPrimal() && ic.getSuctionAmount(Direction.UP) < this.getSuctionAmount(Direction.DOWN)
                    && ic.takeEssentia(ta, 1, Direction.UP) == 1) {
                this.aspectIn = ta;
                this.process = 39;
                this.notifyChange();
            }
        }
    }

    @Override
    public void setAspects(AspectList aspects) {
    }
}
