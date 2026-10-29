package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.devices.BlockDioptra;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

import java.util.Arrays;

/**
 * Диоптра (порт TileDioptra): раз в секунду снимает карту 13×13 чанков вокруг — Вис (включена) или Выброс (выключена),
 * 0–64 на ячейку (500 = 64). Только уже известные ауре чанки, без загрузки. Рендер берёт из counter фазу «волны» яркости.
 */
public class TileDioptra extends TileThaumcraft {
    public int counter = 0;
    public byte[] grid_amt = new byte[169];

    public TileDioptra(BlockPos pos, BlockState state) {
        super(ConfigBlocks.DIOPTRA_TILE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileDioptra tile) {
        tile.counter++;
        if (level.isClientSide) {
            // как в оригинале: на клиенте счётчик сбрасывается каждый тик — «волна» яркости сетки стоит на месте
            tile.counter = 0;
            return;
        }
        if (tile.counter % 20 == 0) {
            Arrays.fill(tile.grid_amt, (byte) 0);
            boolean vis = state.getValue(BlockDioptra.ENABLED);
            for (int xx = 0; xx < 13; xx++) {
                for (int zz = 0; zz < 13; zz++) {
                    AuraChunk ac = AuraHandler.getAuraChunk(level, (pos.getX() >> 4) + xx - 6, (pos.getZ() >> 4) + zz - 6);
                    if (ac != null) {
                        tile.grid_amt[xx + zz * 13] = (byte) Math.min(64.0F, (vis ? ac.getVis() : ac.getFlux()) / 500.0F * 64.0F);
                    }
                }
            }
            tile.setChanged();
            tile.syncTile(false);
        }
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        if (nbt.contains("grid_a")) {
            byte[] b = nbt.getByteArray("grid_a");
            if (b.length == 169) {
                this.grid_amt = b;
            }
        }
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putByteArray("grid_a", this.grid_amt);
        return nbt;
    }
}
