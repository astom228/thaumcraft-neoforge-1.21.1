package thaumcraft.common.items.consumables;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.projectile.EntityCausalityCollapser;

/** Коллапсер причинности (порт ItemCausalityCollapser): бросаемый заряд, до 16 в стаке. */
public class ItemCausalityCollapser extends Item {
    public ItemCausalityCollapser(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.playSound(SoundEvents.EGG_THROW, 0.3F, 0.4F / (world.random.nextFloat() * 0.4F + 0.8F));
        if (!world.isClientSide) {
            EntityCausalityCollapser projectile = new EntityCausalityCollapser(world, player);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, 0.8F, 2.0F);
            world.addFreshEntity(projectile);
        }
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }
}
