package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;

/** Сервер → клиент: разряд (FXBolt) от source к target цвета color (RGB), толщиной width. */
public record PacketFXZap(Vec3 source, Vec3 target, int color, float width) implements CustomPacketPayload {
    public static final Type<PacketFXZap> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_zap"));
    public static final StreamCodec<ByteBuf, PacketFXZap> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeDouble(m.source.x);
                buf.writeDouble(m.source.y);
                buf.writeDouble(m.source.z);
                buf.writeDouble(m.target.x);
                buf.writeDouble(m.target.y);
                buf.writeDouble(m.target.z);
                buf.writeInt(m.color);
                buf.writeFloat(m.width);
            },
            buf -> new PacketFXZap(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readInt(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
