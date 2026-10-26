package thaumcraft.common.lib.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.items.curios.ItemThaumonomicon;
import thaumcraft.common.items.resources.ItemCrystalEssence;
import thaumcraft.common.lib.capabilities.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * События игрока (порт PlayerEvents оригинала; остальные обработчики добавляются на своих этапах).
 * attachCapabilitiesPlayer и cloneCapabilitiesEvent заменены Data Attachments с copyOnDeath (ThaumcraftAttachments).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class PlayerEvents {
    /** livingTick оригинала (часть для игрока на сервере): отложенная синхронизация знаний и периодические находки. */
    @SubscribeEvent
    public static void livingTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.tickCount % 20 == 0 && ResearchManager.syncList.remove(player.getUUID())) {
                ThaumcraftCapabilities.getKnowledge(player).sync(player);
            }
            if (player.tickCount % 200 == 0) {
                ConfigResearch.checkPeriodicStuff(player);
            }
        }
    }

    @SubscribeEvent
    public static void pickupItem(ItemEntityPickupEvent.Pre event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemEntity().getItem();
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (stack.getItem() instanceof ItemCrystalEssence && !knowledge.isResearchKnown("!gotcrystals")) {
                knowledge.addResearch("!gotcrystals");
                knowledge.sync(player);
                player.sendSystemMessage(Component.translatable("got.crystals").withStyle(ChatFormatting.DARK_PURPLE));
            }
            if (stack.getItem() instanceof ItemThaumonomicon && !knowledge.isResearchKnown("!gotthaumonomicon")) {
                knowledge.addResearch("!gotthaumonomicon");
                knowledge.sync(player);
            }
        }
    }

    @SubscribeEvent
    public static void wakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge.isResearchKnown("!gotcrystals") && !knowledge.isResearchKnown("!gotdream")) {
                knowledge.addResearch("!gotdream");
                knowledge.sync(player);
                ItemStack book = ConfigItems.startBook(player.getName().getString());
                if (!player.getInventory().add(book)) {
                    // InventoryUtils.dropItemAtEntity
                    player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + player.getEyeHeight() / 2.0F, player.getZ(), book));
                }
                player.sendSystemMessage(Component.translatable("got.dream").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    /** playerJoin: при появлении игрока в мире (вход, возрождение, смена измерения) — синхронизация знаний и Искажения. */
    @SubscribeEvent
    public static void playerJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            if (ThaumcraftCapabilities.getKnowledge(player) instanceof PlayerKnowledge pk) {
                pk.markFullSync();
            }
            ThaumcraftCapabilities.getKnowledge(player).sync(player);
            ThaumcraftCapabilities.getWarp(player).sync(player);
        }
    }

    /** Заряд рунической защиты предмета (TC.RUNIC оригинала). */
    public static int getRunicCharge(net.minecraft.world.item.ItemStack stack) {
        return stack.getOrDefault(thaumcraft.common.lib.TCDataComponents.RUNIC_CHARGE.get(), 0);
    }

    /** Искажение от предмета: IWarpingGear + TC.WARP оригинала. */
    public static int getFinalWarp(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int warp = 0;
        if (stack.getItem() instanceof thaumcraft.api.items.IWarpingGear armor) {
            warp += armor.getWarp(stack, player);
        }
        return warp + stack.getOrDefault(thaumcraft.common.lib.TCDataComponents.WARPING.get(), 0);
    }

    /** Скидка Вис от предмета, %. */
    public static int getFinalDiscount(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof thaumcraft.api.items.IVisDiscountGear gear ? gear.getVisDiscount(stack, player) : 0;
    }
}
