package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.essentia.TileTube;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;

/**
 * Резонатор эссенции (порт ItemResonator): ПКМ по блоку, передающему эссенцию, — сообщение о содержимом и всасывании
 * (у трубки — со стороны, на которую смотрит игрок). Конденсатор — с этапом 14.
 */
public class ItemResonator extends Item {

    public ItemResonator(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !stack.getComponentsPatch().isEmpty();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockEntity tile = world.getBlockEntity(pos);
        if (!(tile instanceof IEssentiaTransport et) || player == null) {
            return InteractionResult.FAIL;
        }
        if (world.isClientSide) {
            player.swing(context.getHand());
            return InteractionResult.PASS;
        }
        Direction side = context.getClickedFace();
        if (tile instanceof TileTube tube) {
            int hit = tube.getSubHit(player);
            if (hit >= 0 && hit < 6) {
                side = Direction.values()[hit];
            }
        }
        if (!(tile instanceof TileTubeBuffer) && et.getEssentiaType(side) != null) {
            player.sendSystemMessage(Component.translatable("tc.resonator1", "" + et.getEssentiaAmount(side), et.getEssentiaType(side).getName()));
        } else if (tile instanceof TileTubeBuffer && ((IAspectContainer) tile).getAspects().size() > 0) {
            for (Aspect aspect : ((IAspectContainer) tile).getAspects().getAspectsSortedByName()) {
                player.sendSystemMessage(Component.translatable("tc.resonator1", "" + ((IAspectContainer) tile).getAspects().getAmount(aspect), aspect.getName()));
            }
        }
        Component s = Component.translatable("tc.resonator3");
        if (et.getSuctionType(side) != null) {
            s = Component.literal(et.getSuctionType(side).getName());
        }
        player.sendSystemMessage(Component.translatable("tc.resonator2", "" + et.getSuctionAmount(side), s));
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS, 0.5F, 1.9F + world.random.nextFloat() * 0.1F);
        if (tile instanceof thaumcraft.common.tiles.devices.TileCondenser cond) {
            player.sendSystemMessage(Component.translatable("tc.condenser1", "" + cond.cost));
            player.sendSystemMessage(Component.translatable("tc.condenser2", "" + cond.interval, "" + cond.interval / 20));
        }
        return InteractionResult.SUCCESS;
    }
}
