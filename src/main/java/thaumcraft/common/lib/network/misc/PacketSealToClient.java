package thaumcraft.common.lib.network.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Сервер → клиент (порт PacketSealToClient): состояние печати целиком (NBT SealEntity); тип «REMOVE» — печать снята.
 * В оригинале поля писались по одному — здесь та же информация одним NBT.
 */
public record PacketSealToClient(CompoundTag data) implements CustomPacketPayload {
    public static final Type<PacketSealToClient> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "seal_to_client"));
    public static final StreamCodec<ByteBuf, PacketSealToClient> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, PacketSealToClient::data,
            PacketSealToClient::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
