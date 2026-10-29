package thaumcraft.common.blocks.basic;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.misc.TileBanner;

/**
 * Предмет знамени (порт BlockBannerTCItem): ставится на верх блока (на шесте, по повороту игрока на 16 направлений) или на
 * сторону (висит на стене); не на низ. Аспект из компонента переносится в знамя; в подсказке — название аспекта.
 */
public class BlockBannerTCItem extends BlockItem {
    public BlockBannerTCItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String as = stack.get(TCDataComponents.BANNER_ASPECT.get());
        if (as != null && Aspect.getAspect(as) != null) {
            tooltip.add(Component.literal(Aspect.getAspect(as).getName()));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Direction side = ctx.getClickedFace();
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos clicked = ctx.getClickedPos();
        if (side == Direction.DOWN || !world.getBlockState(clicked).isSolid()) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = clicked.relative(side);
        ItemStack stack = ctx.getItemInHand();
        if (player != null && !player.mayUseItemAt(pos, side, stack) || !BlockBannerTC.canPlaceAt(world, pos)) {
            return InteractionResult.FAIL;
        }
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        world.setBlock(pos, this.getBlock().defaultBlockState(), 3);
        if (world.getBlockEntity(pos) instanceof TileBanner tile) {
            if (side == Direction.UP) {
                float yaw = player != null ? player.getYRot() : 0.0F;
                tile.setBannerFacing((byte) (Mth.floor((yaw + 180.0F) * 16.0F / 360.0F + 0.5) & 15));
            } else {
                tile.setWall(true);
                int i = switch (side) {
                    case NORTH -> 8;
                    case WEST -> 4;
                    case EAST -> 12;
                    default -> 0;
                };
                tile.setBannerFacing((byte) i);
            }
            String as = stack.get(TCDataComponents.BANNER_ASPECT.get());
            if (as != null) {
                tile.setAspect(Aspect.getAspect(as));
            }
            tile.setChanged();
            tile.syncTile(true);
        }
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
