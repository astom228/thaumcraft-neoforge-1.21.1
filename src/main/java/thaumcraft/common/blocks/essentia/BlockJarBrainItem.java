package thaumcraft.common.blocks.essentia;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.lib.TCDataComponents;

/** Мозг в банке — предмет (порт BlockJarBrainItem): хранит опыт, показывает его в подсказке. */
public class BlockJarBrainItem extends BlockItem {

    public BlockJarBrainItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Integer xp = stack.get(TCDataComponents.BRAIN_XP.get());
        if (xp != null) {
            tooltip.add(Component.literal("§a" + xp + " xp"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
