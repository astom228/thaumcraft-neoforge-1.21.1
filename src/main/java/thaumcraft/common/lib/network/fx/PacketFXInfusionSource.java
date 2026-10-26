package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Сервер → клиент: источник частиц наполнения у матрицы (x, y, z). (dx, dy, dz) — «матрица − источник» (пьедестал);
 * нулевой сдвиг — игрок с id = color (отдаёт опыт).
 */
public record PacketFXInfusionSource(int x, int y, int z, byte dx, byte dy, byte dz, int color) implements CustomPacketPayload {
    public static final Type<PacketFXInfusionSource> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_infusion_source"));
    public static final StreamCodec<ByteBuf, PacketFXInfusionSource> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeInt(m.x);
                buf.writeInt(m.y);
                buf.writeInt(m.z);
                buf.writeInt(m.color);
                buf.writeByte(m.dx);
                buf.writeByte(m.dy);
                buf.writeByte(m.dz);
            },
            buf -> {
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                int color = buf.readInt();
                return new PacketFXInfusionSource(x, y, z, buf.readByte(), buf.readByte(), buf.readByte(), color);
            });

    public PacketFXInfusionSource(BlockPos pos, byte dx, byte dy, byte dz, int color) {
        this(pos.getX(), pos.getY(), pos.getZ(), dx, dy, dz, color);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
