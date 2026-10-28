package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → клиент: клубы Выброса над блоком (amount частиц, не больше 40) — при загрязнении ауры с эффектом. */
public record PacketFXPollute(int x, int y, int z, byte amount) implements CustomPacketPayload {
    public static final Type<PacketFXPollute> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_pollute"));
    public static final StreamCodec<ByteBuf, PacketFXPollute> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeInt(m.x);
                buf.writeInt(m.y);
                buf.writeInt(m.z);
                buf.writeByte(m.amount);
            },
            buf -> new PacketFXPollute(buf.readInt(), buf.readInt(), buf.readInt(), buf.readByte()));

    /** Как в оригинале: количество приводится к byte (больше 127 — переполнение, эффекта нет). */
    public PacketFXPollute(BlockPos pos, float amt) {
        this(pos.getX(), pos.getY(), pos.getZ(), (byte) amt);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
