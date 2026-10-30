package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → клиент (порт PacketFXSonic): звуковая волна вопля потустороннего стража от сущности source. */
public record PacketFXSonic(int source) implements CustomPacketPayload {
    public static final Type<PacketFXSonic> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_sonic"));
    public static final StreamCodec<ByteBuf, PacketFXSonic> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, PacketFXSonic::source, PacketFXSonic::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
