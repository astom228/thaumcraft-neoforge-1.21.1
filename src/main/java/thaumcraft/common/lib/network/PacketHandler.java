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
        registrar.playToClient(thaumcraft.common.lib.network.misc.PacketMiscEvent.TYPE, thaumcraft.common.lib.network.misc.PacketMiscEvent.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.client.lib.events.WarpFog.handle(msg));
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
        registrar.playToServer(thaumcraft.common.lib.network.playerdata.PacketFocusNodesToServer.TYPE,
                thaumcraft.common.lib.network.playerdata.PacketFocusNodesToServer.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.common.lib.network.playerdata.PacketFocusNodesToServer.handle(msg, (ServerPlayer) ctx.player()));
        registrar.playToServer(thaumcraft.common.lib.network.playerdata.PacketFocusNameToServer.TYPE,
                thaumcraft.common.lib.network.playerdata.PacketFocusNameToServer.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.common.lib.network.playerdata.PacketFocusNameToServer.handle(msg, (ServerPlayer) ctx.player()));
        registrar.playToServer(thaumcraft.common.lib.network.misc.PacketFocusChangeToServer.TYPE, thaumcraft.common.lib.network.misc.PacketFocusChangeToServer.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.common.lib.network.misc.PacketFocusChangeToServer.handle(msg, (ServerPlayer) ctx.player()));
        registrar.playToServer(thaumcraft.common.lib.network.misc.PacketItemKeyToServer.TYPE, thaumcraft.common.lib.network.misc.PacketItemKeyToServer.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.common.lib.network.misc.PacketItemKeyToServer.handle(msg, (ServerPlayer) ctx.player()));
        registrar.playToServer(thaumcraft.common.lib.network.misc.PacketStartGolemCraftToServer.TYPE,
                thaumcraft.common.lib.network.misc.PacketStartGolemCraftToServer.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.common.lib.network.misc.PacketStartGolemCraftToServer.handle(msg, (ServerPlayer) ctx.player()));
        registrar.playToServer(thaumcraft.common.lib.network.misc.PacketLogisticsRequestToServer.TYPE,
                thaumcraft.common.lib.network.misc.PacketLogisticsRequestToServer.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.common.lib.network.misc.PacketLogisticsRequestToServer.handle(msg, (ServerPlayer) ctx.player()));
        registrar.playToServer(thaumcraft.common.lib.network.misc.PacketMiscStringToServer.TYPE,
                thaumcraft.common.lib.network.misc.PacketMiscStringToServer.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.common.lib.network.misc.PacketMiscStringToServer.handle(msg, (ServerPlayer) ctx.player()));
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
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact.TYPE,
                thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact.STREAM_CODEC, (msg, ctx) -> ClientPacketHandlers.handleFocusPartImpact(msg));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXBlockMist.TYPE, thaumcraft.common.lib.network.fx.PacketFXBlockMist.STREAM_CODEC,
                (msg, ctx) -> thaumcraft.common.lib.fx.IFXDispatcher.get().drawBlockMistParticles(msg.pos(), msg.color()));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXShield.TYPE, thaumcraft.common.lib.network.fx.PacketFXShield.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleShield(msg));
        registrar.playToClient(thaumcraft.common.lib.network.misc.PacketSealToClient.TYPE, thaumcraft.common.lib.network.misc.PacketSealToClient.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleSeal(msg));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXZap.TYPE, thaumcraft.common.lib.network.fx.PacketFXZap.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleZap(msg));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXSonic.TYPE, thaumcraft.common.lib.network.fx.PacketFXSonic.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleSonic(msg));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXFocusEffect.TYPE, thaumcraft.common.lib.network.fx.PacketFXFocusEffect.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleFocusEffect(msg));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXPollute.TYPE, thaumcraft.common.lib.network.fx.PacketFXPollute.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handlePollute(msg));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXScanSource.TYPE, thaumcraft.common.lib.network.fx.PacketFXScanSource.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleScanSource(msg));
        registrar.playToClient(thaumcraft.common.lib.network.fx.PacketFXSlash.TYPE, thaumcraft.common.lib.network.fx.PacketFXSlash.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleSlash(msg));
        registrar.playToClient(PacketWarpMessage.TYPE, PacketWarpMessage.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleWarpMessage(msg));
    }
}
