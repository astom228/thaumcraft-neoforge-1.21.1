package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Защищённая банка (порт TileJarFillable): до 250 эссенции одного аспекта, ярлык задаёт фильтр (всасывание 64 вместо 32),
 * скоба закрывает банку от устройств (isBlocked). Труба подключается только сверху; раз в 5 тиков банка тянет эссенцию из неё.
 */
public class TileJarFillable extends TileJar implements IAspectSource, IEssentiaTransport {
    public static final int CAPACITY = 250;
    public Aspect aspect = null;
    public Aspect aspectFilter = null;
    public int amount = 0;
    public int facing = 2;
    public boolean blocked = false;
    int count = 0;

    public TileJarFillable(BlockPos pos, BlockState state) {
        this(ConfigBlocks.WARDED_JAR_TILE.get(), pos, state);
    }

    protected TileJarFillable(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        this.aspect = Aspect.getAspect(nbttagcompound.getString("Aspect"));
        this.aspectFilter = Aspect.getAspect(nbttagcompound.getString("AspectFilter"));
        this.amount = nbttagcompound.getShort("Amount");
        this.facing = nbttagcompound.getByte("facing");
        this.blocked = nbttagcompound.getBoolean("blocked");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        if (this.aspect != null) {
            nbttagcompound.putString("Aspect", this.aspect.getTag());
        }
        if (this.aspectFilter != null) {
            nbttagcompound.putString("AspectFilter", this.aspectFilter.getTag());
        }
        nbttagcompound.putShort("Amount", (short) this.amount);
        nbttagcompound.putByte("facing", (byte) this.facing);
        nbttagcompound.putBoolean("blocked", this.blocked);
        return nbttagcompound;
    }

    /** Предмет-банка → блок (placeBlockAt BlockJarItem оригинала): эссенция и фильтр из компонентов предмета. */
    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.setAspects(input.get(TCDataComponents.ASPECTS.get()));
        String f = input.get(TCDataComponents.ASPECT_FILTER.get());
        if (f != null) {
            this.aspectFilter = Aspect.getAspect(f);
        }
    }

    /** Блок → предмет (выпадение, выбор средней кнопкой с Ctrl). */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.amount > 0 && this.aspect != null) {
            components.set(TCDataComponents.ASPECTS.get(), new AspectList().add(this.aspect, this.amount));
        }
        if (this.aspectFilter != null) {
            components.set(TCDataComponents.ASPECT_FILTER.get(), this.aspectFilter.getTag());
        }
    }

    @Override
    public AspectList getAspects() {
        AspectList al = new AspectList();
        if (this.aspect != null && this.amount > 0) {
            al.add(this.aspect, this.amount);
        }
        return al;
    }

    @Override
    public void setAspects(AspectList aspects) {
        if (aspects != null && aspects.size() > 0) {
            this.aspect = aspects.getAspectsSortedByAmount()[0];
            this.amount = aspects.getAmount(aspects.getAspectsSortedByAmount()[0]);
        }
    }

    @Override
    public int addToContainer(Aspect tt, int am) {
        if (am <= 0 || !this.doesContainerAccept(tt)) {
            return am;
        }
        if (this.amount < CAPACITY && tt == this.aspect || this.amount == 0) {
            this.aspect = tt;
            int added = Math.min(am, CAPACITY - this.amount);
            this.amount += added;
            am -= added;
        }
        this.syncTile(false);
        this.setChanged();
        return am;
    }

    @Override
    public boolean takeFromContainer(Aspect tt, int am) {
        if (am > 0 && tt != null && this.amount >= am && tt == this.aspect) {
            this.amount -= am;
            if (this.amount <= 0) {
                this.aspect = null;
                this.amount = 0;
            }
            this.syncTile(false);
            this.setChanged();
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
        return this.amount >= amt && tag == this.aspect;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        for (Aspect tt : ot.getAspects()) {
            if (this.amount > 0 && tt == this.aspect) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return tag == this.aspect ? this.amount : 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return tag != null && (this.aspectFilter == null || tag == this.aspectFilter);
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face == Direction.UP;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face == Direction.UP;
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
        return this.aspectFilter != null ? 64 : 32;
    }

    @Override
    public Aspect getSuctionType(Direction loc) {
        return this.aspectFilter != null ? this.aspectFilter : this.aspect;
    }

    @Override
    public int getSuctionAmount(Direction loc) {
        if (this.amount < CAPACITY) {
            return this.aspectFilter != null ? 64 : 32;
        }
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
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return this.canInputFrom(face) ? amount - this.addToContainer(aspect, amount) : 0;
    }

    @Override
    public void update() {
        if (!this.level.isClientSide && ++this.count % 5 == 0 && this.amount < CAPACITY) {
            this.fillJar();
        }
    }

    void fillJar() {
        BlockEntity te = ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition, Direction.UP);
        if (te != null) {
            IEssentiaTransport ic = (IEssentiaTransport) te;
            if (!ic.canOutputTo(Direction.DOWN)) {
                return;
            }
            Aspect ta = null;
            if (this.aspectFilter != null) {
                ta = this.aspectFilter;
            } else if (this.aspect != null && this.amount > 0) {
                ta = this.aspect;
            } else if (ic.getEssentiaAmount(Direction.DOWN) > 0 && ic.getSuctionAmount(Direction.DOWN) < this.getSuctionAmount(Direction.UP)
                    && this.getSuctionAmount(Direction.UP) >= ic.getMinimumSuction()) {
                ta = ic.getEssentiaType(Direction.DOWN);
            }
            if (ta != null && ic.getSuctionAmount(Direction.DOWN) < this.getSuctionAmount(Direction.UP)) {
                this.addToContainer(ta, ic.takeEssentia(ta, 1, Direction.DOWN));
            }
        }
    }

    @Override
    public boolean isBlocked() {
        return this.blocked;
    }
}
