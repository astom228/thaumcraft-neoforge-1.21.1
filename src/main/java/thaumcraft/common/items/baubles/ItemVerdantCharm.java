package thaumcraft.common.items.baubles;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.lib.TCDataComponents;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Оберег зелёного сердца (порт ItemVerdantCharm): слот оберега, заряд Вис до 200. Раз в секунду за заряд снимает иссушение (20),
 * яд (10), отравление скверной (5); «Дарующий жизнь» (тип 1) лечит 1 ед. за 5; «Поддерживающий» (тип 2) восполняет воздух
 * и сытость за 1. Тип — компонент thaumcraft:verdant_type (NBT "type" оригинала), модель — свойство type.
 */
public class ItemVerdantCharm extends Item implements ICurioItem, IRechargable, thaumcraft.common.items.ICreativeVariants {
    public ItemVerdantCharm(Properties props) {
        super(props);
    }

    public static int getType(ItemStack stack) {
        return stack.getOrDefault(TCDataComponents.VERDANT_TYPE.get(), 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (getType(stack) == 1) {
            tooltip.add(Component.translatable("item.verdant_charm.life.text").withStyle(ChatFormatting.GOLD));
        }
        if (getType(stack) == 2) {
            tooltip.add(Component.translatable("item.verdant_charm.sustain.text").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity player = slotContext.entity();
        if (player.level().isClientSide || player.tickCount % 20 != 0 || !(player instanceof Player p)) {
            return;
        }
        if (p.hasEffect(MobEffects.WITHER) && RechargeHelper.consumeCharge(stack, p, 20)) {
            p.removeEffect(MobEffects.WITHER);
            return;
        }
        if (p.hasEffect(MobEffects.POISON) && RechargeHelper.consumeCharge(stack, p, 10)) {
            p.removeEffect(MobEffects.POISON);
            return;
        }
        if (p.hasEffect(thaumcraft.common.config.ConfigPotions.FLUX_TAINT) && RechargeHelper.consumeCharge(stack, p, 5)) {
            p.removeEffect(thaumcraft.common.config.ConfigPotions.FLUX_TAINT);
            return;
        }
        int type = getType(stack);
        if (type == 1 && p.getHealth() < p.getMaxHealth() && RechargeHelper.consumeCharge(stack, p, 5)) {
            p.heal(1.0F);
            return;
        }
        if (type == 2) {
            if (p.getAirSupply() < 100 && RechargeHelper.consumeCharge(stack, p, 1)) {
                p.setAirSupply(300);
                return;
            }
            if (p.canEat(false) && RechargeHelper.consumeCharge(stack, p, 1)) {
                p.getFoodData().eat(1, 0.3F);
            }
        }
    }

    @Override
    public void addCreativeVariants(net.minecraft.world.item.CreativeModeTab.Output output) {
        for (int t = 0; t < 3; t++) {
            ItemStack s = new ItemStack(this);
            if (t > 0) {
                s.set(TCDataComponents.VERDANT_TYPE.get(), t);
            }
            output.accept(s);
        }
    }

    @Override
    public int getMaxCharge(ItemStack stack, LivingEntity player) {
        return 200;
    }

    @Override
    public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player) {
        return EnumChargeDisplay.NORMAL;
    }
}
