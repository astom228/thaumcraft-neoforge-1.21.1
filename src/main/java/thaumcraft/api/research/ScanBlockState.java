package thaumcraft.api.research;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Конкретное состояние блока (item = true — и предмет блока с тем же ключом). */
public class ScanBlockState implements IScanThing {
    String research;
    BlockState blockState;

    public ScanBlockState(BlockState blockState) {
        this.research = "!" + blockState.toString();
        this.blockState = blockState;
    }

    public ScanBlockState(String research, BlockState blockState) {
        this.research = research;
        this.blockState = blockState;
    }

    public ScanBlockState(String research, BlockState blockState, boolean item) {
        this.research = research;
        this.blockState = blockState;
        if (item) {
            ScanningManager.addScannableThing(new ScanItem(research, new ItemStack(blockState.getBlock())));
        }
    }

    @Override
    public boolean checkThing(Player player, Object obj) {
        return obj instanceof BlockPos pos && player.level().getBlockState(pos) == this.blockState;
    }

    @Override
    public String getResearchKey(Player player, Object object) {
        return this.research;
    }
}
