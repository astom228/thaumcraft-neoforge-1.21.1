package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import thaumcraft.Thaumcraft;

/** Сервер → клиент: молния от точки (tx, ty, tz) к центру блока (x, y, z) цвета (r, g, b). */
public record PacketFXBlockArc(int x, int y, int z, float tx, float ty, float tz, float r, float g, float b) implements CustomPacketPayload {
    public static final Type<PacketFXBlockArc> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_block_arc"));
    public static final StreamCodec<ByteBuf, PacketFXBlockArc> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeInt(m.x);
                buf.writeInt(m.y);
                buf.writeInt(m.z);
                buf.writeFloat(m.tx);
                buf.writeFloat(m.ty);
                buf.writeFloat(m.tz);
                buf.writeFloat(m.r);
                buf.writeFloat(m.g);
                buf.writeFloat(m.b);
            },
            buf -> new PacketFXBlockArc(buf.readInt(), buf.readInt(), buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat()));

    /** К существу: середина его высоты. */
    public PacketFXBlockArc(BlockPos pos, Entity source, float r, float g, float b) {
        this(pos.getX(), pos.getY(), pos.getZ(), (float) source.getX(), (float) (source.getBoundingBox().minY + source.getBbHeight() / 2.0F),
                (float) source.getZ(), r, g, b);
    }

    /** К блоку: центр блока pos2. */
    public PacketFXBlockArc(BlockPos pos, BlockPos pos2, float r, float g, float b) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos2.getX() + 0.5F, pos2.getY() + 0.5F, pos2.getZ() + 0.5F, r, g, b);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
