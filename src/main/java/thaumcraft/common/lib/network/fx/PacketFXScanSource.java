package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → игрок: «Зондирование» — подсветить руды вокруг блока loc (радиус 4 + 4·уровень). */
public record PacketFXScanSource(long loc, byte size) implements CustomPacketPayload {
    public static final Type<PacketFXScanSource> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_scan_source"));
    public static final StreamCodec<ByteBuf, PacketFXScanSource> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeLong(m.loc);
                buf.writeByte(m.size);
            },
            buf -> new PacketFXScanSource(buf.readLong(), buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
