package thaumcraft.common.lib.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.network.ClientPacketHandlers;
import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;
import thaumcraft.common.lib.network.fx.PacketFXEssentiaSource;
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.lib.network.misc.PacketKnowledgeGain;
import thaumcraft.common.lib.network.misc.PacketStartTheoryToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledgeDelta;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncResearchFlagsToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncWarp;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;

/**
 * Регистрация пакетов (в оригинале — SimpleNetworkWrapper "thaumcraft"). Пакеты добавляются по этапам вместе с системами.
 * Обработчики клиентских пакетов вызываются из лямбд, поэтому клиентские классы не загружаются на сервере.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class PacketHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(PacketSyncWarp.TYPE, PacketSyncWarp.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleSyncWarp(msg));
        registrar.playToClient(PacketSyncKnowledge.TYPE, PacketSyncKnowledge.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleSyncKnowledge(msg));
        registrar.playToClient(PacketAuraToClient.TYPE, PacketAuraToClient.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleAura(msg));
        registrar.playToClient(PacketSyncKnowledgeDelta.TYPE, PacketSyncKnowledgeDelta.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleSyncKnowledgeDelta(msg));
        registrar.playToClient(PacketKnowledgeGain.TYPE, PacketKnowledgeGain.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleKnowledgeGain(msg));
        registrar.playToServer(PacketSyncProgressToServer.TYPE, PacketSyncProgressToServer.STREAM_CODEC,
                (msg, ctx) -> PacketSyncProgressToServer.handle(msg, (ServerPlayer) ctx.player()));
        registrar.playToServer(PacketSyncResearchFlagsToServer.TYPE, PacketSyncResearchFlagsToServer.STREAM_CODEC,
                (msg, ctx) -> PacketSyncResearchFlagsToServer.handle(msg, (ServerPlayer) ctx.player()));
        registrar.playToServer(PacketStartTheoryToServer.TYPE, PacketStartTheoryToServer.STREAM_CODEC,
                (msg, ctx) -> PacketStartTheoryToServer.handle(msg, (ServerPlayer) ctx.player()));
        registrar.playToClient(PacketFXBlockBamf.TYPE, PacketFXBlockBamf.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleBlockBamf(msg));
        registrar.playToClient(PacketFXEssentiaSource.TYPE, PacketFXEssentiaSource.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.client.lib.events.EssentiaSourceFXClient.handle(msg));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXInfusionSource.TYPE, thaumcraft.common.lib.network.fx.PacketFXInfusionSource.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleInfusionSource(msg));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXBlockArc.TYPE, thaumcraft.common.lib.network.fx.PacketFXBlockArc.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleBlockArc(msg));
        registrar.playToClient(PacketWarpMessage.TYPE, PacketWarpMessage.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleWarpMessage(msg));
    }
}
