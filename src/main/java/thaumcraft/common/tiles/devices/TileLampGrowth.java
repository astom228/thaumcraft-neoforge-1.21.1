package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.devices.BlockTCDeviceFacing;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.network.fx.PacketFXBlockMist;
import thaumcraft.common.lib.utils.CropUtils;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Лампа роста (порт TileLampGrowth): 20 зарядов за 1 Herba (тянет эссенцию со стороны крепления, есть запас ещё на одну);
 * без сигнала каждый тик выбирает очередной столбец в квадрате 13×13 над собой (до 6 вниз) и даёт первому неспелому растению
 * в радиусе 6 «случайный тик» роста (в 1.12 — запланированный тик блока, вызывавший рост). Туман над растением — пакетом.
 */
public class TileLampGrowth extends TileThaumcraft implements IEssentiaTransport {
    private boolean reserve = false;
    public int charges = -1;
    public int maxCharges = 20;
    private BlockPos last = BlockPos.ZERO;
    private Block lastBlock = null;
    private BlockState lastState = null;
    private final List<BlockPos> checklist = new ArrayList<>();
    private int drawDelay = 0;

    public TileLampGrowth(BlockPos pos, BlockState state) {
        super(ConfigBlocks.LAMP_GROWTH_TILE.get(), pos, state);
    }

    private Direction facing() {
        return this.getBlockState().getValue(BlockTCDeviceFacing.FACING);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileLampGrowth tile) {
        if (tile.charges <= 0) {
            if (tile.reserve) {
                tile.charges = tile.maxCharges;
                tile.reserve = false;
                tile.setChanged();
                tile.syncTile(true);
            } else if (tile.drawEssentia()) {
                tile.charges = tile.maxCharges;
                tile.setChanged();
                tile.syncTile(true);
            }
            BlockState st = level.getBlockState(pos);
            if (tile.charges <= 0) {
                if (st.getValue(BlockTCDeviceFacing.ENABLED)) {
                    level.setBlock(pos, st.setValue(BlockTCDeviceFacing.ENABLED, false), 3);
                }
            } else if (!tile.gettingPower() && !st.getValue(BlockTCDeviceFacing.ENABLED)) {
                level.setBlock(pos, st.setValue(BlockTCDeviceFacing.ENABLED, true), 3);
            }
        }
        if (!tile.reserve && tile.drawEssentia()) {
            tile.reserve = true;
        }
        if (tile.charges == 0) {
            tile.charges = -1;
            tile.syncTile(true);
        }
        if (!tile.gettingPower() && tile.charges > 0) {
            tile.updatePlant((ServerLevel) level);
        }
    }

    private void updatePlant(ServerLevel level) {
        BlockState bs = level.getBlockState(this.last);
        if (this.lastState != null && bs != this.lastState) {
            PacketDistributor.sendToPlayersNear(level, null, this.last.getX(), this.last.getY(), this.last.getZ(), 32.0,
                    new PacketFXBlockMist(this.last, 4259648));
            this.lastBlock = bs.getBlock();
            this.lastState = bs;
        }
        int distance = 6;
        if (this.checklist.isEmpty()) {
            for (int a = -distance; a <= distance; a++) {
                for (int b = -distance; b <= distance; b++) {
                    this.checklist.add(this.worldPosition.offset(a, distance, b));
                }
            }
            Collections.shuffle(this.checklist, new java.util.Random(level.random.nextLong()));
        }
        BlockPos col = this.checklist.remove(0);
        int x = col.getX();
        int y = col.getY();
        int z = col.getZ();
        while (y >= this.worldPosition.getY() - distance) {
            BlockPos bp = new BlockPos(x, y, z);
            if (level.isLoaded(bp) && !level.isEmptyBlock(bp) && CropUtils.isPlant(level, bp)
                    && this.worldPosition.distToCenterSqr(x + 0.5, y + 0.5, z + 0.5) < distance * distance && !CropUtils.isGrownCrop(level, bp)
                    && CropUtils.doesLampGrow(level, bp)) {
                this.charges--;
                this.last = bp;
                BlockState bs2 = level.getBlockState(bp);
                this.lastBlock = bs2.getBlock();
                this.lastState = bs2;
                bs2.randomTick(level, bp, level.random);
                return;
            }
            y--;
        }
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.reserve = nbt.getBoolean("reserve");
        this.charges = nbt.getInt("charges");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putBoolean("reserve", this.reserve);
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
            return ic.getSuctionAmount(f.getOpposite()) < this.getSuctionAmount(f) && ic.takeEssentia(Aspect.PLANT, 1, f.getOpposite()) == 1;
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
        return Aspect.PLANT;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return face != this.facing() || this.reserve && this.charges > 0 ? 0 : 128;
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
