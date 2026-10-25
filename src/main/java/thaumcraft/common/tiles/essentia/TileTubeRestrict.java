package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ConfigBlocks;

/** Ограничительная трубка: передаёт дальше только половину всасывания. */
public class TileTubeRestrict extends TileTube {

    public TileTubeRestrict(BlockPos pos, BlockState state) {
        super(ConfigBlocks.TUBE_RESTRICT_TILE.get(), pos, state);
    }

    @Override
    void calculateSuction(Aspect filter, boolean restrict, boolean dir) {
        super.calculateSuction(filter, true, dir);
    }
}
