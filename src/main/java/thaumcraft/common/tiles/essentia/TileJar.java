package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.tiles.TileThaumcraft;

/** Основа банок (порт TileJar). */
public class TileJar extends TileThaumcraft {

    public TileJar(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void update() {
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileJar te) {
        te.update();
    }
}
