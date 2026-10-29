package thaumcraft.common.items.consumables;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.projectile.EntityAlumentum;

/**
 * Алюментум (порт ItemAlumentum): бросается и взрывается; топливо на 4800 тиков (IFuelHandler оригинала), в плавильне
 * эссенции — ускорение. Раздатчик бросает его (BehaviorDispenseAlumetum) — через ProjectileItem.
 */
public class ItemAlumentum extends Item implements ProjectileItem {
    public ItemAlumentum(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.playSound(SoundEvents.EGG_THROW, 0.3F, 0.4F / (world.random.nextFloat() * 0.4F + 0.8F));
        if (!world.isClientSide) {
            EntityAlumentum alumentum = new EntityAlumentum(world, player);
            alumentum.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, 0.4F, 2.0F);
            world.addFreshEntity(alumentum);
        }
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) {
        return 4800;
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new EntityAlumentum(level, pos.x(), pos.y(), pos.z());
    }
}
