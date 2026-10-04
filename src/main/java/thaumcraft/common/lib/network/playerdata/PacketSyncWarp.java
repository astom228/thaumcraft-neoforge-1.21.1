package thaumcraft.common.lib.network.playerdata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → клиент: Искажение игрока (NBT как в оригинале). */
public record PacketSyncWarp(CompoundTag data) implements CustomPacketPayload {
    public static final Type<PacketSyncWarp> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_warp"));
    public static final StreamCodec<FriendlyByteBuf, PacketSyncWarp> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(PacketSyncWarp::new, PacketSyncWarp::data).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
