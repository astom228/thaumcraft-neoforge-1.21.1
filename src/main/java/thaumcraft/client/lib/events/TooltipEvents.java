package thaumcraft.client.lib.events;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

/** Подсказки предметов: аспекты (RenderEventHandler + HudHandler оригинала) и содержимое контейнеров эссенции (PlayerEvents). */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class TooltipEvents {

    @SubscribeEvent
    public static void registerFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(AspectTooltip.class, AspectTooltip.Client::new);
    }

    /** Аспекты показываются в экранах с инвентарём при зажатом Shift (или наоборот, если включено showTags). */
    @SubscribeEvent
    public static void gatherComponents(RenderTooltipEvent.GatherComponents event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen<?> && Screen.hasShiftDown() != ModConfig.CONFIG_GRAPHICS.showTags.get()
                && !mc.mouseHandler.isMouseGrabbed()) {
            AspectList tags = ThaumcraftCraftingManager.getObjectTags(event.getItemStack());
            if (tags != null && tags.size() > 0) {
                event.getTooltipElements().add(Either.right(new AspectTooltip(tags)));
            }
        }
    }

    /**
     * PlayerEvents.tooltipEvent: заряд рунической защиты, Искажение, скидка Вис, у предметов с аспектами внутри — «Aer x1»
     * (названия аспектов — латинские), наполняющие зачарования (вставляются второй строкой).
     */
    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        int charge = thaumcraft.common.lib.events.PlayerEvents.getRunicCharge(stack);
        if (charge > 0) {
            event.getToolTip().add(Component.translatable("item.runic.charge").append(" +" + charge).withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        int warp = thaumcraft.common.lib.events.PlayerEvents.getFinalWarp(stack, event.getEntity());
        if (warp > 0) {
            event.getToolTip().add(Component.translatable("item.warping").append(" " + warp).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
        int al = thaumcraft.common.lib.events.PlayerEvents.getFinalDiscount(stack, event.getEntity());
        if (al > 0) {
            event.getToolTip().add(Component.translatable("tc.visdiscount").append(": " + al + "%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
        if (event.getItemStack().getItem() instanceof thaumcraft.api.items.IRechargable) {
            int c = thaumcraft.api.items.RechargeHelper.getCharge(event.getItemStack());
            if (c >= 0) {
                event.getToolTip().add(Component.translatable("tc.charge").append(" " + c).withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
        }
        if (event.getItemStack().getItem() instanceof IEssentiaContainerItem container) {
            AspectList aspects = container.getAspects(event.getItemStack());
            if (aspects != null && aspects.size() > 0) {
                for (Aspect tag : aspects.getAspectsSortedByName()) {
                    event.getToolTip().add(Component.literal(tag.getName() + " x" + aspects.getAmount(tag)));
                }
            }
        }
        var ies = thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.getInfusionEnchantmentTagList(stack);
        if (ies != null && !event.getToolTip().isEmpty()) {
            for (var e : ies) {
                var ie = e.enchantment();
                if (ie != null) {
                    net.minecraft.network.chat.MutableComponent line = Component.translatable("enchantment.infusion." + ie.name());
                    if (ie.maxLevel > 1) {
                        line = line.append(" ").append(Component.translatable("enchantment.level." + e.lvl()));
                    }
                    event.getToolTip().add(1, line.withStyle(net.minecraft.ChatFormatting.GOLD));
                }
            }
        }
    }
}
