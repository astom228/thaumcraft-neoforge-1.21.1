package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.IWarpingGear;

/** Пустотный меч (порт ItemVoidSword): самопочинка, слабость на 3 с при ударе (игрокам — при PvP), Искажение 1. */
public class ItemVoidSword extends SwordItem implements IWarpingGear {
    public ItemVoidSword(Tier tier, Properties props) {
        super(tier, props.attributes(SwordItem.createAttributes(tier, 3, -2.4F)));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidMetalHelper.repairTick(stack, level, entity);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // в 1.12 — проверка PvP, только если бьют игрока игроком
        if (!(attacker instanceof Player) || !(target instanceof Player)) {
            if (!target.level().isClientSide) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 60));
            }
        } else {
            VoidMetalHelper.weaken(target, 60);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.special.sapless").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
