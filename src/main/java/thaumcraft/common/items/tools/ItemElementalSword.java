package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Меч Зефира (порт ItemElementalSword): пока зажата ПКМ — вихрь: игрок парит (падение гасится), существа (не игроки) в радиусе
 * 2.5 отбрасываются, дымная спираль; 1 прочности в секунду. Из рецепта — «Дуга II».
 */
public class ItemElementalSword extends SwordItem implements ICreativeVariants {
    public ItemElementalSword(Tier tier, Properties props) {
        super(tier, props.attributes(SwordItem.createAttributes(tier, 3, -2.4F)));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
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
        super.onUseTick(level, player, stack, count);
        int ticks = this.getUseDuration(stack, player) - count;
        Vec3 m = player.getDeltaMovement();
        double my = m.y;
        if (my < 0.0) {
            my /= 1.2F;
            player.fallDistance /= 1.2F;
        }
        my += 0.08F;
        if (my > 0.5) {
            my = 0.2F;
        }
        player.setDeltaMovement(m.x, my, m.z);
        if (player instanceof ServerPlayer sp) {
            // resetFloatCounter оригинала: не выкидывать за «полёт»
            sp.connection.aboveGroundTickCount = 0;
        }
        List<Entity> targets = level.getEntities(player, player.getBoundingBox().inflate(2.5, 2.5, 2.5));
        for (Entity entity : targets) {
            if (!(entity instanceof Player) && entity instanceof LivingEntity && entity.isAlive() && player.getVehicle() != entity) {
                Vec3 p = player.position();
                Vec3 t = entity.position();
                double distance = p.distanceTo(t) + 0.1;
                Vec3 r = t.subtract(p);
                entity.setDeltaMovement(entity.getDeltaMovement().add(r.x / 2.5 / distance, r.y / 2.5 / distance, r.z / 2.5 / distance));
            }
        }
        if (level.isClientSide) {
            int miny = (int) (player.getBoundingBox().minY - 2.0);
            if (player.onGround()) {
                miny = Mth.floor(player.getBoundingBox().minY);
            }
            for (int a = 0; a < 5; a++) {
                IFXDispatcher.get().smokeSpiral(player.getX(), player.getBoundingBox().minY + player.getBbHeight() / 2.0F, player.getZ(), 1.5F,
                        level.random.nextInt(360), miny, 14540253);
            }
            if (player.onGround()) {
                float r1 = level.random.nextFloat() * 360.0F;
                float mx = -Mth.sin(r1 / 180.0F * (float) Math.PI) / 5.0F;
                float mz = Mth.cos(r1 / 180.0F * (float) Math.PI) / 5.0F;
                level.addParticle(ParticleTypes.SMOKE, player.getX(), player.getBoundingBox().minY + 0.1F, player.getZ(), mx, 0.0, mz);
            }
        } else {
            if (ticks == 0 || ticks % 20 == 0) {
                player.playSound(SoundsTC.wind.get(), 0.5F, 0.9F + level.random.nextFloat() * 0.2F);
            }
            if (ticks % 20 == 0) {
                stack.hurtAndBreak(1, player, player.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
            }
        }
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        ItemStack w1 = new ItemStack(this);
        EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.ARCING, 2);
        output.accept(w1);
    }
}
