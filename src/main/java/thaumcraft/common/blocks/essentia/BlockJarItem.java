package thaumcraft.common.blocks.essentia;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.essentia.TileAlembic;

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

    /** Набрать в банку всю эссенцию алембика (до 250; с ярлыком — только аспект ярлыка, с эссенцией — только её аспект). */
    @Override
    public InteractionResult onItemUseFirst(ItemStack itemstack, UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player != null && world.getBlockState(pos).is(BlocksTC.alembic.get()) && !world.isClientSide
                && world.getBlockEntity(pos) instanceof TileAlembic tile && tile.amount > 0) {
            if (this.getFilter(itemstack) != null && this.getFilter(itemstack) != tile.aspect) {
                return InteractionResult.FAIL;
            }
            if (this.getAspects(itemstack) != null && this.getAspects(itemstack).getAspects()[0] != tile.aspect) {
                return InteractionResult.FAIL;
            }
            int amt = tile.amount;
            if (this.getAspects(itemstack) != null && this.getAspects(itemstack).visSize() + amt > 250) {
                amt = Math.abs(this.getAspects(itemstack).visSize() - 250);
            }
            if (amt <= 0) {
                return InteractionResult.FAIL;
            }
            Aspect a = tile.aspect;
            if (tile.takeFromContainer(tile.aspect, amt)) {
                int base = this.getAspects(itemstack) == null ? 0 : this.getAspects(itemstack).visSize();
                if (itemstack.getCount() > 1) {
                    ItemStack stack = itemstack.copy();
                    this.setAspects(stack, new AspectList().add(a, base + amt));
                    itemstack.shrink(1);
                    stack.setCount(1);
                    if (!player.getInventory().add(stack)) {
                        world.addFreshEntity(new ItemEntity(world, player.getX(), player.getY(), player.getZ(), stack));
                    }
                } else {
                    this.setAspects(itemstack, new AspectList().add(a, base + amt));
                }
                player.playSound(SoundEvents.BOTTLE_FILL, 0.25F, 1.0F);
                player.inventoryMenu.broadcastChanges();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

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
