package thaumcraft.client.lib.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.client.lib.events.HudHandler;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
import thaumcraft.common.lib.network.playerdata.PacketSyncWarp;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;

/** Клиентская обработка пакетов (onMessage оригинала). Вызывается в главном потоке клиента. */
public final class ClientPacketHandlers {
    public static void handleAura(PacketAuraToClient msg) {
        HudHandler.currentAura = new AuraChunk(null, msg.base(), msg.vis(), msg.flux());
    }

    public static void handleSyncWarp(PacketSyncWarp msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ThaumcraftCapabilities.getWarp(player).deserializeNBT(player.registryAccess(), msg.data());
        }
    }

    public static void handleSyncKnowledge(PacketSyncKnowledge msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        IPlayerKnowledge pk = ThaumcraftCapabilities.getKnowledge(player);
        pk.deserializeNBT(player.registryAccess(), msg.data());
        for (String key : pk.getResearchList()) {
            if (pk.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP)) {
                // TODO этап 7: Minecraft.getInstance().getToasts().addToast(new ResearchToast(ResearchCategories.getResearch(key)))
            }
            pk.clearResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP);
        }
    }

    /**
     * Как в оригинале (processMessage): для постоянного Искажения (type 0) текст формируется, но не показывается —
     * играет только шёпот; для обычного (1) — текст на панели действий и шёпот; для временного (2) — только текст.
     */
    public static void handleWarpMessage(PacketWarpMessage msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || msg.amount() == 0) {
            return;
        }
        if (msg.warpType() == 0 && msg.amount() > 0) {
            player.playSound(SoundsTC.whispers.get(), 0.5F, 1.0F);
        } else if (msg.warpType() == 1) {
            String key = "tc.addwarpsticky";
            if (msg.amount() < 0) {
                key = "tc.removewarpsticky";
            } else {
                player.playSound(SoundsTC.whispers.get(), 0.5F, 1.0F);
            }
            player.displayClientMessage(Component.translatable(key), true);
        } else if (msg.amount() > 0) {
            player.displayClientMessage(Component.translatable("tc.addwarptemp"), true);
        }
    }
}
