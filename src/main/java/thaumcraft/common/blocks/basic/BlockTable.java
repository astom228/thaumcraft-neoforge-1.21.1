package thaumcraft.common.blocks.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.IScribeTools;
import thaumcraft.common.tiles.crafting.TileResearchTable;

/**
 * Деревянный и каменный стол (порт BlockTable). Деревянный стол + письменные принадлежности (правый клик) → стол исследований,
 * принадлежности кладутся в него; срабатывает событие крафта стола исследований (требование исследования «Теория»).
 * Блок не полный (ножки), выпадает без инструмента.
 */
public class BlockTable extends Block {
    public BlockTable(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (world.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (this == BlocksTC.tableWood.get() && stack.getItem() instanceof IScribeTools) {
            BlockState bs = BlocksTC.researchTable.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, player.getDirection());
            world.setBlock(pos, bs, 3);
            if (world.getBlockEntity(pos) instanceof TileResearchTable tile) {
                tile.setInventorySlotContents(0, stack.copy());
                player.setItemInHand(hand, ItemStack.EMPTY);
                tile.setChanged();
            }
            EventHooks.firePlayerCraftingEvent(player, new ItemStack(BlocksTC.researchTable.get()), new SimpleContainer(1));
        }
        return ItemInteractionResult.SUCCESS;
    }
}
