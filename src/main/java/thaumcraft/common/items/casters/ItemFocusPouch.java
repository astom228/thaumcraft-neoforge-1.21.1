package thaumcraft.common.items.casters;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import thaumcraft.common.container.ContainerFocusPouch;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Сумка для фокусов (порт ItemFocusPouch): 18 фокусов; ПКМ — открыть. Фокусы из сумки (в инвентаре и на поясе)
 * доступны при смене фокуса перчатки. Носится на поясе (Curios, тег curios:belt).
 */
public class ItemFocusPouch extends Item implements top.theillusivec4.curios.api.type.capability.ICurioItem {
    public static final int SIZE = 18;

    public ItemFocusPouch(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        // как в оригинале — сумка из выбранной ячейки основной руки
        if (!world.isClientSide && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            int slot = sp.getInventory().selected;
            sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ContainerFocusPouch(id, inv, slot), Component.translatable("item.thaumcraft.focus_pouch")),
                    buf -> buf.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), world.isClientSide);
    }

    public NonNullList<ItemStack> getInventory(ItemStack item) {
        NonNullList<ItemStack> stackList = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ItemContainerContents c = item.get(TCDataComponents.POUCH_CONTENTS.get());
        if (c != null) {
            c.copyInto(stackList);
        }
        return stackList;
    }

    public void setInventory(ItemStack item, NonNullList<ItemStack> stackList) {
        item.set(TCDataComponents.POUCH_CONTENTS.get(), ItemContainerContents.fromItems(stackList));
    }
}
