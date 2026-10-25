package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.container.ContainerHandMirror;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.devices.TileMirror;

/**
 * Ручное зеркало (порт ItemHandMirror): щелчком по магическому зеркалу связывается с ним; в руке открывает окно, предмет из
 * которого сразу уходит в связанное зеркало. Если зеркала больше нет — связь стирается.
 */
public class ItemHandMirror extends Item {

    public ItemHandMirror(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(TCDataComponents.MIRROR_LINK.get());
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !world.getBlockState(pos).is(BlocksTC.mirror.get())) {
            return InteractionResult.FAIL;
        }
        if (world.isClientSide) {
            player.swing(context.getHand());
            return InteractionResult.PASS;
        }
        if (world.getBlockEntity(pos) instanceof TileMirror) {
            stack.set(TCDataComponents.MIRROR_LINK.get(), GlobalPos.of(world.dimension(), pos));
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundsTC.jar.get(), SoundSource.BLOCKS, 1.0F, 2.0F);
            player.sendSystemMessage(Component.translatable("tc.handmirrorlinked"));
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GlobalPos link = stack.get(TCDataComponents.MIRROR_LINK.get());
        if (!world.isClientSide && link != null && player instanceof ServerPlayer sp) {
            if (this.target(sp, stack, link) != null) {
                sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ContainerHandMirror(id, inv), stack.getHoverName()));
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    /** Связанное зеркало (как в оригинале, его чанк при обращении загружается); нет зеркала — связь стирается. */
    private TileMirror target(Player player, ItemStack mirror, GlobalPos link) {
        ServerLevel targetWorld = player.getServer() == null ? null : player.getServer().getLevel(link.dimension());
        if (targetWorld == null) {
            return null;
        }
        if (targetWorld.getBlockEntity(link.pos()) instanceof TileMirror tm) {
            return tm;
        }
        mirror.remove(TCDataComponents.MIRROR_LINK.get());
        player.playNotifySound(SoundsTC.zap.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        player.sendSystemMessage(Component.translatable("tc.handmirrorerror"));
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos link = stack.get(TCDataComponents.MIRROR_LINK.get());
        if (link != null) {
            tooltip.add(Component.translatable("tc.handmirrorlinkedto").append(" " + link.pos().getX() + "," + link.pos().getY() + ","
                    + link.pos().getZ() + " ").append(Component.translatable("tc.mirror.in_dimension", link.dimension().location().toString())));
        }
    }

    /** Отправить предметы в связанное зеркало (из окна ручного зеркала). */
    public static boolean transport(ItemStack mirror, ItemStack items, Player player) {
        GlobalPos link = mirror.get(TCDataComponents.MIRROR_LINK.get());
        if (link == null || !(mirror.getItem() instanceof ItemHandMirror hm)) {
            return false;
        }
        TileMirror tm = hm.target(player, mirror, link);
        if (tm == null) {
            return false;
        }
        if (tm.transportDirect(items)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.1F, 1.0F);
        }
        return true;
    }
}
