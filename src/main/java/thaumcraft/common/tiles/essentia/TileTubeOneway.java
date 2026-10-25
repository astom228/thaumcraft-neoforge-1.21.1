package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ConfigBlocks;

/** Направленная трубка: всасывание только со стороны facing, эссенция — только с противоположной. */
public class TileTubeOneway extends TileTube {

    public TileTubeOneway(BlockPos pos, BlockState state) {
        super(ConfigBlocks.TUBE_ONEWAY_TILE.get(), pos, state);
    }

    @Override
    void calculateSuction(Aspect filter, boolean restrict, boolean directional) {
        super.calculateSuction(filter, restrict, true);
    }

    @Override
    void equalizeWithNeighbours(boolean directional) {
        super.equalizeWithNeighbours(true);
    }
}
