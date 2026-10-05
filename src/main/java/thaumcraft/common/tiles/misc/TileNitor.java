package thaumcraft.common.tiles.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.config.ConfigBlocks;

/** Частицы нитора (порт TileNitor.update на клиенте): каждый тик — язычок пламени, раз в 10 тиков — ядро. */
public class TileNitor extends BlockEntity {
    int count = 0;

    public TileNitor(BlockPos pos, BlockState state) {
        super(ConfigBlocks.NITOR_TILE.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileNitor tile) {
        IFXDispatcher.get().drawNitorFlames(
                pos.getX() + 0.5F + level.random.nextGaussian() * 0.025,
                pos.getY() + 0.45F + level.random.nextGaussian() * 0.025,
                pos.getZ() + 0.5F + level.random.nextGaussian() * 0.025,
                level.random.nextGaussian() * 0.0025,
                level.random.nextFloat() * 0.06,
                level.random.nextGaussian() * 0.0025,
                state.getMapColor(level, pos).col, 0);
        if (tile.count++ % 10 == 0) {
            IFXDispatcher.get().drawNitorCore(pos.getX() + 0.5F, pos.getY() + 0.49F, pos.getZ() + 0.5F, 0.0, 0.0, 0.0);
        }
    }
}
