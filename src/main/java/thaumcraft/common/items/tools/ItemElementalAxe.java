package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Топор Потока (порт ItemElementalAxe): урон 8, пока зажата ПКМ — притягивает предметы в радиусе 10 (с пузырьками).
 * Из рецепта выходит с наполняющими «Сборщик» и «Прокапывание».
 */
public class ItemElementalAxe extends AxeItem implements ICreativeVariants {
    public ItemElementalAxe(Tier tier, Properties props) {
        super(tier, props.attributes(AxeItem.createAttributes(tier, 8.0F - tier.getAttackDamageBonus(), -3.0F)));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
        List<ItemEntity> stuff = EntityUtils.getEntitiesInRange(level, player.getX(), player.getY(), player.getZ(), player, ItemEntity.class, 10.0);
        for (ItemEntity e : stuff) {
            if (!e.isAlive()) {
                continue;
            }
            double d6 = e.getX() - player.getX();
            double d8 = e.getY() - player.getY() + player.getBbHeight() / 2.0F;
            double d10 = e.getZ() - player.getZ();
            double d11 = Math.sqrt(d6 * d6 + d8 * d8 + d10 * d10);
            d6 /= d11;
            d8 /= d11;
            d10 /= d11;
            double d13 = 0.3;
            Vec3 m = e.getDeltaMovement();
            double mx = Math.max(-0.25, Math.min(0.25, m.x - d6 * d13));
            double my = Math.max(-0.25, Math.min(0.25, m.y - (d8 * d13 - 0.1)));
            double mz = Math.max(-0.25, Math.min(0.25, m.z - d10 * d13));
            e.setDeltaMovement(mx, my, mz);
            if (level.isClientSide) {
                IFXDispatcher.get().crucibleBubble((float) e.getX() + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F,
                        (float) e.getY() + e.getBbHeight() + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F,
                        (float) e.getZ() + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F, 0.33F, 0.33F, 1.0F);
            }
        }
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        ItemStack w1 = new ItemStack(this);
        EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.BURROWING, 1);
        EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.COLLECTOR, 1);
        output.accept(w1);
    }
}
