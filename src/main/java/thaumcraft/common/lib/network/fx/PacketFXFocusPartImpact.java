package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → клиент: вспышка эффектов фокуса в точке попадания (ключи через «%»), 15 / (число эффектов) частиц каждого. */
public record PacketFXFocusPartImpact(float x, float y, float z, String parts) implements CustomPacketPayload {
    public static final Type<PacketFXFocusPartImpact> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_focus_part_impact"));
    public static final StreamCodec<ByteBuf, PacketFXFocusPartImpact> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeFloat(m.x);
                buf.writeFloat(m.y);
                buf.writeFloat(m.z);
                ByteBufCodecs.STRING_UTF8.encode(buf, m.parts);
            },
            buf -> new PacketFXFocusPartImpact(buf.readFloat(), buf.readFloat(), buf.readFloat(), ByteBufCodecs.STRING_UTF8.decode(buf)));

    public PacketFXFocusPartImpact(double x, double y, double z, String[] parts) {
        this((float) x, (float) y, (float) z, String.join("%", parts));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
