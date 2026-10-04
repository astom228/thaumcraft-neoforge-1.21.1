package thaumcraft.common.lib.network.playerdata;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Сервер → клиент: сообщение об изменении Искажения. type: 0 — постоянное, 1 — обычное, 2 — временное.
 * Порядок полей как в оригинале: int data (amount), byte type (warpType).
 */
public record PacketWarpMessage(int amount, byte warpType) implements CustomPacketPayload {
    public static final Type<PacketWarpMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "warp_message"));
    public static final StreamCodec<ByteBuf, PacketWarpMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PacketWarpMessage::amount,
            ByteBufCodecs.BYTE, PacketWarpMessage::warpType,
            PacketWarpMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
