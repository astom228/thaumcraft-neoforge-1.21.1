package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.devices.BlockCondenser;
import thaumcraft.common.blocks.devices.BlockCondenserLattice;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Конденсатор Выброса (порт TileCondenser): втягивает эссенцию сбоку (до 100) и, тратя её, переводит Выброс ауры в эссенцию
 * Выброса (до 100), которую отдаёт вниз. Скорость задаёт решётка над ним: чем больше решёток (не засорённых, с меньшим числом
 * соседей), тем короче интервал; цена — 10 + ¼ числа решёток. Иногда решётка засоряется. Без сигнала редстоуна.
 */
public class TileCondenser extends TileThaumcraft implements IEssentiaTransport {
    private int essentia;
    private int flux;
    private static final int MAX = 100;
    private int count = 0;
    private final Set<Long> history = new HashSet<>();
    private final List<Long> blockList = new ArrayList<>();
    private final List<Long> uncloggedList = new ArrayList<>();
    public float latticeCount = -1.0F;
    public int interval = 0;
    public int cost = 0;

    public TileCondenser(BlockPos pos, BlockState state) {
        super(ConfigBlocks.CONDENSER_TILE.get(), pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.essentia = nbt.getShort("essentia");
        this.flux = nbt.getShort("flux");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putShort("essentia", (short) this.essentia);
        nbt.putShort("flux", (short) this.flux);
        return nbt;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileCondenser tile) {
        if (tile.latticeCount < 0.0F) {
            tile.triggerCheck();
        }
        tile.count++;
        if (!state.getValue(BlockCondenser.ENABLED) || !(tile.latticeCount > 0.0F)) {
            return;
        }
        if (level.isClientSide) {
            if (tile.essentia > 0 && !tile.uncloggedList.isEmpty() && tile.count % Math.max(3, tile.interval / 50) == 0) {
                BlockPos p = BlockPos.of(tile.uncloggedList.get(level.random.nextInt(tile.uncloggedList.size())));
                IFXDispatcher.get().spark(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4.5F + level.random.nextFloat(),
                        0.33F + level.random.nextFloat() * 0.66F, 0.33F + level.random.nextFloat() * 0.66F, 0.33F + level.random.nextFloat() * 0.66F, 0.8F);
            }
            return;
        }
        if (tile.count % 5 == 0 && tile.essentia < MAX) {
            tile.fill();
        }
        if (tile.interval > 0 && tile.essentia >= tile.cost && tile.flux < MAX && tile.count % tile.interval == 0
                && AuraHelper.getFlux(level, pos) >= 1.0F) {
            AuraHelper.drainFlux(level, pos, 1.0F, false);
            tile.essentia = tile.essentia - tile.cost;
            tile.flux++;
            if (level.random.nextInt(50) == 0) {
                tile.makeLatticeDirty();
            }
            tile.syncTile(false);
            tile.setChanged();
        }
    }

    private void makeLatticeDirty() {
        if (!this.uncloggedList.isEmpty()) {
            int q = this.level.random.nextInt(this.uncloggedList.size());
            if (q == 0) {
                q = this.level.random.nextInt(this.uncloggedList.size());
            }
            BlockPos p = BlockPos.of(this.uncloggedList.get(q));
            BlockState bs = this.level.getBlockState(p);
            if (bs.getBlock() == BlocksTC.condenserlattice.get()) {
                BlockState dirty = BlocksTC.condenserlatticeDirty.get().defaultBlockState();
                for (var prop : List.of(BlockCondenserLattice.NORTH, BlockCondenserLattice.EAST, BlockCondenserLattice.SOUTH, BlockCondenserLattice.WEST,
                        BlockCondenserLattice.UP, BlockCondenserLattice.DOWN)) {
                    dirty = dirty.setValue(prop, bs.getValue(prop));
                }
                this.level.setBlock(p, dirty, 3);
                BlockCondenserLattice.triggerUpdate(this.level, p);
            }
        }
    }

    private void fill() {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            BlockEntity te = ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition, face);
            if (te instanceof IEssentiaTransport ic) {
                if (!ic.canOutputTo(face.getOpposite())) {
                    return;
                }
                Aspect ta = null;
                if (ic.getEssentiaAmount(face.getOpposite()) > 0 && ic.getSuctionAmount(face.getOpposite()) < this.getSuctionAmount(face)
                        && this.getSuctionAmount(face) >= ic.getMinimumSuction()) {
                    ta = ic.getEssentiaType(face.getOpposite());
                }
                if (ta != null) {
                    if (ta != Aspect.FLUX) {
                        this.essentia = this.essentia + ic.takeEssentia(ta, 1, face.getOpposite());
                    } else {
                        this.makeLatticeDirty();
                    }
                    this.syncTile(false);
                    this.setChanged();
                    if (this.essentia >= MAX) {
                        return;
                    }
                }
            }
        }
    }

    public void triggerCheck() {
        this.history.clear();
        this.blockList.clear();
        this.uncloggedList.clear();
        this.latticeCount = 0.0F;
        this.interval = 0;
        this.performCheck(this.worldPosition, true, false);
        this.history.clear();
        if (this.latticeCount <= 0.0F) {
            this.latticeCount = 0.0F;
        } else {
            if (this.latticeCount > 40.0F) {
                this.latticeCount = 40.0F;
            }
            this.interval = Math.round(600.0F - this.latticeCount * 15.0F);
            if (this.interval < 5) {
                this.interval = 5;
            }
            this.cost = (int) (10.0F + this.blockList.size() * 0.25F);
        }
    }

    private void performCheck(BlockPos pos, boolean skip, boolean clogged) {
        if (this.latticeCount < 0.0F) {
            return;
        }
        this.history.add(pos.asLong());
        boolean found = false;
        int sides = 0;
        for (Direction face : Direction.values()) {
            if (!skip || face == Direction.UP) {
                BlockPos p2 = pos.relative(face);
                if (!this.level.isLoaded(p2)) {
                    continue;
                }
                BlockState bs = this.level.getBlockState(p2);
                boolean lattice = bs.getBlock() == BlocksTC.condenserlattice.get();
                boolean latticeDirty = bs.getBlock() == BlocksTC.condenserlatticeDirty.get();
                if (lattice || latticeDirty) {
                    sides++;
                }
                if (!this.history.contains(p2.asLong())) {
                    if (face == Direction.DOWN && bs.getBlock() == BlocksTC.condenser.get()) {
                        this.latticeCount = -99.0F;
                        return;
                    }
                    if (this.worldPosition.getY() < p2.getY() && !(this.worldPosition.distSqr(p2) > 74.0) && (lattice || latticeDirty)) {
                        this.blockList.add(p2.asLong());
                        if (lattice) {
                            this.uncloggedList.add(p2.asLong());
                        }
                        found = true;
                        this.performCheck(p2, false, clogged || latticeDirty);
                        if (skip) {
                            break;
                        }
                    }
                }
            }
        }
        if (found && !clogged) {
            this.latticeCount += 1.0F - 0.15F * sides;
        }
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.UP;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != Direction.UP && face != Direction.DOWN;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return face == Direction.DOWN ? 0 : 64;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        int amt = !this.canOutputTo(face) || aspect != null && aspect != Aspect.FLUX ? 0 : Math.min(amount, this.flux);
        if (amt > 0) {
            this.flux -= amt;
            this.syncTile(false);
            this.setChanged();
        }
        return amt;
    }

    /** В оригинале принятое количество не прибавлялось (эссенция пропадала) — здесь прибавляется (DEVIATIONS 48). */
    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        int amt = this.canInputFrom(face) ? Math.min(amount, MAX - this.essentia) : 0;
        if (amt > 0) {
            this.essentia += amt;
            this.syncTile(false);
            this.setChanged();
        }
        return amt;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return Aspect.FLUX;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return this.flux;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }
}
