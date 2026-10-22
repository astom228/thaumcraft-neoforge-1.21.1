package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Сервер → клиент: облачко дыма в точке (превращение блока). color −9999 — цвет по умолчанию;
 * flags: бит 0 — звук, бит 1 — «вспышка»; face — сторона или −1.
 */
public record PacketFXBlockBamf(double x, double y, double z, int color, byte flags, byte face) implements CustomPacketPayload {
    public static final Type<PacketFXBlockBamf> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_block_bamf"));
    public static final StreamCodec<ByteBuf, PacketFXBlockBamf> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeDouble(m.x);
                buf.writeDouble(m.y);
                buf.writeDouble(m.z);
                buf.writeInt(m.color);
                buf.writeByte(m.flags);
                buf.writeByte(m.face);
            },
            buf -> new PacketFXBlockBamf(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readByte(), buf.readByte()));

    public PacketFXBlockBamf(double x, double y, double z, int color, boolean sound, boolean flair, Direction side) {
        this(x, y, z, color, (byte) ((sound ? 1 : 0) | (flair ? 2 : 0)), (byte) (side != null ? side.ordinal() : -1));
    }

    public PacketFXBlockBamf(BlockPos pos, int color, boolean sound, boolean flair, Direction side) {
        this(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, color, sound, flair, side);
    }

    public boolean sound() {
        return (this.flags & 1) != 0;
    }

    public boolean flair() {
        return (this.flags & 2) != 0;
    }

    public Direction side() {
        return this.face >= 0 && this.face < Direction.values().length ? Direction.values()[this.face] : null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
