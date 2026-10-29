package thaumcraft.common.items.tools;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;

/** Кирка Ядра (порт ItemElementalPickaxe): поджигает цель на 2 с (игроков — при PvP); из рецепта — «Очистка» и «Зондирование II». */
public class ItemElementalPickaxe extends PickaxeItem implements ICreativeVariants {
    public ItemElementalPickaxe(Tier tier, Properties props) {
        super(tier, props.attributes(PickaxeItem.createAttributes(tier, 1.0F, -2.8F)));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide) {
            MinecraftServer server = player.level().getServer();
            if (!(entity instanceof Player) || server != null && server.isPvpAllowed()) {
                entity.igniteForSeconds(2.0F);
            }
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        ItemStack w1 = new ItemStack(this);
        EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.REFINING, 1);
        EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.SOUNDING, 2);
        output.accept(w1);
    }
}
