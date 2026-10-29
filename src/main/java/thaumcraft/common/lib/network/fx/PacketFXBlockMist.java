package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → клиент: туман над блоком (drawBlockMistParticles) цвета color — рост от лампы роста. */
public record PacketFXBlockMist(BlockPos pos, int color) implements CustomPacketPayload {
    public static final Type<PacketFXBlockMist> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_block_mist"));
    public static final StreamCodec<ByteBuf, PacketFXBlockMist> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketFXBlockMist::pos, ByteBufCodecs.INT, PacketFXBlockMist::color, PacketFXBlockMist::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
