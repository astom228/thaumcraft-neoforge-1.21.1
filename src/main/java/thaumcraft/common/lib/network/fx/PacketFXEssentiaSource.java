package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Сервер → клиент: поток эссенции от источника к устройству. (x, y, z) — устройство (куда течёт), (dx, dy, dz) — сдвиг до источника
 * со знаком «устройство − источник»; ext — длина потока.
 */
public record PacketFXEssentiaSource(int x, int y, int z, byte dx, byte dy, byte dz, int color, int ext) implements CustomPacketPayload {
    public static final Type<PacketFXEssentiaSource> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_essentia_source"));
    public static final StreamCodec<ByteBuf, PacketFXEssentiaSource> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeInt(m.x);
                buf.writeInt(m.y);
                buf.writeInt(m.z);
                buf.writeInt(m.color);
                buf.writeByte(m.dx);
                buf.writeByte(m.dy);
                buf.writeByte(m.dz);
                buf.writeShort(m.ext);
            },
            buf -> {
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                int color = buf.readInt();
                byte dx = buf.readByte();
                byte dy = buf.readByte();
                byte dz = buf.readByte();
                return new PacketFXEssentiaSource(x, y, z, dx, dy, dz, color, buf.readShort());
            });

    public PacketFXEssentiaSource(BlockPos p1, byte dx, byte dy, byte dz, int color, int ext) {
        this(p1.getX(), p1.getY(), p1.getZ(), dx, dy, dz, color, ext);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
