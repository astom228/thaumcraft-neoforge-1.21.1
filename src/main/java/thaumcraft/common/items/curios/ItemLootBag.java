package thaumcraft.common.items.curios;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.Utils;

/** Мешок с добычей (порт ItemLootBag, варианты 0–2 — обычный, необычный, редкий): ПКМ — 8–12 предметов по таблице редкости. */
public class ItemLootBag extends Item {
    private final int rarity;

    public ItemLootBag(int rarity, Properties props) {
        super(props);
        this.rarity = rarity;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tc.lootbag"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide) {
            int q = 8 + world.random.nextInt(5);
            for (int a = 0; a < q; a++) {
                ItemStack is = Utils.generateLoot(world, this.rarity, world.random);
                if (!is.isEmpty()) {
                    world.addFreshEntity(new ItemEntity(world, player.getX(), player.getY(), player.getZ(), is.copy()));
                }
            }
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundsTC.coins.get(), player.getSoundSource(), 0.75F, 1.0F);
        }
        player.getItemInHand(hand).shrink(1);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), world.isClientSide);
    }
}
