package thaumcraft.common.items.tools;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.entities.projectile.EntityGrapple;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Магический крюкомёт (порт ItemGrappleGun): заряд Вис до 100, выстрел крюком — 1 заряд; пока крюк летит или держит —
 * модель «заряжен» (компонент thaumcraft:grapple_loaded, NBT "loaded" оригинала; свойство модели type).
 */
public class ItemGrappleGun extends Item implements IRechargable {
    public ItemGrappleGun(Properties props) {
        super(props);
    }

    public static boolean isLoaded(ItemStack stack) {
        return stack.getOrDefault(TCDataComponents.GRAPPLE_LOADED.get(), false);
    }

    @Override
    public int getMaxCharge(ItemStack stack, LivingEntity player) {
        return 100;
    }

    @Override
    public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player) {
        return EnumChargeDisplay.NORMAL;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // крюков игрока знает только сервер; клиент получает компонент синхронизацией
        if (!level.isClientSide && isLoaded(stack) && entity instanceof Player p && !(level.getEntity(EntityGrapple.getGrapple(p)) instanceof EntityGrapple)) {
            stack.remove(TCDataComponents.GRAPPLE_LOADED.get());
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        player.playSound(SoundsTC.ice.get(), 3.0F, 0.8F + world.random.nextFloat() * 0.1F);
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && RechargeHelper.getCharge(stack) > 0) {
            EntityGrapple grapple = new EntityGrapple(world, player, hand);
            grapple.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, 1.5F, 0.0F);
            double px = -Mth.cos((player.getYRot() - 0.5F) / 180.0F * 3.141593F) * 0.2F * (hand == InteractionHand.MAIN_HAND ? 1 : -1);
            double pz = -Mth.sin((player.getYRot() - 0.5F) / 180.0F * 3.141593F) * 0.3F * (hand == InteractionHand.MAIN_HAND ? 1 : -1);
            Vec3 vl = player.getLookAngle();
            // в 1.12 по z прибавлялось vl.y (опечатка) — здесь vl.z, крюк вылетает из руки по направлению взгляда
            grapple.setPos(grapple.getX() + px + vl.x, grapple.getY(), grapple.getZ() + pz + vl.z);
            if (world.addFreshEntity(grapple)) {
                RechargeHelper.consumeCharge(stack, player, 1);
                stack.set(TCDataComponents.GRAPPLE_LOADED.get(), true);
            }
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (oldStack.getItem() == this && newStack.getItem() == this) {
            return isLoaded(oldStack) != isLoaded(newStack);
        }
        return newStack.getItem() != oldStack.getItem();
    }
}
