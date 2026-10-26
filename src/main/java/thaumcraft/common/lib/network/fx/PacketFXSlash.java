package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → клиент: росчерк удара от существа source к target («Дуга»). */
public record PacketFXSlash(int source, int target) implements CustomPacketPayload {
    public static final Type<PacketFXSlash> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_slash"));
    public static final StreamCodec<ByteBuf, PacketFXSlash> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeInt(m.source);
                buf.writeInt(m.target);
            },
            buf -> new PacketFXSlash(buf.readInt(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
