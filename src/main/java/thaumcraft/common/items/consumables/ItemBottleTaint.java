package thaumcraft.common.items.consumables;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.projectile.EntityBottleTaint;

/** Бутылка порчи (порт ItemBottleTaint): бросается по дуге (до 8 в стаке). */
public class ItemBottleTaint extends Item {
    public ItemBottleTaint(Properties props) {
        super(props.stacksTo(8));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.playSound(SoundEvents.EGG_THROW, 0.5F, 0.4F / (world.random.nextFloat() * 0.4F + 0.8F));
        if (!world.isClientSide) {
            EntityBottleTaint bottle = new EntityBottleTaint(world, player);
            bottle.setItem(stack);
            bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, 0.66F, 1.0F);
            world.addFreshEntity(bottle);
        }
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }
}
