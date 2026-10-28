package thaumcraft.common.items.tools;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.IWarpingGear;

/** Пустотная кирка (порт ItemVoidPickaxe): самопочинка, слабость на 4 с при ударе (игрокам — при PvP), Искажение 1. */
public class ItemVoidPickaxe extends PickaxeItem implements IWarpingGear {
    public ItemVoidPickaxe(Tier tier, Properties props) {
        super(tier, props.attributes(PickaxeItem.createAttributes(tier, 1.0F, -2.8F)));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidMetalHelper.repairTick(stack, level, entity);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        VoidMetalHelper.weaken(entity, 80);
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 1;
    }
}
