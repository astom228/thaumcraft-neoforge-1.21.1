package thaumcraft.common.lib.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IDustTrigger;

/** Соль мира на любой блок из тега (в оригинале — запись OreDictionary) → результат. */
public class DustTriggerOre implements IDustTrigger {
    final TagKey<Block> target;
    final ItemStack result;
    final String research;

    public DustTriggerOre(String research, TagKey<Block> target, ItemStack result) {
        this.target = target;
        this.result = result;
        this.research = research;
    }

    @Override
    public Placement getValidFace(Level world, Player player, BlockPos pos, Direction face) {
        boolean b = world.getBlockState(pos).is(this.target);
        return !b || this.research != null && !ThaumcraftCapabilities.getKnowledge(player).isResearchKnown(this.research)
                ? null : new Placement(0, 0, 0, null);
    }

    @Override
    public void execute(Level world, Player player, BlockPos pos, Placement placement, Direction side) {
        DustTriggerSimple.execute(world, player, pos, this.result);
    }
}
