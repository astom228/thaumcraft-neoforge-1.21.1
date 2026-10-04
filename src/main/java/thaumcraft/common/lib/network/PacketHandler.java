package thaumcraft.common.lib.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.network.ClientPacketHandlers;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
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
        registrar.playToClient(PacketWarpMessage.TYPE, PacketWarpMessage.STREAM_CODEC,
                (msg, ctx) -> ClientPacketHandlers.handleWarpMessage(msg));
    }
}
