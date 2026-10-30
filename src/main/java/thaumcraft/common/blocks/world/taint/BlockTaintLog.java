package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.blocks.BlocksTC;

/** Заражённое бревно (порт BlockTaintLog): распространяет волокна; вдали от семени становится флюкс-слизью. */
public class BlockTaintLog extends RotatedPillarBlock implements ITaintBlock {
    public BlockTaintLog(Properties props) {
        super(props);
    }

    @Override
    public void die(Level world, BlockPos pos, BlockState state) {
        world.setBlockAndUpdate(pos, BlocksTC.fluxGoo.get().defaultBlockState());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!TaintHelper.isNearTaintSeed(world, pos)) {
            this.die(world, pos, state);
        } else {
            TaintHelper.spreadFibres(world, pos);
        }
    }
}
