package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Алембик (порт TileAlembic): до 128 эссенции одного аспекта от плавильни снизу; ярлык (facing — его грань) задаёт фильтр.
 * Отдаёт эссенцию трубам по бокам и сверху (кроме грани с ярлыком), сам не всасывает.
 */
public class TileAlembic extends TileThaumcraft implements IAspectContainer, IEssentiaTransport {
    public Aspect aspect;
    public Aspect aspectFilter = null;
    public int amount = 0;
    public int maxAmount = 128;
    public int facing = Direction.DOWN.ordinal();

    public TileAlembic(BlockPos pos, BlockState state) {
        super(ConfigBlocks.ALEMBIC_TILE.get(), pos, state);
    }

    @Override
    public AspectList getAspects() {
        return this.aspect != null ? new AspectList().add(this.aspect, this.amount) : new AspectList();
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.facing = nbt.getByte("facing");
        this.aspectFilter = Aspect.getAspect(nbt.getString("AspectFilter"));
        this.aspect = Aspect.getAspect(nbt.getString("aspect"));
        this.amount = nbt.getShort("amount");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        if (this.aspect != null) {
            nbt.putString("aspect", this.aspect.getTag());
        }
        if (this.aspectFilter != null) {
            nbt.putString("AspectFilter", this.aspectFilter.getTag());
        }
        nbt.putShort("amount", (short) this.amount);
        nbt.putByte("facing", (byte) this.facing);
        return nbt;
    }

    @Override
    public int addToContainer(Aspect tt, int am) {
        if (tt == null || am <= 0) return am;
        if (this.aspectFilter != null && tt != this.aspectFilter) {
            return am;
        }
        if (this.amount < this.maxAmount && tt == this.aspect || this.amount == 0) {
            this.aspect = tt;
            int added = Math.min(am, this.maxAmount - this.amount);
            this.amount += added;
            am -= added;
        }
        this.setChanged();
        this.syncTile(false);
        return am;
    }

    @Override
    public boolean takeFromContainer(Aspect tt, int am) {
        if (tt == null || am <= 0) return false;
        if (this.amount == 0 || this.aspect == null) {
            this.aspect = null;
            this.amount = 0;
        }
        if (this.aspect != null && this.amount >= am && tt == this.aspect) {
            this.amount -= am;
            if (this.amount <= 0) {
                this.aspect = null;
                this.amount = 0;
            }
            this.setChanged();
            this.syncTile(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        return this.amount > 0 && this.aspect != null && ot.getAmount(this.aspect) > 0;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tt, int am) {
        return this.amount >= am && tt == this.aspect;
    }

    @Override
    public int containerContains(Aspect tt) {
        return tt == this.aspect ? this.amount : 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.values()[this.facing] && face != Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return false;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face != Direction.values()[this.facing] && face != Direction.DOWN;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(Direction loc) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction loc) {
        return 0;
    }

    @Override
    public Aspect getEssentiaType(Direction loc) {
        return this.aspect;
    }

    @Override
    public int getEssentiaAmount(Direction loc) {
        return this.amount;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return this.canOutputTo(face) && this.takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction loc) {
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    /**
     * Отдать единицу эссенции стопке алембиков над pos: сначала тому, где уже есть этот аспект, затем первому подходящему
     * (по фильтру) со свободным местом. Позиции над плавильней — в том же столбце чанка, он загружен.
     */
    static boolean processAlembics(Level world, BlockPos pos, Aspect aspect) {
        int deep = 1;
        while (true) {
            BlockEntity te = world.getBlockEntity(pos.above(deep));
            if (!(te instanceof TileAlembic alembic)) {
                break;
            }
            if (alembic.amount > 0 && alembic.aspect == aspect && alembic.addToContainer(aspect, 1) == 0) {
                return true;
            }
            deep++;
        }
        deep = 1;
        while (true) {
            BlockEntity te = world.getBlockEntity(pos.above(deep));
            if (!(te instanceof TileAlembic alembic)) {
                return false;
            }
            if ((alembic.aspectFilter == null || alembic.aspectFilter == aspect) && alembic.addToContainer(aspect, 1) == 0) {
                return true;
            }
            deep++;
        }
    }
}
