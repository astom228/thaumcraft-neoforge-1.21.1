package thaumcraft.common.blocks.devices;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.common.tiles.devices.TileMirrorEssentia;
import thaumcraft.common.tiles.devices.TileMirrorLinked;

/**
 * Зеркало-предмет (порт BlockMirrorItem): щелчок по несвязанному зеркалу того же вида даёт копию со связью с ним
 * (одно зеркало тратится); установленная копия связывается с ним.
 */
public class BlockMirrorItem extends BlockItem {

    public BlockMirrorItem(Block block, Properties properties) {
        super(block, properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !(world.getBlockState(pos).getBlock() instanceof BlockMirror)) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide) {
            player.swing(context.getHand());
            return InteractionResult.PASS;
        }
        BlockEntity te = world.getBlockEntity(pos);
        boolean sameKind = this.getBlock() == world.getBlockState(pos).getBlock()
                && (te instanceof TileMirror || te instanceof TileMirrorEssentia);
        if (sameKind && te instanceof TileMirrorLinked tm) {
            if (!tm.isLinkValid(true)) {
                ItemStack st = stack.copy();
                st.setCount(1);
                st.set(TCDataComponents.MIRROR_LINK.get(), GlobalPos.of(world.dimension(), pos));
                world.playSound(null, pos, SoundsTC.jar.get(), SoundSource.BLOCKS, 1.0F, 2.0F);
                if (!player.getInventory().add(st)) {
                    world.addFreshEntity(new ItemEntity(world, player.getX(), player.getY(), player.getZ(), st));
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.inventoryMenu.broadcastChanges();
            } else {
                player.sendSystemMessage(Component.translatable("tc.mirror.already_linked"));
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        GlobalPos link = stack.get(TCDataComponents.MIRROR_LINK.get());
        if (link != null) {
            list.add(Component.translatable("tc.mirror.linked_to", link.pos().getX(), link.pos().getY(), link.pos().getZ(),
                    link.dimension().location().toString()));
        }
        super.appendHoverText(stack, context, list, flag);
    }
}
