package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.SimpleTier;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.api.items.ItemsTC;

/**
 * Багровый клинок (порт ItemCrimsonBlade): материал CVOID (уровень 4, прочность 200, урон 3.5, зачаровываемость 20, чинится пустотным
 * металлом), самопочинка, при ударе — слабость 3 с и голод 6 с («Великое ослабление»), Искажение 2. Оружие лидера культистов.
 */
public class ItemCrimsonBlade extends SwordItem implements IWarpingGear {
    public static final Tier MATERIAL = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 200, 8.0F, 3.5F, 20,
            () -> Ingredient.of(ItemsTC.ingotVoid.get()));

    public ItemCrimsonBlade(Properties props) {
        super(MATERIAL, props.attributes(SwordItem.createAttributes(MATERIAL, 3, -2.4F)));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidMetalHelper.repairTick(stack, level, entity);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            boolean pvpBlocked = target instanceof Player && attacker instanceof Player
                    && (target.level().getServer() == null || !target.level().getServer().isPvpAllowed());
            if (!pvpBlocked) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60));
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120));
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.special.sapgreat").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
