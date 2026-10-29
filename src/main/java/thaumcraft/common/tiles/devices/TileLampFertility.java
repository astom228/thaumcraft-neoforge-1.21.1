package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.devices.BlockTCDeviceFacing;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Лампа плодородия (порт TileLampFertility): копит до 10 зарядов Cupiditas; без сигнала раз в 15 с ищет в радиусе 7 двух взрослых
 * свободных животных одного вида (если их не больше 9) и вводит их в «любовное состояние» за 5 зарядов.
 */
public class TileLampFertility extends TileThaumcraft implements IEssentiaTransport {
    public int charges = 0;
    private int count = 0;
    private int drawDelay = 0;

    public TileLampFertility(BlockPos pos, BlockState state) {
        super(ConfigBlocks.LAMP_FERTILITY_TILE.get(), pos, state);
    }

    private Direction facing() {
        return this.getBlockState().getValue(BlockTCDeviceFacing.FACING);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileLampFertility tile) {
        if (tile.charges < 10) {
            if (tile.drawEssentia()) {
                tile.charges++;
                tile.setChanged();
                tile.syncTile(true);
            }
            BlockState st = level.getBlockState(pos);
            if (tile.charges <= 1) {
                if (st.getValue(BlockTCDeviceFacing.ENABLED)) {
                    level.setBlock(pos, st.setValue(BlockTCDeviceFacing.ENABLED, false), 3);
                }
            } else if (!tile.gettingPower() && !st.getValue(BlockTCDeviceFacing.ENABLED)) {
                level.setBlock(pos, st.setValue(BlockTCDeviceFacing.ENABLED, true), 3);
            }
        }
        if (!tile.gettingPower() && tile.charges > 1 && tile.count++ % 300 == 0) {
            tile.updateAnimals();
        }
    }

    private void updateAnimals() {
        int distance = 7;
        List<Animal> animals = this.level.getEntitiesOfClass(Animal.class, new AABB(this.worldPosition).inflate(distance));
        for (Animal a : animals) {
            if (a.getAge() == 0 && !a.isInLove()) {
                List<Animal> same = new ArrayList<>();
                for (Animal b : animals) {
                    if (b.getClass().equals(a.getClass())) {
                        same.add(b);
                    }
                }
                if (same.size() <= 9) {
                    Animal partner = null;
                    for (Animal b : same) {
                        if (b.getAge() == 0 && !b.isInLove()) {
                            if (partner != null) {
                                this.charges -= 5;
                                b.setInLove(null);
                                partner.setInLove(null);
                                this.setChanged();
                                return;
                            }
                            partner = b;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.charges = nbt.getInt("charges");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putInt("charges", this.charges);
        return nbt;
    }

    private boolean drawEssentia() {
        if (++this.drawDelay % 5 != 0) {
            return false;
        }
        Direction f = this.facing();
        if (ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition, f) instanceof IEssentiaTransport ic) {
            if (!ic.canOutputTo(f.getOpposite())) {
                return false;
            }
            return ic.getSuctionAmount(f.getOpposite()) < this.getSuctionAmount(f) && ic.takeEssentia(Aspect.DESIRE, 1, f.getOpposite()) == 1;
        }
        return false;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face == this.facing();
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face == this.facing();
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
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
        return Aspect.DESIRE;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return face == this.facing() ? 128 - this.charges * 10 : 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }
}
