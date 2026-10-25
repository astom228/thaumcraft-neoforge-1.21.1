package thaumcraft.common.blocks.essentia;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Банка-предмет (порт BlockJarItem): хранит эссенцию и ярлык (компоненты), при установке передаёт их блоку.
 * Как в оригинале, полоса «прочности» видна у банки с эссенцией и всегда полная (деление нацело visSize / 250).
 */
public class BlockJarItem extends BlockItem implements IEssentiaContainerItem {

    public BlockJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return this.getAspects(stack) != null;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return 13;
    }

    // TODO этап 10.4: onItemUseFirst — набрать эссенцию из алембика

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Aspect tag = BlockJar.filterOf(stack);
        if (tag != null) {
            tooltip.add(Component.literal("§5" + tag.getName()));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public AspectList getAspects(ItemStack itemstack) {
        AspectList al = itemstack.get(TCDataComponents.ASPECTS.get());
        return al != null && al.size() > 0 ? al.copy() : null;
    }

    public Aspect getFilter(ItemStack itemstack) {
        return BlockJar.filterOf(itemstack);
    }

    @Override
    public void setAspects(ItemStack itemstack, AspectList aspects) {
        itemstack.set(TCDataComponents.ASPECTS.get(), aspects.copy());
    }

    @Override
    public boolean ignoreContainedAspects() {
        return false;
    }
}
