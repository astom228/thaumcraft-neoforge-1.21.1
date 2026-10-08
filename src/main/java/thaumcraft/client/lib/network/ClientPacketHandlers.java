package thaumcraft.client.lib.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.gui.ResearchToast;
import thaumcraft.client.lib.events.HudHandler;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.capabilities.PlayerKnowledge;
import thaumcraft.common.lib.network.misc.PacketKnowledgeGain;
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledgeDelta;
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
            showPopup(pk, key);
        }
    }

    /** Дельта знаний (PacketSyncKnowledgeDelta): то же, что полная синхронизация, только для изменившихся записей. */
    public static void handleSyncKnowledgeDelta(PacketSyncKnowledgeDelta msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(ThaumcraftCapabilities.getKnowledge(player) instanceof PlayerKnowledge pk)) {
            return;
        }
        for (PacketSyncKnowledgeDelta.ResearchDelta d : msg.research()) {
            pk.applyResearchDelta(d.key(), d.present(), d.stage(), d.flags());
        }
        for (PacketSyncKnowledgeDelta.KnowledgeDelta d : msg.knowledge()) {
            pk.applyKnowledgeDelta(d.key(), d.amount());
        }
        for (PacketSyncKnowledgeDelta.ResearchDelta d : msg.research()) {
            if (d.present()) {
                showPopup(pk, d.key());
            }
        }
    }

    /** Тост «Исследование завершено» для записи с флагом POPUP, затем флаг снимается. */
    private static void showPopup(IPlayerKnowledge pk, String key) {
        if (pk.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP)) {
            ResearchEntry ri = ResearchCategories.getResearch(key);
            if (ri != null) {
                Minecraft.getInstance().getToasts().addToast(new ResearchToast(ri));
            }
        }
        pk.clearResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP);
    }

    /** PacketKnowledgeGain: эффект на HUD и звук. */
    public static void handleKnowledgeGain(PacketKnowledgeGain msg) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || msg.knowledgeType() < 0 || msg.knowledgeType() >= IPlayerKnowledge.EnumKnowledgeType.values().length) {
            return;
        }
        IPlayerKnowledge.EnumKnowledgeType type = IPlayerKnowledge.EnumKnowledgeType.values()[msg.knowledgeType()];
        ResearchCategory cat = !msg.cat().isEmpty() ? ResearchCategories.getResearchCategory(msg.cat()) : null;
        HudHandler.knowledgeGainTrackers.add(new HudHandler.KnowledgeGainTracker(type, cat, 40 + p.level().random.nextInt(20), p.level().random.nextLong()));
        p.level().playLocalSound(p.getX(), p.getY(), p.getZ(), SoundsTC.learn.get(), SoundSource.AMBIENT, 1.0F, 1.0F, false);
    }

    /**
     * processMessage оригинала: постоянное (type 0) и обычное (1) — текст на панели действий и шёпот при получении;
     * для обычного (1) — текст на панели действий и шёпот; для временного (2) — только текст.
     */
    public static void handleWarpMessage(PacketWarpMessage msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || msg.amount() == 0) {
            return;
        }
        if (msg.warpType() == 0) {
            // в оригинале текст для постоянного Искажения формировался, но не выводился (DEVIATIONS.md)
            String key = "tc.addwarp";
            if (msg.amount() < 0) {
                key = "tc.removewarp";
            } else {
                player.playSound(SoundsTC.whispers.get(), 0.5F, 1.0F);
            }
            player.displayClientMessage(Component.translatable(key), true);
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
