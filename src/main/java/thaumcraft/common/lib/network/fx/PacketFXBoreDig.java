package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → клиент (порт PacketFXBoreDig): частицы копания блока pos буром bore в течение delay тиков. */
public record PacketFXBoreDig(int x, int y, int z, int bore, int delay) implements CustomPacketPayload {
    public static final Type<PacketFXBoreDig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_bore_dig"));
    public static final StreamCodec<ByteBuf, PacketFXBoreDig> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeInt(m.x);
                buf.writeInt(m.y);
                buf.writeInt(m.z);
                buf.writeInt(m.bore);
                buf.writeInt(m.delay);
            },
            buf -> new PacketFXBoreDig(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()));

    public PacketFXBoreDig(BlockPos pos, int bore, int delay) {
        this(pos.getX(), pos.getY(), pos.getZ(), bore, delay);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
