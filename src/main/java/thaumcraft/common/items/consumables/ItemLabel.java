package thaumcraft.common.items.consumables;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.blocks.ILabelable;
import thaumcraft.api.items.ItemGenericEssentiaContainer;

/**
 * Ярлык (порт ItemLabel; metadata 0/1 → label / marked_label): клеится на блок или BlockEntity, реализующий ILabelable
 * (банки — этап 10.2). Аспект ярлыка не входит в аспекты предмета.
 */
public class ItemLabel extends ItemGenericEssentiaContainer {
    private final boolean filled;

    public ItemLabel(boolean filled, Properties properties) {
        super(1, properties);
        this.filled = filled;
    }

    /** Во вкладке — только чистый ярлык. */
    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        if (!this.filled) {
            output.accept(this);
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        BlockState bs = world.getBlockState(context.getClickedPos());
        if (bs.getBlock() instanceof ILabelable lab) {
            if (lab.applyLabel(player, context.getClickedPos(), context.getClickedFace(), stack)) {
                stack.shrink(1);
                if (player != null) {
                    player.inventoryMenu.broadcastChanges();
                }
            }
            return InteractionResult.SUCCESS;
        }
        BlockEntity te = world.getBlockEntity(context.getClickedPos());
        if (te instanceof ILabelable lab) {
            if (lab.applyLabel(player, context.getClickedPos(), context.getClickedFace(), stack)) {
                stack.shrink(1);
                if (player != null) {
                    player.inventoryMenu.broadcastChanges();
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
    }

    @Override
    public boolean ignoreContainedAspects() {
        return true;
    }
}
