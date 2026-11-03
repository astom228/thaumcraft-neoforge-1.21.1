package thaumcraft.common.lib.network.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerLogistics;

/** Клиент → сервер (порт PacketMiscStringToServer): 0 — строка поиска окна логистики (не длиннее 32 символов). */
public record PacketMiscStringToServer(int id, String text) implements CustomPacketPayload {
    public static final int MAX = 32;
    public static final Type<PacketMiscStringToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "misc_string"));
    public static final StreamCodec<ByteBuf, PacketMiscStringToServer> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, PacketMiscStringToServer::id,
            ByteBufCodecs.stringUtf8(MAX), PacketMiscStringToServer::text, PacketMiscStringToServer::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketMiscStringToServer msg, ServerPlayer player) {
        if (msg.id == 0 && player.containerMenu instanceof ContainerLogistics container) {
            container.setSearchText(msg.text);
        }
    }
}
