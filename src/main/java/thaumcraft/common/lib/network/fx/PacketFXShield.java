package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Сервер → клиент: удар по рунической защите сущности source (FXShieldRunes). target — id атакующего; −1 — без источника,
 * −2 — падение, −3 — падающий блок.
 */
public record PacketFXShield(int source, int target) implements CustomPacketPayload {
    public static final Type<PacketFXShield> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_shield"));
    public static final StreamCodec<ByteBuf, PacketFXShield> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PacketFXShield::source, ByteBufCodecs.INT, PacketFXShield::target, PacketFXShield::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
