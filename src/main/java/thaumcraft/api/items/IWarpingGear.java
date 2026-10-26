package thaumcraft.api.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Снаряжение, дающее Искажение, пока надето или в руке (порт IWarpingGear). */
public interface IWarpingGear {
    int getWarp(ItemStack stack, Player player);
}
