package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → клиент: частицы эффектов фокуса в точке (ключи эффектов через «%»), с движением (mx, my, mz). */
public record PacketFXFocusEffect(float x, float y, float z, float mx, float my, float mz, String parts) implements CustomPacketPayload {
    public static final Type<PacketFXFocusEffect> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_focus_effect"));
    public static final StreamCodec<ByteBuf, PacketFXFocusEffect> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeFloat(m.x);
                buf.writeFloat(m.y);
                buf.writeFloat(m.z);
                buf.writeFloat(m.mx);
                buf.writeFloat(m.my);
                buf.writeFloat(m.mz);
                ByteBufCodecs.STRING_UTF8.encode(buf, m.parts);
            },
            buf -> new PacketFXFocusEffect(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    ByteBufCodecs.STRING_UTF8.decode(buf)));

    public PacketFXFocusEffect(float x, float y, float z, float mx, float my, float mz, String[] parts) {
        this(x, y, z, mx, my, mz, String.join("%", parts));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
